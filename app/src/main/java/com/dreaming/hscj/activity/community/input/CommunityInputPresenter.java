package com.dreaming.hscj.activity.community.input;

import com.dreaming.hscj.App;
import com.dreaming.hscj.base.ToastDialog;
import com.dreaming.hscj.core.ThreadPoolProvider;
import com.dreaming.hscj.template.DataParser;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.template.api.ApiProvider;
import com.dreaming.hscj.template.api.ApiTemplate;
import com.dreaming.hscj.template.api.impl.Api;
import com.dreaming.hscj.template.api.wrapper.ApiParam;
import com.dreaming.hscj.template.database.DatabaseTemplate;
import com.dreaming.hscj.template.database.wrapper.DatabaseConfig;
import com.dreaming.hscj.template.database.wrapper.IGetterListener;
import com.dreaming.hscj.template.database.wrapper.ISetterListener;
import com.dreaming.hscj.utils.ToastUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

public class CommunityInputPresenter extends ICommunityInputContract.Presenter{

    public final String sUserName     = Template.getCurrentTemplate().getUserOverallDatabase().getNameFiledName();
    public final String sIdCardNoName = Template.getCurrentTemplate().getUserOverallDatabase().getIdFieldName();

    private ESONObject shownData = new ESONObject();
    @Override
    void updateDataBlock(String key, String value) {
        shownData.putValue(key,value);
    }

    private boolean bIsLocalFirst = true;
    @Override
    void setLocalDataFirst(boolean bIsFirst) {
        if(bIsLocalFirst!=bIsFirst){
            bIsLocalFirst = bIsFirst;
            ThreadPoolProvider.getFixedThreadPool().execute(()->{
                if(bIsLocalFirst){
                    if(dbDataShown.length()!=0){
                        App.Post(()->ToastUtils.show("加载本地数据..."));
                        App.Post(()->mView.showRequestingDialog());
                        update(dbDataShown);
                        App.Post(()->mView.hideRequestingDialog());
                    }
                }
                else{
                    if(netShownData.length()!=0){
                        App.Post(()->ToastUtils.show("加载网络数据..."));
                        App.Post(()->mView.showRequestingDialog());
                        update(netShownData);
                        App.Post(()->mView.hideRequestingDialog());
                    }
                }
            });
        }
    }

    void update(ESONObject newData){
        ESONObject obj = DataParser.parseDatabaseToShown(DatabaseConfig.TYPE_USER_OVERALL,newData);
        for (Iterator<String> it = obj.keys(); it.hasNext(); ) {
            String key = it.next();
            String oldValue = shownData.getJSONValue(key,"");
            String newValue = obj.getJSONValue(key,"");
            if(shownData.has(key) && oldValue.equals(newValue)) continue;
            shownData.putValue(key,newValue);
            App.Post(()->mView.onDataChanged(key,newValue));
        }
    }

    private ESONObject dbData = new ESONObject();
    private ESONObject dbDataShown = new ESONObject();
    long lastReqTimestamp = 0L;
    @Override
    void requestDatabaseData() {
        mView.saveConfig();
        long now = System.currentTimeMillis();
        if(now-lastReqTimestamp<1000) {
            return;
        }
        lastReqTimestamp = now;

        mView.showRequestingDialog();
        String valueOfIdCard = shownData.getJSONValue(sIdCardNoName,"");

        ESONObject item = new ESONObject();
        item.putValue(sIdCardNoName,valueOfIdCard);
        Template.getCurrentTemplate().getUserOverallDatabase().getPeople(item, new IGetterListener() {
            @Override
            public void onSuccess(ESONArray data) {
                if(data==null || data.length()==0){
                    requestNetData();
                    return;
                }
                if(!bIsLocalFirst){
                    dbData = data.getArrayValue(0,new ESONObject());
                    dbDataShown = DataParser.parseDatabaseToShown(DatabaseConfig.TYPE_USER_OVERALL,dbData);
                    requestNetData();
                    return;
                }
                ThreadPoolProvider.getFixedThreadPool().execute(()->{
                    dbData = data.getArrayValue(0,new ESONObject());
                    ESONObject newData = DataParser.parseDatabaseToShown(DatabaseConfig.TYPE_USER_OVERALL,dbData);
                    dbDataShown = newData;
                    update(newData);
                    App.Post(()->mView.hideRequestingDialog());
                    App.Post(()->mView.onLoadedDatabase());
                });
            }
            @Override
            public void onFailure(String err) {
                ToastUtils.show(App.sInstance.getCurrentActivity(),err);
                App.PostDelayed(()->mView.hideRequestingDialog(),100);
            }
        });
    }

    private ESONObject netData = new ESONObject();
    private ESONObject netParsedDbData = new ESONObject();
    private ESONObject netShownData = new ESONObject();
    @Override
    void requestNetData() {
        mView.saveConfig();
        String idCardNo = shownData.getJSONValue(sIdCardNoName,"").toUpperCase();

        ApiProvider.requestPeopleInfoByIdCard(idCardNo, new ApiProvider.IPeopleInfoListener() {
            @Override
            public void onSuccess(ESONObject data) {
                ThreadPoolProvider.getFixedThreadPool().execute(()->{
                    netData = data;
                    ESONObject newData = DataParser.parseApiToDatabase(DatabaseConfig.TYPE_USER_OVERALL,Api.TYPE_GET_PEOPLE_INFO,netData);
                    if(newData.length()==0){
                        App.Post(()->ToastDialog.showCenter(App.sInstance.getCurrentActivity(),"没有查询到相关数据！"));
                        App.Post(()->mView.hideRequestingDialog());
                        return;
                    }
                    netParsedDbData = newData;
                    ESONObject shownData = DataParser.parseDatabaseToShown(DatabaseConfig.TYPE_USER_OVERALL,newData);
                    netShownData = shownData;
                    update(shownData);

                    App.Post(()->mView.onLoadedNetwork());
                    App.Post(()->mView.hideRequestingDialog());
                });
            }

            @Override
            public void onFailure(String err) {
                App.Post(()->ToastUtils.show(App.sInstance.getCurrentActivity(),err));
                App.PostDelayed(()->mView.hideRequestingDialog(),100);
            }
        });
    }

    @Override
    void saveToDatabase() {
        ESONObject savingData = new ESONObject();
        savingData.putValue(sIdCardNoName,shownData.getJSONValue(sIdCardNoName,""));
        ESONObject dbSaveData = DataParser.parseShownToDatabase(DatabaseConfig.TYPE_USER_OVERALL,shownData);

        for (Iterator<String> it = dbSaveData.keys(); it.hasNext(); ) {
            String key = it.next();
            String oldValue =     dbData.getJSONValue(key,"");
            String newValue = dbSaveData.getJSONValue(key,"");
            if(!newValue.equals(oldValue) || !dbData.has(key)){
                savingData.putValue(key,newValue);
            }
        }

        Template.getCurrentTemplate().getUserOverallDatabase().addPeople(savingData, new ISetterListener() {
            @Override
            public void onSuccess() {
                mView.resetView();
            }

            @Override
            public void onFailure(String err) {
                ToastDialog.showCenter(App.sInstance.getCurrentActivity(),err);
            }
        });
    }
}
