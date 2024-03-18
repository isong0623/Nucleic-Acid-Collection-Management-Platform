package com.dreaming.hscj.template.database.impl;

import android.database.Cursor;
import android.util.Pair;

import com.dreaming.hscj.App;
import com.dreaming.hscj.core.ThreadPoolProvider;
import com.dreaming.hscj.template.api.wrapper.ApiParam;
import com.dreaming.hscj.template.database.DatabaseTemplate;
import com.dreaming.hscj.template.database.wrapper.BaseDatabase;
import com.dreaming.hscj.template.database.wrapper.DatabaseConfig;
import com.dreaming.hscj.template.database.wrapper.DatabaseSetting;
import com.dreaming.hscj.template.database.wrapper.ICountListener;
import com.dreaming.hscj.template.database.wrapper.IDeleteListener;
import com.dreaming.hscj.template.database.wrapper.IGetterListener;
import com.dreaming.hscj.template.database.wrapper.ISetterListener;


import net.sqlcipher.database.SQLiteDatabase;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

//核酸采样分组
public class NucleicAcidGroupingDatabase extends BaseDatabase {

    public static NucleicAcidGroupingDatabase init(ESONObject data, UserInputGuideDatabase db, DatabaseSetting setting) throws Exception{
        DatabaseConfig config = DatabaseConfig.parse("DB",data);
        NucleicAcidGroupingDatabase database = new NucleicAcidGroupingDatabase(config, setting);

        String sIdField   = db.getIdFieldName();
        String sNameField = db.getNameFiledName();

        if(config.getFields().size()<3){
            throw new InvalidParameterException("DB[2][\"fields\"]数组长度不能少于3！");
        }

        boolean b1 = false;
        boolean b2 = false;
        for(ApiParam param:config.getFields()){
            if(param.getName().equals(sIdField  )) b1 = true;
            if(param.getName().equals(sNameField)) b2 = true;
        }
        if(!b1 || !b2){
            throw new InvalidParameterException("DB[2][\"fields\"]中没有没有完全包含DB[0][\"fields\"]所有字段！");
        }
        database.sGroupIdField = config.getFields().get(0).getName();
        database.sCardIdField  = config.getFields().get(1).getName();
        database.sNameField    = config.getFields().get(2).getName();

        return database;
    }
    final DatabaseSetting setting;
    private NucleicAcidGroupingDatabase(DatabaseConfig config,DatabaseSetting setting) throws Exception{
        super(config);
        this.setting = setting;
        if(config.getType()!=2) throw new InvalidParameterException("数据库类型不匹配！");
    }

    @Override
    protected List<String> getPrimaryKey() {
        return new ArrayList<String>(){{
            add(getGroupIdFieldName());
            add(getCardIdFieldName());
        }};
    }

    private String sGroupIdField;
    public String getGroupIdFieldName(){
        return sGroupIdField;
    }

    private String sCardIdField;
    public String getCardIdFieldName(){
        return sCardIdField;
    }

    private String sNameField;
    public String getNameFiledName(){
        return sNameField;
    }

    @Override
    protected String getPassword() {
        return getConfig().getPassword();
    }

    @Override
    protected String getDatabaseName() {
        return "_grouping.db";
    }

    @Override
    protected String getTableName() {
        return "grouping";
    }

    public void addPeopleToGroup(ESONObject data, ISetterListener listener){
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            Pair<List<String>,List<Object>> pKV = null;
            try {
                pKV = parseJsonToKV(data);
            } catch (Exception e) {
                post(()->listener.onFailure(e.getMessage()));
                return;
            }

            Pair<List<String>,List<Object>> pKVPrimary =  parseToPrimaryKV(pKV.first,pKV.second);
            int count = rawCount(pKVPrimary.first,pKVPrimary.second);
            if(count==0){//新增
                String group = data.getJSONValue(sGroupIdField,"");
                count = rawCount(new ArrayList<String>(){{add(sGroupIdField);}},new ArrayList<Object>(){{add(group);}});
                if(count<0) {
                    post(()->listener.onFailure("数据保存异常！"));
                    return;
                }
                if(count>=setting.getGroupMemberNum()){
                    post(()->listener.onFailure("组员不能超过"+setting.getGroupMemberNum()+"人！"));
                    return;
                }
            }

            try {
                boolean result = insertOrUpdate(pKV.first,pKV.second);
                if(result){
                    try { db.delete(data.getJSONValue(getCardIdFieldName(),"")); } catch (Exception e) { }
                    post(()->listener.onSuccess());
                }
                else{
                    post(()->listener.onFailure("数据保存失败！"));
                }
            } catch (Exception e) {
                e.printStackTrace();
                post(()->listener.onFailure(e.getMessage()));
            }
        });
    }

    public void addPeopleToGroupSync(ESONObject data, ISetterListener listener){
        Pair<List<String>,List<Object>> pKV = null;
        try {
            pKV = parseJsonToKV(data);
        } catch (Exception e) {
            if(listener!=null) listener.onFailure(e.getMessage());
            return;
        }

        Pair<List<String>,List<Object>> pKVPrimary =  parseToPrimaryKV(pKV.first,pKV.second);
        int count = rawCount(pKVPrimary.first,pKVPrimary.second);
        if(count==0){//新增
            String group = data.getJSONValue(sGroupIdField,"");
            count = rawCount(new ArrayList<String>(){{add(sGroupIdField);}},new ArrayList<Object>(){{add(group);}});
            if(count<0) {
                if(listener!=null) listener.onFailure("数据保存异常！");
                return;
            }
            if(count>=setting.getGroupMemberNum()){
                if(listener!=null) listener.onFailure("组员不能超过"+setting.getGroupMemberNum()+"人！");
                return;
            }
        }

        try {
            boolean result = insertOrUpdate(pKV.first,pKV.second);
            if(result){
                if(listener!=null) listener.onSuccess();
            }
            else{
                if(listener!=null) listener.onFailure("数据保存失败！");
            }
        } catch (Exception e) {
            if(listener!=null) listener.onFailure(e.getMessage());
        }
    }

    NoneGroupingDatabase db;
    public void attachNoneGroupingDatabase(NoneGroupingDatabase db){
        this.db = db;
    }

    public void deletePeople(String idCard, IDeleteListener listener){
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            Pair<List<String>,List<Object>> pKV = null;
            try {
                pKV = parseJsonToKV(new ESONObject().putValue(getCardIdFieldName(),idCard));
            } catch (Exception e) {
                if(listener!=null) post(()->listener.onFailure(e.getMessage()));
                return;
            }
            try {
                ESONArray result = query(pKV.first,pKV.second);
                int delete = delete(pKV.first,pKV.second);
                if(listener!=null) App.Post(()->listener.onSuccess(delete));
                if(result.length()>0){
                    String name = result.getArrayValue(0,new ESONObject()).getJSONValue(getNameFiledName(),"");
                    try { db.log(idCard,name); } catch (Exception e) { }
                }
            } catch (Exception e) {
                e.printStackTrace();
                if(listener!=null) post(()->listener.onFailure(e.getMessage()));
            }
        });
    }

    public void deletePeopleWithGroup(String idCardId, String groupId, IDeleteListener listener){
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            Pair<List<String>,List<Object>> pKV = null;
            try {
                pKV = parseJsonToKV(new ESONObject().putValue(getCardIdFieldName(),idCardId).putValue(getGroupIdFieldName(),groupId));
            } catch (Exception e) {
                post(()->listener.onFailure(e.getMessage()));
                return;
            }
            try {
                ESONArray result = query(pKV.first,pKV.second);
                int delete = delete(pKV.first,pKV.second);
                App.Post(()->listener.onSuccess(delete));
                if(result.length()>0){
                    String name = result.getArrayValue(0,new ESONObject()).getJSONValue(getNameFiledName(),"");
                    try { db.log(idCardId,name); } catch (Exception e) { }
                }
            } catch (Exception e) {
                e.printStackTrace();
                post(()->listener.onFailure(e.getMessage()));
            }
        });
    }

    public void findPeople(String idCardId, IGetterListener listener){
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            ThreadPoolProvider.getFixedThreadPool().execute(()->{
                Pair<List<String>,List<Object>> pKV = null;
                try {
                    pKV = parseJsonToKV(new ESONObject().putValue(getCardIdFieldName(),idCardId));
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
        });
    }

    public void deleteGroup(String groupId,ISetterListener listener){
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            List<String> k = new ArrayList<>();
            k.add(getGroupIdFieldName());

            List<Object> v = new ArrayList<>();
            v.add(groupId);

            ESONArray array = query(k,v);

            int count = delete(k,v);
            if(count>0){
                App.Post(()->listener.onSuccess());
                for(int i=0,ni=array.length();i<ni;++i){
                    ESONObject o    = array.getArrayValue(i,new ESONObject());
                    String name     = o.getJSONValue(getNameFiledName(),"");
                    String idCardId = o.getJSONValue(getCardIdFieldName(),"");

                    try { db.log(idCardId,name); } catch (Exception e) { }
                }
            }
            else
                App.Post(()->listener.onFailure("删除失败！"));
        });

    }

    public int countSync(){
        return rawCount(new ArrayList<>(),new ArrayList<>());
    }

    public int countGroup(String groupId){
        List<String> k = new ArrayList<>(); k.add(sGroupIdField);
        List<Object> v = new ArrayList<>(); v.add(groupId);
        return rawCount(k,v);
    }

    public int countGroup(){
        Cursor cursor = getDatabase().query("SELECT COUNT(0) FROM (SELECT COUNT(`"+getGroupIdFieldName()+"`) FROM `"+getTableName()+"` GROUP BY `"+getGroupIdFieldName()+"`);");
        try {
            if(cursor.moveToNext()){
                return cursor.getInt(0);
            }
        } finally {
            try { cursor.close(); } catch (Exception e) { }
        }
        return -1;
    }

    public void getGroupingList(int page, int pageSize, IGetterListener listener){
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            try {
                Cursor cursor = getDatabase().query("SELECT `"+getGroupIdFieldName()+"`,"+"COUNT(`"+getGroupIdFieldName()+"`) as `peopleNum` FROM `"+getTableName()+"` GROUP BY `"+getGroupIdFieldName()+"` LIMIT "+pageSize+" OFFSET "+page*pageSize+";");
                ESONArray result = parseCursorToArrayNoChange(cursor);
                post(()->listener.onSuccess(result));
            } catch (Exception e) {
                e.printStackTrace();
                post(()->listener.onFailure(e.getMessage()));
            }
        });
    }

    public void getGroupingList(IGetterListener listener){
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            try {
                Cursor cursor = getDatabase().query("SELECT `"+getGroupIdFieldName()+"`,"+"COUNT(`"+getGroupIdFieldName()+"`) as `peopleNum` FROM `"+getTableName()+"` GROUP BY `"+getGroupIdFieldName()+"`;");
                ESONArray result = parseCursorToArrayNoChange(cursor);
                post(()->listener.onSuccess(result));
            } catch (Exception e) {
                e.printStackTrace();
                post(()->listener.onFailure(e.getMessage()));
            }
        });
    }

    public void getGroupInfo(String groupId, IGetterListener listener){
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            ThreadPoolProvider.getFixedThreadPool().execute(()->{
                Pair<List<String>,List<Object>> pKV = null;
                try {
                    pKV = parseJsonToKV(new ESONObject().putValue(getGroupIdFieldName(),groupId));
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
        });
    }
}
