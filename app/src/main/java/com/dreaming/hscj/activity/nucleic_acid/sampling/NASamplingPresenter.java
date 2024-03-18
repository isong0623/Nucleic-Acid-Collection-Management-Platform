package com.dreaming.hscj.activity.nucleic_acid.sampling;

import android.util.Log;

import com.dreaming.hscj.App;
import com.dreaming.hscj.core.ThreadPoolProvider;
import com.dreaming.hscj.template.DataParser;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.template.api.ApiProvider;
import com.dreaming.hscj.template.api.ApiTemplate;
import com.dreaming.hscj.template.api.impl.Api;
import com.dreaming.hscj.template.api.impl.ApiConfig;
import com.dreaming.hscj.template.api.impl.ApiListener;
import com.dreaming.hscj.template.database.DatabaseTemplate;
import com.dreaming.hscj.template.database.impl.DaySamplingLogDatabase;
import com.dreaming.hscj.template.database.impl.UserOverallDatabase;
import com.dreaming.hscj.template.database.wrapper.DatabaseConfig;
import com.dreaming.hscj.template.database.wrapper.ISetterListener;
import com.dreaming.hscj.utils.JsonUtils;
import com.dreaming.hscj.utils.ToastUtils;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

public class NASamplingPresenter extends INASamplingContract.Presenter{
    private static String TAG = NASamplingPresenter.class.getSimpleName();
    private String strSearchedBarcode = null;
    @Override
    void setSearchedBarcode(String barcode) {
        strSearchedBarcode = barcode;
    }


    @Override
    void setChangedBarcode(String barcode) {
        strSearchedBarcode = barcode;
    }

    public int maxNum = Template.getCurrentTemplate().getDatabaseSetting().getGroupMemberNum();

    public final String sUserName     = Template.getCurrentTemplate().getUserOverallDatabase().getNameFiledName();
    public final String sIdCardNoName = Template.getCurrentTemplate().getUserOverallDatabase().getIdFieldName();

    private String strChangedBarcode = null;
    private long lastRequestTime = 0L;
    @Override
    void requestTestTubeInfo() {
        if(strSearchedBarcode == null) return;
        if(strSearchedBarcode.isEmpty()) return;
        long now = System.currentTimeMillis();
        if(now - lastRequestTime< 1000L){
            if(strSearchedBarcode.equals(strChangedBarcode)) return;
        }
        lastRequestTime = now;
        strChangedBarcode = strSearchedBarcode;

        ApiProvider.requestSamplingResult(strSearchedBarcode, new ApiProvider.ISamplingResultListener() {
            @Override
            public void onSuccess(ESONArray data) {
                mView.updateView(JsonUtils.parseToList(data));
            }

            @Override
            public void onFailure(String err) {
                ToastUtils.show(err);
                strChangedBarcode = null;
            }
        });

    }

    DaySamplingLogDatabase db5 = Template.getCurrentTemplate().getDaySamplingLogDatabase();
    @Override
    void requestAddPeopleToTestTube(String idCardNo, ISetterListener listener) {
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            UserOverallDatabase db = Template.getCurrentTemplate().getUserOverallDatabase();

            List<String> k = new ArrayList<>(); k.add(db.getIdFieldName());
            List<Object> v = new ArrayList<>(); v.add(idCardNo);
            ESONArray a = db.query(k,v);
            if(a == null || a.length() == 0){
                App.Post(()->listener.onFailure("本地数据查询失败！"));
                return;
            }
            ESONObject o = a.getArrayValue(0,new ESONObject());
            if(o.length() == 0){
                App.Post(()->listener.onFailure("本地数据查询失败！"));
                return;
            }
            final String name = o.getJSONValue(sUserName,"");
            ApiProvider.requestNucleicAcidSampling(strSearchedBarcode, o, new ApiProvider.INCSamplingListener() {
                @Override
                public void onSuccess() {
                    App.Post(()->listener.onSuccess());
                    Template.getCurrentTemplate().getUserOverallDatabase().syncIf(idCardNo);
                    ThreadPoolProvider.getFixedThreadPool().execute(()->{
                        for(int i=0;i<10;++i){
                            Log.e(TAG,"saving sampling log! "+i);
                            if(db5.log(idCardNo,name)) break;
                            try { Thread.sleep(500); } catch (Exception ex) { }
                        }
                    });
                }

                @Override
                public void onFailure(String err) {
                    App.Post(()->listener.onFailure(err));
                }
            });
        });
    }
}
