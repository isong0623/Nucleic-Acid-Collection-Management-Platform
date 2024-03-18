package com.dreaming.hscj.template.database.impl;

import android.database.Cursor;
import android.util.Log;
import android.util.Pair;

import com.dreaming.hscj.core.ThreadPoolProvider;
import com.dreaming.hscj.template.DataParser;
import com.dreaming.hscj.template.api.ApiProvider;
import com.dreaming.hscj.template.api.impl.Api;
import com.dreaming.hscj.template.api.impl.ApiConfig;
import com.dreaming.hscj.template.api.wrapper.ApiParam;
import com.dreaming.hscj.template.database.DatabaseTemplate;
import com.dreaming.hscj.template.database.wrapper.BaseDatabase;
import com.dreaming.hscj.template.database.wrapper.DatabaseConfig;
import com.dreaming.hscj.template.database.wrapper.ICountListener;
import com.dreaming.hscj.template.database.wrapper.IDeleteListener;
import com.dreaming.hscj.template.database.wrapper.IGetterListener;
import com.dreaming.hscj.template.database.wrapper.ISetterListener;
import com.dreaming.hscj.utils.JsonUtils;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

//社区成员数据库
public class UserOverallDatabase extends BaseDatabase {
    private static final String TAG = UserOverallDatabase.class.getSimpleName();
    public static UserOverallDatabase init(ESONObject data,UserInputGuideDatabase db) throws Exception{
        DatabaseConfig config = DatabaseConfig.parse("DB",data);
        UserOverallDatabase database = new UserOverallDatabase(config);

        String sIdField   = db.getIdFieldName();
        String sNameField = db.getNameFiledName();

        boolean b1 = false;
        boolean b2 = false;
        for(int i=0,ni=config.getFields().size();i<ni;++i){
            ApiParam param = config.getFields().get(i);
            if(param.getName().equals(sIdField  )) b1 = true;
            if(param.getName().equals(sNameField)) b2 = true;
        }
        if(!b1 || !b2){
            Log.e(TAG,"error");
            throw new InvalidParameterException("DB[1][\"fields\"]中没有没有完全包含DB[0][\"fields\"]所有字段！");
        }
        database.idField = sIdField;
        database.nmField = sNameField;

        return database;
    }

    private UserOverallDatabase(DatabaseConfig config) throws Exception{
        super(config);
        if(config.getType()!=1) throw new InvalidParameterException("数据库类型不匹配！");
    }

    @Override
    protected List<String> getPrimaryKey() {
        return new ArrayList<String>(){{add(getIdFieldName());}};
    }

    private String idField;
    public String getIdFieldName(){
        return idField;
    }

    private String nmField;
    public String getNameFiledName(){
        return nmField;
    }

    @Override
    protected String getPassword() {
        return getConfig().getPassword();
    }

    @Override
    protected String getDatabaseName() {
        return "_user_overall.db";
    }

    @Override
    protected String getTableName() {
        return "user";
    }

    public void addPeople(ESONObject data, ISetterListener listener){
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            addPeopleSync(data, new ISetterListener() {
                @Override
                public void onSuccess() {
                    post(()->listener.onSuccess());
                }

                @Override
                public void onFailure(String err) {
                    post(()->listener.onFailure(err));
                }
            });
        });
    }
    public void addPeopleSync(ESONObject data, ISetterListener listener){
        synchronized (BaseDatabase.class){

            Pair<List<String>,List<Object>> pKV = null;
            try {
                pKV = parseJsonToKV(data);
            } catch (Exception e) {
                listener.onFailure(e.getMessage());
                return;
            }

            try {

                boolean result = true;
                Pair<List<String>,List<Object>> pKVPrimary = parseToPrimaryKV(pKV.first, pKV.second);
                if(pKVPrimary == null) result= false;

                if(result){
                    if(rawCount(pKVPrimary.first, pKVPrimary.second)>0){
                        result = rawUpdate(pKV.first,pKV.second);
                    }
                    else{
                        result = rawInsert(pKV.first,pKV.second);
                        if(result){
                            try {
                                db.log(data.getJSONValue(getIdFieldName(),""),data.getJSONValue(getNameFiledName(),""));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }

                if(result){
                    listener.onSuccess();
                }
                else{
                    listener.onFailure("数据保存失败！");
                }
            } catch (Exception e) {
                e.printStackTrace();
                listener.onFailure(e.getMessage());
            }
        }
    }

    NoneGroupingDatabase db;
    public void attachNoneGroupingDatabase(NoneGroupingDatabase db){
        this.db = db;
    }
    NucleicAcidGroupingDatabase db3;
    public void attachNucleicAcidGroupingDatabase(NucleicAcidGroupingDatabase db){
        db3 = db;
    }

    public void deletePeople(ESONObject data, IDeleteListener listener){
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            Pair<List<String>,List<Object>> pKV = null;
            try {
                pKV = parseJsonToKV(data);
            } catch (Exception e) {
                post(()->listener.onFailure(e.getMessage()));
                return;
            }
            try {
                int delete = delete(pKV.first,pKV.second);
                post(()->listener.onSuccess(delete));
                try { db.delete(new ESONObject().getJSONValue(getIdFieldName(),"")); } catch (Exception e) { }
                db3.deletePeople(new ESONObject().getJSONValue(getIdFieldName(), ""), new IDeleteListener() {
                    @Override
                    public void onSuccess(int count) {}

                    @Override
                    public void onFailure(String err) {}
                });
            } catch (Exception e) {
                e.printStackTrace();
                post(()->listener.onFailure(e.getMessage()));
            }
        });
    }

    public int clear(){
        db.clear();
        return super.clear();
    }

    public void getPeople(ESONObject data, IGetterListener listener){
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            Pair<List<String>,List<Object>> pKV = null;
            try {
                pKV = parseJsonToKV(data);
            } catch (Exception e) {
                post(()->listener.onFailure(e.getMessage()));
                return;
            }

            try {
                ESONArray result = query(pKV.first,pKV.second);
                post(()->listener.onSuccess(result));
            } catch (Exception e) {
                e.printStackTrace();
                post(()->listener.onFailure(e.getMessage()));
            }
        });
    }

    public void getPeople(ESONObject data, int page, int pageSize, IGetterListener listener){
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            Pair<List<String>,List<Object>> pKV = null;
            try {
                pKV = parseJsonToKV(data);
            } catch (Exception e) {
                post(()->listener.onFailure(e.getMessage()));
                return;
            }

            try {
                ESONArray result = query(pKV.first,pKV.second,page,pageSize);
                post(()->listener.onSuccess(result));
            } catch (Exception e) {
                e.printStackTrace();
                post(()->listener.onFailure(e.getMessage()));
            }
        });
    }

    public int countSync(){
        return rawCount(new ArrayList<>(),new ArrayList<>());
    }

    public int countSync(List<String> keys, List<Object> values){
        return rawCount(keys, values);
    }

    public void getList(int page, IGetterListener listener){
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            try {
                ESONArray result = query(new ArrayList<>(),new ArrayList<>(),page,15);
                post(()->listener.onSuccess(result));
            } catch (Exception e) {
                e.printStackTrace();
                post(()->listener.onFailure(e.getMessage()));
            }
        });
    }

    ApiConfig apiConfig;
    public void attachApiConfig(ApiConfig config){
        apiConfig = config;
    }

    public void sync(){
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            List<String> apiId = apiConfig.getId().getId();
            if(apiId.isEmpty()) return;
            if(apiId == null || apiId.isEmpty()) return;
            if(apiId.size() == 1 && getIdFieldName().equals(apiId.get(0))) return;

            Map<String,String> getterMapper = getConfig().getSetMapper().get(3);
            if(getterMapper == null || getterMapper.isEmpty()) return;

            List<String> dbId = new ArrayList();

            for(String apiField:apiId){
                String dbField = getterMapper.get(apiField);
                if(dbField == null) continue;
                if(!getConfig().getFieldMapper().containsKey(dbField)) continue;
                dbId.add(dbField);
            }

            if(dbId.isEmpty()) return;

            StringBuilder sbWhere = new StringBuilder();
            for(String dbField:dbId){
                if(sbWhere.length() != 0) sbWhere.append(" OR ");
                sbWhere.append(String.format("`%s`==NULL OR TRIM('%s')=''",dbField,dbField));
            }

            synchronized (BaseDatabase.class){
                Cursor cursor = getDatabase().query("SELECT * FROM `"+getTableName()+"` WHERE "+sbWhere.toString()+";");
                List<ESONObject> data = JsonUtils.parseToList(parseCursorToArray(cursor));
                for(ESONObject item : data){
                    String idNo = item.getJSONValue(getIdFieldName(),"");
                    if(idNo.trim().isEmpty()) continue;
                    sync(idNo,3);
                }
            }
        });
    }

    public void syncIf(String idNo){
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            List<String> apiId = apiConfig.getId().getId();
            if(apiId.isEmpty()) return;
            if(apiId == null || apiId.isEmpty()) return;
            if(apiId.size() == 1 && getIdFieldName().equals(apiId.get(0))) return;

            Map<String,String> getterMapper = getConfig().getSetMapper().get(3);
            if(getterMapper == null || getterMapper.isEmpty()) return;

            List<String> dbId = new ArrayList();

            for(String apiField:apiId){
                String dbField = getterMapper.get(apiField);
                if(dbField == null) continue;
                if(!getConfig().getFieldMapper().containsKey(dbField)) continue;
                dbId.add(dbField);
            }

            if(dbId.isEmpty()) return;

            StringBuilder sbWhere = new StringBuilder();
            for(String dbField:dbId){
                if(sbWhere.length() != 0) sbWhere.append(" OR ");
                sbWhere.append(String.format("`%s`==NULL OR TRIM('%s')=''",dbField,dbField));
            }
            String sWhere = String.format("`%s`=? AND (%s)",getIdFieldName(),sbWhere.toString());
            List<Object> v = new ArrayList<>();
            v.add(idNo);
            Cursor cursor = getDatabase().query("SELECT * FROM `"+getTableName()+"` WHERE "+sWhere+";",v.toArray());
            List<ESONObject> data = JsonUtils.parseToList(parseCursorToArray(cursor));
            for(ESONObject item : data){
                String idNo2 = item.getJSONValue(getIdFieldName(),"");
                if(idNo2.trim().isEmpty()) continue;
                sync(idNo2,3);
            }
        });
    }

    public void sync(String idNo, int retryCount){
        synchronized (BaseDatabase.class){
            ApiProvider.requestPeopleInfoByIdCardSync(idNo, new ApiProvider.IPeopleInfoListener() {
                @Override
                public void onSuccess(ESONObject data) {
                    if(data == null || data.length() == 0) return;
                    ESONObject save = DataParser.parseApiToDatabase(DatabaseConfig.TYPE_USER_OVERALL, Api.TYPE_GET_PEOPLE_INFO,data);
                    AtomicBoolean bIsSuccess = new AtomicBoolean(false);
                    for(int i=0;i<3&&!bIsSuccess.get();++i){
                        addPeopleSync(save, new ISetterListener() {
                            @Override
                            public void onSuccess() {
                                bIsSuccess.set(true);
                            }

                            @Override
                            public void onFailure(String err) {}
                        });
                    }
                }

                @Override
                public void onFailure(String err) {
                    if(retryCount>0){
                        sync(idNo,retryCount-1);
                    }
                }
            });
        }
    }
}
