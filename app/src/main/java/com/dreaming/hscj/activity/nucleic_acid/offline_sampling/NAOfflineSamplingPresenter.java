package com.dreaming.hscj.activity.nucleic_acid.offline_sampling;

import com.dreaming.hscj.App;
import com.dreaming.hscj.core.ThreadPoolProvider;
import com.dreaming.hscj.entity.OfflineExcel;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.utils.JsonUtils;
import com.tencent.bugly.proguard.A;

import org.apache.xmlbeans.impl.piccolo.util.DuplicateKeyException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

public class NAOfflineSamplingPresenter extends INAOfflineSamplingContract.Presenter{

    public final int    maxNum        = Template.getCurrentTemplate().getDatabaseSetting().getGroupMemberNum();
    public final String sUserName     = Template.getCurrentTemplate().getUserOverallDatabase().getNameFiledName();
    public final String sIdCardNoName = Template.getCurrentTemplate().getUserOverallDatabase().getIdFieldName();
    public final String sPhone        = Template.getCurrentTemplate().getPhoneFieldName();

    private OfflineExcel excel;
    void setOfflineExcel(OfflineExcel excel){
        this.excel = excel;
        App.Post(()->mView.setOfflineFileName(excel.getConfig().path.substring(excel.getConfig().path.lastIndexOf(File.separator)+1)));
        App.Post(()->mView.showBarcodeInputViewAndEnableKeyAct());
    }

    String getPath(){
        return excel.getConfig().path;
    }

    void close() throws Exception{
        if(excel == null) return;
        excel.close();
    }

    @Override
    void write(String tubNo, String idNo, String name, String phone) throws IOException, DuplicateKeyException {
        excel.sampling(tubNo, name, idNo, phone);
    }

    void delete(String tubNo,String idNo) throws Exception{
        excel.deleteSampling(tubNo,idNo);
    }

    void matchLocalName(String toMatch){
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            List<String> lstK = new ArrayList<>(); lstK.add(sUserName);
            List<Object> lstV = new ArrayList<>(); lstV.add(toMatch);
            ESONArray result = Template.getCurrentTemplate().getUserOverallDatabase().search(lstK,lstV);

            List<ESONObject> lstR = JsonUtils.parseToList(result);

            List<String> lstShown = new ArrayList<>();
            for(int i=0,ni=lstR.size();i<ni;++i){
                ESONObject item = lstR.get(i);
                String name = item.getJSONValue(sUserName,"").trim();
                String id   = item.getJSONValue(sIdCardNoName,"").trim();
                if(name.isEmpty() || id.isEmpty()){
                    lstR.remove(i);
                    --i;
                    --ni;
                    continue;
                }
                lstShown.add(name +" "+id.substring(13));
            }
            if(lstShown.isEmpty()) return;
            App.Post(()->mView.autoShownMatchName(lstR,lstShown));
        });
    }

    void matchLocalId(String toMatch){
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            List<String> lstK = new ArrayList<>(); lstK.add(sIdCardNoName);
            List<Object> lstV = new ArrayList<>(); lstV.add(toMatch);
            ESONArray result = Template.getCurrentTemplate().getUserOverallDatabase().search(lstK,lstV);

            List<ESONObject> lstR = JsonUtils.parseToList(result);

            List<String> lstShown = new ArrayList<>();
            for(int i=0,ni=lstR.size();i<ni;++i){
                ESONObject item = lstR.get(i);
                String name = item.getJSONValue(sUserName,"").trim();
                String id   = item.getJSONValue(sIdCardNoName,"").trim();
                if(name.isEmpty() || id.isEmpty()){
                    lstR.remove(i);
                    --i;
                    --ni;
                    continue;
                }
                lstShown.add(id + " " + name);
            }
            if(lstShown.isEmpty()) return;
            App.Post(()->mView.autoShownMatchId(lstR,lstShown));
        });
    }

    void setTubNo(String tubNo){
        this.tubNo = tubNo;
    }

    private String tubNo;
    String getTubNo(){
        return tubNo;
    }

    List<String> getOfflineTubNoList(){
        List<String> result = new ArrayList<>();
        Map<String,List<ESONObject>> m = excel.getAllSampling();
        for (Map.Entry<String, List<ESONObject>> entry : m.entrySet()) {
            if(entry.getValue() == null || entry.getValue().isEmpty()) continue;
            result.add(String.format("%s 共%d人",entry.getKey(),entry.getValue().size()));
        }

        return result;
    }

    List<ESONObject> getOfflineViewList(){
        Map<String,List<ESONObject>> m = excel.getAllSampling();
        return m.get(getTubNo());
    }

    int getOfflineInput(){
        List lst = excel.getAllSampling().get(tubNo);
        if(lst == null) return 0;
        return lst.size();
    }
}
