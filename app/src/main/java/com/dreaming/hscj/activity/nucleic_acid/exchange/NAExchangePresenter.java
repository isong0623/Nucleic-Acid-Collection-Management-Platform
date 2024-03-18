package com.dreaming.hscj.activity.nucleic_acid.exchange;

import android.util.Pair;

import com.dreaming.hscj.App;
import com.dreaming.hscj.Constants;
import com.dreaming.hscj.core.ThreadPoolProvider;
import com.dreaming.hscj.dialog.loading.LoadingDialog;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.template.api.ApiProvider;
import com.dreaming.hscj.template.api.ParamSetter;
import com.dreaming.hscj.template.api.impl.Api;
import com.dreaming.hscj.template.api.impl.ApiConfig;
import com.dreaming.hscj.template.api.wrapper.ApiParam;
import com.dreaming.hscj.utils.ExcelUtils;
import com.dreaming.hscj.utils.FileUtils;
import com.dreaming.hscj.utils.JsonUtils;
import com.dreaming.hscj.utils.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

public class NAExchangePresenter extends INAExchangeContract.Presenter{

    void showWaitDialog(){
        App.Post(()-> LoadingDialog.showDialog("Wait_For_Exchange",App.sInstance.getCurrentActivity()));
    }

    void hideWaitDialog(){
        App.Post(()->LoadingDialog.dismissDialog("Wait_For_Exchange"));
    }

    int allCount = 0;
    private List<ESONObject> netNotFoundLog = new ArrayList<>();//本地未匹配记录
    private void query(int page, ESONObject params, int ips){
        App.Post(()-> mView.onMessage(String.format("正在获取%d页记录。",page+1)));
        params.putValue(query.getRequest().getPageIndex(),page);

        ApiProvider.requestQuerySamplingHistoryRecordSync(params, new ApiProvider.ISamplingRecordQueryListener() {
            @Override
            public void onSuccess(ESONArray data) {
                if(data == null) data = new ESONArray();
                List<ESONObject> lst = JsonUtils.parseToList(data);
                for (int i=0,ni=lst.size();i<ni;++i) {
                    ESONObject item = lst.get(i);
                    String tubNumber = item.getJSONValue(query.getResponse().getTubNo(),"");
                    switch (mode){
                        case 0:
                            if(!tubNumber.equals(srcNo)){
                                lst.remove(i);
                                --i;
                                --ni;
                            }
                            break;
                        case 1:
                            if(!StringUtils.isMatch(tubNumber,srcNo)){
                                lst.remove(i);
                                --i;
                                --ni;
                            }
                            break;
                    }
                }
                allCount += lst.size();
                App.Post(()->mView.onTransferReadyUpdate(allCount));
                match(page,lst);
                if(data.length() == ips){
                    query(page+1, params, ips);
                }
            }

            @Override
            public void onFailure(String err) {
                query(page, params, ips);
            }
        });
    }

    private List<ESONObject> lstDbInfo = new ArrayList<>();
    private Map<String,String> mSetter = new HashMap<>();

    //Barcode Log User
    private Map<String,List<Pair<ESONObject,ESONObject>>> mLocalSuccess = new TreeMap<>();

    private void match(int page, List<ESONObject> lstData){
        App.Post(()-> mView.onMessage(String.format("正在匹配第%d页本地数据！",page+1)));

        if(lstDbInfo.isEmpty()){
            ESONArray a = Constants.DBConfig.getAllDatabase();
            for(int i=0,ni=a.length();i<ni;++i){
                ESONObject o = a.getArrayValue(i,new ESONObject());
                if(o.length()==0) continue;

                lstDbInfo.add(o);
            }

            Map<String,List<String>> mAllSetter = Template.getCurrentTemplate().getUserOverallDatabase().getConfig().getSetter();
            for (Map.Entry<String, List<String>> entry : mAllSetter.entrySet()) {
                List<String> lst = entry.getValue();
                if(lst == null) continue;
                for(String field : lst){
                    if(Api.typeOf(field) == 6){
                        mSetter.put(entry.getKey(),Api.fieldOf(field));
                        break;
                    }
                }
            }
        }

        int selectedDb = Constants.DBConfig.getSelectedDatabase().getJSONValue("id",-1);
        try {
            for(int i=0,ni=lstDbInfo.size();i<ni;++i){
                ESONObject dbInfo = lstDbInfo.get(i);

                int id = dbInfo.getJSONValue("id",-1);
                for(int j=0;j<3;++j){
                    if(Constants.DBConfig.setSelectedDatabase(id)) break;
                }

                for(int j=0,nj=lstData.size();j<nj;++j){
                    ESONObject log = lstData.get(j);
                    for (Map.Entry<String, String> entry : mSetter.entrySet()) {
                        String dbKey  = entry.getKey();
                        String apiKey = entry.getValue();
                        List<String> lstK = new ArrayList<>(); lstK.add(dbKey);
                        List<Object> lstV = new ArrayList<>(); lstV.add(log.getJSONValue(apiKey,""));
                        ESONArray result = Template.getCurrentTemplate().getUserOverallDatabase().search(lstK,lstV);
                        if(result!=null && result.length() == 1){
                            ESONObject json = result.getArrayValue(0,new ESONObject());
                            if(json.length() == 0) continue;

                            lstData.remove(j);
                            --j;
                            --nj;

                            String barcode = log.getJSONValue(query.getResponse().getTubNo(),"");
                            List<Pair<ESONObject,ESONObject>> lst = mLocalSuccess.get(barcode);
                            if(lst == null) lst = new ArrayList<>();
                            lst.add(new Pair<>(log,json));
                            mLocalSuccess.put(barcode, lst);
                        }
                    }
                }
            }

            if(!lstData.isEmpty()){
                netNotFoundLog.addAll(lstData);
            }
        } finally {
            for(int j=0;j<3;++j){
                if(Constants.DBConfig.setSelectedDatabase(selectedDb)) break;
            }
        }

    }

    private boolean delete(String barcode, ESONObject data){
        AtomicBoolean result = new AtomicBoolean(false);
        ParamSetter setter = new ParamSetter(6,data);
        ApiProvider.requestDeleteSamplingRecordWithTubeNoSnyc(barcode, setter, new ApiProvider.ISamplingRecordDeleteListener() {
            @Override
            public void onSuccess() {}
            @Override
            public void onFailure(String err) {}
        });
        return result.get();
    }

    int samplingCount = 0;
    private boolean sampling(String barcode, ESONObject data){
        AtomicBoolean result = new AtomicBoolean(false);
        ApiProvider.requestNucleicAcidSampling(barcode, data, new ApiProvider.INCSamplingListener() {
            @Override
            public void onSuccess() {
                App.Post(()->mView.onTransferEndUpdate(++samplingCount));
            }
            @Override
            public void onFailure(String err) {}
        });
        return result.get();
    }

    int processed = 0;
    private void process(){
        if(mLocalSuccess.isEmpty()){
            return;
        }
        Map.Entry<String,List<Pair<ESONObject,ESONObject>>> item = mLocalSuccess.entrySet().iterator().next();

        if(item.getValue() == null || item.getValue().size() == 0){
            mLocalSuccess.remove(item.getKey());
            process();
            return;
        }

        ++processed;

        int progress = ((int)(((float)processed)/allCount * 50));
        App.Post(()->mView.onProgress(Math.min(50,progress)));

        ESONObject log  = item.getValue().get(0).first;
        ESONObject data = item.getValue().get(0).second;
        item.getValue().remove(0);
        String barcode = item.getKey();
        String newCode = mode == 0 ? tarNo : StringUtils.replaceAll(barcode,srcNo,tarNo);
        delete(barcode,log);
        sampling(newCode,data);

        process();
    }

    private Map<String,List<Pair<ESONObject,ESONObject>>> mChecking = new TreeMap<>();
    private void check(int page, ESONObject params, int ips){
        App.Post(()-> mView.onMessage(String.format("正在验证%d页转移结果。",page+1)));
        params.putValue(query.getRequest().getPageIndex(),page);

        ApiProvider.requestQuerySamplingHistoryRecordSync(params, new ApiProvider.ISamplingRecordQueryListener() {
            @Override
            public void onSuccess(ESONArray data) {
                if(data == null) data = new ESONArray();
                List<ESONObject> lst = JsonUtils.parseToList(data);
                for (int i=0,ni=lst.size();i<ni;++i) {
                    ESONObject item = lst.get(i);
                    String tubNumber = item.getJSONValue(query.getResponse().getTubNo(),"");
                    switch (mode){
                        case 0:
                            if(!tubNumber.equals(srcNo)){
                                lst.remove(i);
                                --i;
                                --ni;
                            }
                            break;
                        case 1:
                            if(!StringUtils.isMatch(tubNumber,srcNo)){
                                lst.remove(i);
                                --i;
                                --ni;
                            }
                            break;
                    }
                }
                matchChecking(page,lst);
                if(data.length() == ips){
                    check(page+1, params, ips);
                }
            }

            @Override
            public void onFailure(String err) {
                check(page, params, ips);
            }
        });

    }

    private void matchChecking(int page, List<ESONObject> lstData){
        int selectedDb = Constants.DBConfig.getSelectedDatabase().getJSONValue("id",-1);
        try {
            final int allCount = lstData.size();
            for(int i=0,ni=lstDbInfo.size();i<ni;++i){
                ESONObject dbInfo = lstDbInfo.get(i);

                int id = dbInfo.getJSONValue("id",-1);
                for(int j=0;j<3;++j){
                    if(Constants.DBConfig.setSelectedDatabase(id)) break;
                }

                for(int j=0,nj=lstData.size();j<nj;++j){
                    ESONObject log = lstData.get(j);
                    for (Map.Entry<String, String> entry : mSetter.entrySet()) {
                        String dbKey  = entry.getKey();
                        String apiKey = entry.getValue();
                        List<String> lstK = new ArrayList<>(); lstK.add(dbKey);
                        List<Object> lstV = new ArrayList<>(); lstV.add(log.getJSONValue(apiKey,""));
                        ESONArray result = Template.getCurrentTemplate().getUserOverallDatabase().search(lstK,lstV);
                        if(result!=null && result.length() == 1){
                            ESONObject json = result.getArrayValue(0,new ESONObject());
                            if(json.length() == 0) continue;

                            lstData.remove(j);
                            --j;
                            --nj;

                            String barcode = log.getJSONValue(query.getResponse().getTubNo(),"");
                            List<Pair<ESONObject,ESONObject>> lst = mChecking.get(barcode);
                            if(lst == null) lst = new ArrayList<>();
                            lst.add(new Pair<>(log,json));
                            mChecking.put(barcode, lst);
                        }
                    }
                }
            }
        } finally {
            for(int j=0;j<3;++j){
                if(Constants.DBConfig.setSelectedDatabase(selectedDb)) break;
            }
        }
    }

    final String stateFieldName = "1_state__field___Nme_";
    final String tarFieldName = "_tar_field_name__1";
    private List<ESONObject> lstTransferReady = new ArrayList<>();
    private List<ESONObject> lstTransferEnd   = new ArrayList<>();
    int summary = 0;
    private void summary(){
        App.Post(()-> mView.onMessage("转换完成，正在整理转移结果！"));
        lstTransferReady.clear();
        lstTransferEnd  .clear();

        for (Map.Entry<String, List<Pair<ESONObject, ESONObject>>> entry : mLocalSuccess.entrySet()) {
            String srcBarcode = entry.getKey();
            String tarBarcode = mode == 0 ? tarNo : StringUtils.replaceAll(srcBarcode,srcNo,tarNo);


            List<Pair<ESONObject, ESONObject>> lstSource = entry.getValue();
            if(lstSource == null) lstSource = new ArrayList<>();
            List<Pair<ESONObject, ESONObject>> lstTarget = mChecking.get(tarBarcode);
            if(lstTarget == null) lstTarget = new ArrayList<>();

            Map<String,Pair<ESONObject,ESONObject>> mTarget = new HashMap<>();
            Map<String,Pair<ESONObject,ESONObject>> mSource = new HashMap<>();

            for (Pair<ESONObject, ESONObject> pair : lstSource) {
                String id = pair.second.getJSONValue(Template.getCurrentTemplate().getIdCardNoFieldName(), "");
                mSource.put(id,pair);
            }

            for (Pair<ESONObject, ESONObject> pair : lstTarget) {
                String id = pair.second.getJSONValue(Template.getCurrentTemplate().getIdCardNoFieldName(), "");
                mTarget.put(id,pair);
            }

            for (Pair<ESONObject, ESONObject> pair : lstSource) {
                String id = pair.second.getJSONValue(Template.getCurrentTemplate().getIdCardNoFieldName(), "");
                String barcode = pair.first.getJSONValue(query.getResponse().getTubNo(),"");
                String newcode = mode == 0 ? tarNo : StringUtils.replaceAll(barcode,srcNo,tarNo);

                ++summary;
                int progress = (int)((float)summary/allCount * 50);
                App.Post(()->mView.onProgress(Math.min(100,50+progress)));

                Pair<ESONObject,ESONObject> pMatch = mTarget.get(id);
                if(pMatch != null) {
                    pMatch.first.putValue(query.getResponse().getIdNo(),pMatch.second.getJSONValue(Template.getCurrentTemplate().getIdCardNoFieldName(), ""));
                    pMatch.first.putValue(stateFieldName,"已转换");
                    pMatch.first.putValue(tarFieldName,newcode);
                    lstTransferEnd.add(pMatch.first);
                    continue;
                }

                pMatch = mSource.get(id);
                if(pMatch != null){
                    pMatch.first.putValue(query.getResponse().getIdNo(),pMatch.second.getJSONValue(Template.getCurrentTemplate().getIdCardNoFieldName(), ""));
                    pMatch.first.putValue(stateFieldName,"未转换");
                    pMatch.first.putValue(tarFieldName,newcode);
                    lstTransferReady.add(pMatch.first);
                    continue;
                }

                pair.first.putValue(query.getResponse().getIdNo(),pair.second.getJSONValue(Template.getCurrentTemplate().getIdCardNoFieldName(), ""));
                pair.first.putValue(stateFieldName,"已删除");
                pair.first.putValue(tarFieldName,newcode);
                lstTransferReady.add(pair.first);
            }
        }

    }

    private void exportNoMatch(){
        App.Post(()-> mView.onMessage("转换失败，正在保存待匹配结果！"));
        ExcelUtils.Writer writer = new ExcelUtils.Writer();
        String path = String.format("%s/hscj_未匹配_%s.xlsx", FileUtils.getExternalDir(),new SimpleDateFormat("yyyy_MM_dd__HH_mm_ss").format(new Date()));

        writer.write(0,0,0,"姓名");
        writer.write(0,0,1,"身份号");
        writer.write(0,0,2,"电话");
        writer.write(0,0,3,"转移条码");

        int counter = 1;
        for (ESONObject item : netNotFoundLog) {
            writer.write(0,counter,0,item.getJSONValue(query.getResponse().getName(),""));
            writer.write(0,counter,1,item.getJSONValue(query.getResponse().getIdNo(),""));
            writer.write(0,counter,2,item.getJSONValue(query.getResponse().getPhone(),""));
            writer.write(0,counter,3,item.getJSONValue(query.getResponse().getTubNo(),""));
            ++counter;
        }

        writer.save(path);
        App.Post(()->mView.onProgress(100));
        App.Post(()->mView.onMessage("转换失败！"));
        App.Post(()->mView.onTransferFailure());

    }

    private void export(){
        App.Post(()-> mView.onMessage("转换完成，正在写入转换结果！"));
        ExcelUtils.Writer writer = new ExcelUtils.Writer();
        String path = String.format("%s/hscj_转换结果_%s.xlsx", FileUtils.getExternalDir(),new SimpleDateFormat("yyyy_MM_dd__HH_mm_ss").format(new Date()));

        writer.write(0,0,0,"姓名");
        writer.write(0,0,1,"身份号");
        writer.write(0,0,2,"转移条码");
        writer.write(0,0,3,"目标条码");
        writer.write(0,0,4,"转移状态");

        int counter = 1;
        for (ESONObject item : lstTransferReady) {
            writer.write(0,counter,0, item.getJSONValue(query.getResponse().getName(),""));
            writer.write(0,counter,1, item.getJSONValue(query.getResponse().getIdNo(),""));
            String barcode = item.getJSONValue(query.getResponse().getPhone(),"");
            writer.write(0,counter,2, barcode);
            String newcode = mode == 0 ? tarNo : StringUtils.replaceAll(barcode,srcNo,tarNo);
            writer.write(0,counter,3, newcode);
            writer.write(0,counter,4, item.getJSONValue(stateFieldName,""));

            ++counter;
        }

        for (ESONObject item : lstTransferEnd) {
            writer.write(0,counter,0, item.getJSONValue(query.getResponse().getName(),""));
            writer.write(0,counter,1, item.getJSONValue(query.getResponse().getIdNo(),""));
            String barcode = item.getJSONValue(query.getResponse().getPhone(),"");
            writer.write(0,counter,2, barcode);
            String newcode = mode == 0 ? tarNo : StringUtils.replaceAll(barcode,srcNo,tarNo);
            writer.write(0,counter,3, newcode);
            writer.write(0,counter,4, item.getJSONValue(stateFieldName,""));

            ++counter;
        }

        if(!netNotFoundLog.isEmpty()){
            ++counter;
            writer.write(0,counter,0,"姓名");
            writer.write(0,counter,1,"身份号");
            writer.write(0,counter,2,"电话");
            writer.write(0,counter,3,"转移条码");

            ++counter;
            for (ESONObject item : netNotFoundLog) {
                writer.write(0,counter,0,item.getJSONValue(query.getResponse().getName(),""));
                writer.write(0,counter,1,item.getJSONValue(query.getResponse().getIdNo(),""));
                writer.write(0,counter,2,item.getJSONValue(query.getResponse().getPhone(),""));
                writer.write(0,counter,3,item.getJSONValue(query.getResponse().getTubNo(),""));
                ++counter;
            }
        }

        writer.save(path);
        App.Post(()->mView.onProgress(100));
        App.Post(()->mView.onMessage("转换完成！"));
    }

    private String srcNo,tarNo;
    private int mode = 0;
    final ApiConfig.Locate.Query query = Template.getCurrentTemplate().getApiConfig().getQuery();
    @Override
    void transfer(String srcNo, String tarNo) {
        transfer(srcNo,tarNo,null,null);
    }

    @Override
    void transfer(String srcNo, String tarNo, String startTime, String endTime) {
        this.srcNo = srcNo;
        this.tarNo = tarNo;
        mode = startTime == null || endTime == null ? 0 : 1;

        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            try {
                showWaitDialog();

                Map<String, ApiParam> mMapper = Template.getCurrentTemplate().apiOf(6).getRequest().getFieldMapper();

                int iPageSize = 10;
                try {
                    iPageSize = Integer.parseInt(mMapper.get(query.getRequest().getPageSize()).getDefaultValue());
                } catch (Exception e) {}
                int ips = iPageSize;

                ESONObject params =
                        mode == 0 ?

                                new ESONObject()
                                        .putValue(query.getRequest().getPageSize() ,iPageSize)
                                        .putValue(query.getRequest().getTubNo()    ,srcNo)

                                :

                                new ESONObject()
                                        .putValue(query.getRequest().getPageSize() ,iPageSize)
                                        .putValue(query.getRequest().getStartDate(),startTime)
                                        .putValue(query.getRequest().getEndDate()  ,endTime)
                        ;

                query(0, params, iPageSize);

                if(!netNotFoundLog.isEmpty()){
                    App.Post(()->mView.showUnmatchedDialog(
                            netNotFoundLog,
                            v-> exportNoMatch(),//onAbort
                            v -> {//onContinue
                                ThreadPoolProvider.getFixedThreadPool().execute(()->{
                                    try {
                                        showWaitDialog();

                                        process();
                                        check(0, params, ips);
                                        summary();
                                        export();
                                    } finally {
                                        hideWaitDialog();
                                    }
                                });
                            }
                            )
                    );
                    return;
                }

                process();
                check(0, params, iPageSize);
                summary();
                export();
            }
            finally {
                hideWaitDialog();
            }
        });
    }

    @Override
    List<ESONObject> getTransferReadyRecodes() {
        return lstTransferReady;
    }

    @Override
    List<ESONObject> getTransferEndRecodes() {
        return lstTransferEnd;
    }
}
