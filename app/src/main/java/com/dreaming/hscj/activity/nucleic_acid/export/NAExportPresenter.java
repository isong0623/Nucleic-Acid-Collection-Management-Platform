package com.dreaming.hscj.activity.nucleic_acid.export;

import com.dreaming.hscj.App;
import com.dreaming.hscj.Constants;
import com.dreaming.hscj.core.ThreadPoolProvider;
import com.dreaming.hscj.dialog.loading.LoadingDialog;
import com.dreaming.hscj.template.DataParser;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.template.api.ApiProvider;
import com.dreaming.hscj.template.api.impl.Api;
import com.dreaming.hscj.template.api.impl.ApiConfig;
import com.dreaming.hscj.template.api.wrapper.ApiParam;
import com.dreaming.hscj.utils.ExcelUtils;
import com.dreaming.hscj.utils.FileUtils;
import com.dreaming.hscj.utils.JsonUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

public class NAExportPresenter extends INAExportContract.Presenter{
    List<ESONObject> allLog = new ArrayList<>();
    List<ESONObject> netNotFoundLog = new ArrayList<>();//本地未匹配记录

    String tubNoFieldName = "__tub__no_field_name___";
    String samplingTimeFieldName = "__sampling__time_field__name__";

    private String startTime;
    private String endTime;

    final ApiConfig.Locate.Query query = Template.getCurrentTemplate().getApiConfig().getQuery();

    int logCount;
    int netCount = 0;
    void count(ESONObject params, int ips) {
        App.Post(()-> mView.onMessage("正在获取记录页数信息！"));
        final AtomicInteger l = new AtomicInteger(0);
        final AtomicInteger r = new AtomicInteger(100000);
        final AtomicInteger m = new AtomicInteger(500);
        boolean bIsFirst = true;

        while(l.get()<r.get()){
            m.set(bIsFirst ? 500 : ((l.get()+r.get())/2));
            bIsFirst = false;
            params.putValue(query.getRequest().getPageIndex(), m.get());

            final AtomicBoolean bIsFinded = new AtomicBoolean(false);
            ApiProvider.requestQuerySamplingHistoryRecordSync(params, new ApiProvider.ISamplingRecordQueryListener() {
                @Override
                public void onSuccess(ESONArray data) {
                    if(data == null || data.length() == 0){
                        r.set(m.get());
                        return;
                    }

                    if(data.length() == ips){
                        l.set(m.get()+1);
                        logCount = l.get() * ips;
                        return;
                    }

                    r.set(m.get()-1);
                    logCount = m.get() * ips + data.length();
                }

                @Override
                public void onFailure(String err) {}
            });
            if(bIsFinded.get()){
                break;
            }
        }

        if(logCount == 0){
            hideWaitDialog();
            App.Post(()-> mView.showEmptyDialog());
            return;
        }
        successCount = 0;
        failureCount = 0;
        netCount     = logCount;
        App.Post(()->mView.onNetCheckedUpdate(netCount));
    }

    private void query(int page, ESONObject params, int ips){
        App.Post(()-> mView.onMessage(String.format("正在获取%d页记录，共%d页，%d条记录。",page+1,logCount/ips,logCount)));
        params.putValue(query.getRequest().getPageIndex(),page);
        ApiProvider.requestQuerySamplingHistoryRecordSync(params, new ApiProvider.ISamplingRecordQueryListener() {
            @Override
            public void onSuccess(ESONArray data) {
                if(data == null || data.length() == 0) return;
                List<ESONObject> lstData = JsonUtils.parseToList(data);
                allLog.addAll(lstData);
                match(page,lstData);
                int progress = (int)(((float)allLog.size()) / logCount * 95) ;
                App.Post(()->mView.onProgress(Math.min(95,progress)));
                if(data.length() == ips){
                    query(page+1,params,ips);
                }
            }

            @Override
            public void onFailure(String err) {}
        });
    }

    int successCount = 0;
    int failureCount = 0;
    private List<ESONObject> lstDbInfo = new ArrayList<>();
    private Map<String,String> mSetter = new HashMap<>();
    private Map<Integer,List<ESONObject>> mLocalSuccess = new HashMap<>();
    private Map<Integer,Map<String,ESONObject>> mLocalFailure = new HashMap<>();
    private void match(int page, List<ESONObject> lstData){
        App.Post(()-> mView.onMessage(String.format("正在匹配第%d页本地数据！",page)));

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

            int selectedDb = Constants.DBConfig.getSelectedDatabase().getJSONValue("id",-1);
            try {
                for(int i=0,ni=lstDbInfo.size();i<ni;++i){
                    ESONObject dbInfo = lstDbInfo.get(i);
                    int id = dbInfo.getJSONValue("id",-1);
                    for(int j=0;j<3;++j){
                        if(Constants.DBConfig.setSelectedDatabase(id)) break;
                    }
                    ESONArray arrAll = Template.getCurrentTemplate().getUserOverallDatabase().query(new ArrayList<>(),new ArrayList<>());
                    List<ESONObject> lstAll = JsonUtils.parseToList(arrAll);
                    failureCount += lstAll.size();
                    App.Post(()->mView.onLocalUncheckUpdate(failureCount));
                    Map<String,ESONObject> map = new HashMap<>();
                    for(ESONObject item : lstAll){
                        String idNo = item.getJSONValue(Template.getCurrentTemplate().getIdCardNoFieldName(),"");
                        map.put(idNo,item);
                    }
                    mLocalFailure.put(id,map);
                }
            } finally {
                for(int j=0;j<3;++j){
                    if(Constants.DBConfig.setSelectedDatabase(selectedDb)) break;
                }
            }
        }

        int selectedDb = Constants.DBConfig.getSelectedDatabase().getJSONValue("id",-1);
        try {
            final int allCount = lstData.size();

            for(int i=0,ni=lstDbInfo.size();i<ni;++i){
                ESONObject dbInfo = lstDbInfo.get(i);

                int id = dbInfo.getJSONValue("id",-1);
                for(int j=0;j<3;++j){
                    if(Constants.DBConfig.setSelectedDatabase(id)) break;
                }

                Map<String,ESONObject> mFieldMapper = mLocalFailure.get(id);
                if(mFieldMapper == null){
                    mFieldMapper = new HashMap<>();
                }

                for(int j=0,nj=lstData.size();j<nj;++j){
                    ESONObject item = lstData.get(j);
                    for (Map.Entry<String, String> entry : mSetter.entrySet()) {
                        String dbKey  = entry.getKey();
                        String apiKey = entry.getValue();
                        List<String> lstK = new ArrayList<>(); lstK.add(dbKey);
                        List<Object> lstV = new ArrayList<>(); lstV.add(lstData.get(j).getJSONValue(apiKey,""));
                        ESONArray result = Template.getCurrentTemplate().getUserOverallDatabase().search(lstK,lstV);
                        if(result!=null && result.length() == 1){
                            ESONObject json = result.getArrayValue(0,new ESONObject());
                            if(json.length() == 0) continue;

                            lstData.remove(j);
                            --j;
                            --nj;

                            List<ESONObject> lstSuccess = mLocalSuccess.get(id);
                            if(lstSuccess == null) lstSuccess = new ArrayList<>();
                            lstSuccess.add(json);
                            json.putValue(tubNoFieldName,item.getJSONValue(query.getResponse().getTubNo(),""));
                            json.putValue(samplingTimeFieldName,item.getJSONValue(query.getResponse().getSamplingTime(),""));
                            mLocalSuccess.put(id,lstSuccess);
                            App.Post(()->mView.onLocalCheckedUpdate(++successCount));

                            String idNo = json.getJSONValue(Template.getCurrentTemplate().getIdCardNoFieldName(),"");
                            mFieldMapper.remove(idNo);

                            App.Post(()->mView.onLocalUncheckUpdate(--failureCount));
                        }
                    }
                }
                mLocalFailure.put(id,mFieldMapper);
            }

            netCount -= (allCount - lstData.size());
            App.Post(()->mView.onNetCheckedUpdate(netCount));
            if(!lstData.isEmpty()){
                netNotFoundLog.addAll(lstData);
            }

        } finally {
            for(int j=0;j<3;++j){
                if(Constants.DBConfig.setSelectedDatabase(selectedDb)) break;
            }
        }

    }

    private void export(){
        App.Post(()-> mView.onMessage("开始将导出结果写入Excel！"));
        List<String> lstPaths = new ArrayList<>();
        for(int i=0,ni=lstDbInfo.size();i<ni;++i) {
            ESONObject dbInfo = lstDbInfo.get(i);
            String villageName = dbInfo.getJSONValue("villageName","");
            ExcelUtils.Writer writer = new ExcelUtils.Writer();
            String path = String.format("%s/全员核酸检测数据分析人员明细表(%s).xlsx",FileUtils.getExternalDir(),villageName);
            int id = dbInfo.getJSONValue("id",-1);
            List<ESONObject> lstSuccess = mLocalSuccess.get(id);
            if(lstSuccess == null) lstSuccess = new ArrayList<>();
            int counter = 0;
            for (ESONObject item : lstSuccess) {
                writer.write(0,counter,0,villageName);
                writer.write(0,counter,1,item.getJSONValue(Template.getCurrentTemplate().getIdCardNameFieldName(),""));
                writer.write(0,counter,2,item.getJSONValue(Template.getCurrentTemplate().getIdCardNoFieldName(),""));
                writer.write(0,counter,3,"已采样");
                ++counter;
            }

            Map<String,ESONObject> mFailure = mLocalFailure.get(id);
            if(mFailure == null) mFailure = new HashMap<>();
            for (Map.Entry<String, ESONObject> entry : mFailure.entrySet()) {
                ESONObject item = entry.getValue();
                writer.write(0,counter,0,villageName);
                writer.write(0,counter,1,item.getJSONValue(Template.getCurrentTemplate().getIdCardNameFieldName(),""));
                writer.write(0,counter,2,item.getJSONValue(Template.getCurrentTemplate().getIdCardNoFieldName(),""));
                writer.write(0,counter,3,"未采样");
                ++counter;
            }
            lstPaths.add(path);
            writer.save(path);
        }

        ExcelUtils.Writer writer = new ExcelUtils.Writer();

        int counter = 1;
        Api api = Template.getCurrentTemplate().apiOf(6);
        for (ESONObject item : netNotFoundLog) {
            ESONObject data = DataParser.parseObjects(item,api.getResponse().getFields(),false);
            List<ApiParam> params = api.getResponse().getFields();
            for(int i=0,ni=params.size();i<ni;++i){
                if(counter == 1){
                    writer.write(0,0,i,params.get(i).getDescription());
                }
                writer.write(0,counter,i,data.getJSONValue(params.get(i).getName(),""));
            }
            ++counter;
        }

        String path = String.format("%s/hscj_export_%s.xlsx",FileUtils.getExternalDir(),new SimpleDateFormat("yyyy_MM_dd__HH_mm_ss").format(new Date()));
        writer.save(path);
        lstPaths.add(path);

        App.Post(()-> mView.onProgress(100));
        App.Post(()-> mView.onMessage("导出完成！"));
        App.Post(()-> mView.showSuccessDialog(lstPaths));
    }


    void showWaitDialog(){
        App.Post(()-> LoadingDialog.showDialog("Wait_For_Export",App.sInstance.getCurrentActivity()));
    }

    void hideWaitDialog(){
        App.Post(()->LoadingDialog.dismissDialog("Wait_For_Export"));
    }

    @Override
    void export(String startTime,String endTime) {
        this.startTime = startTime;
        this.endTime   = endTime;

        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            try {
                showWaitDialog();
                Map<String, ApiParam> mMapper = Template.getCurrentTemplate().apiOf(6).getRequest().getFieldMapper();
                int iPageSize = 10;
                try {
                    iPageSize = Integer.parseInt(mMapper.get(query.getRequest().getPageSize()).getDefaultValue());
                } catch (Exception e) { }

                final int ips = iPageSize;
                ESONObject params = new ESONObject()
                        .putValue(query.getRequest().getPageSize() ,iPageSize)
                        .putValue(query.getRequest().getStartDate(),startTime)
                        .putValue(query.getRequest().getEndDate()  ,endTime)
                        ;
                count(params,ips);
                query(0,params,ips);
                export();
            } finally {
                hideWaitDialog();
            }
        });

    }

    private List<ESONObject> lstLocalCheckedData = new ArrayList<>();
    @Override
    synchronized List<ESONObject> getLocalCheckedData() {
        if(lstLocalCheckedData.isEmpty()){
            for (Map.Entry<Integer, List<ESONObject>> entry : mLocalSuccess.entrySet()) {
                lstLocalCheckedData.addAll(entry.getValue());
            }
        }
        return lstLocalCheckedData;
    }

    private List<ESONObject> lstLocalUncheckData = new ArrayList<>();
    @Override
    synchronized List<ESONObject> getLocalUnCheckData() {
        if(lstLocalUncheckData.isEmpty()){
            for (Map.Entry<Integer, Map<String, ESONObject>> en : mLocalFailure.entrySet()) {
                for (Map.Entry<String, ESONObject> entry : en.getValue().entrySet()) {
                    lstLocalUncheckData.add(entry.getValue());
                }
            }
        }
        return lstLocalUncheckData;
    }

    @Override
    synchronized List<ESONObject> getNetCheckedData() {
        return netNotFoundLog;
    }
}
