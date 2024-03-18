package com.dreaming.hscj.template.database.impl;

import android.util.Log;

import com.dreaming.hscj.core.ThreadPoolProvider;
import com.dreaming.hscj.template.database.wrapper.BaseDatabase;
import com.dreaming.hscj.template.database.wrapper.DatabaseConfig;

import net.sqlcipher.database.SQLiteDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

public class NoneGroupingDatabase extends BaseDatabase {
    private static final String TAG = NoneGroupingDatabase.class.getSimpleName();
    private static final String sInit = "{\n" +
            "        \"type\": 0,\n" +
            "        \"description\": \"核酸未分组成员\",\n" +
            "        \"password\": \"oiV0ZS3Gwpv03dxL\",\n" +
            "        \"fields\": [\n" +
            "          [\"idCard\"  ,\"STRING\",\"身份号\"  ,\"NOEMPTY\"],\n" +
            "          [\"fullName\",\"STRING\",\"身份证姓名\",\"NOEMPTY\"]\n" +
            "        ],\n" +
            "        \"usage\": {\n" +
            "          \"setter\": {\n" +
            "            \"idCard\"  : [\"INPUT\"],\n" +
            "            \"fullName\": [\"INPUT\"]\n" +
            "          },\n" +
            "          \"getter\": {\n" +
            "            \"idCard\"  : [\"OUTPUT\"],\n" +
            "            \"fullName\": [\"OUTPUT\"]\n" +
            "          }\n" +
            "        }\n" +
            "      }";

    public static NoneGroupingDatabase parse(UserInputGuideDatabase db1,UserOverallDatabase db2,NucleicAcidGroupingDatabase db3){
        return new NoneGroupingDatabase(db1, db2, db3);
    }

    @Override
    protected String getPassword() {
        return getConfig().getPassword();
    }

    @Override
    protected String getDatabaseName() {
        return "_none_grouping.db";
    }

    @Override
    protected String getTableName() {
        return "ng";
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
    protected List<String> getPrimaryKey() {
        List<String> lst = new ArrayList<>();
        lst.add(db1.getIdFieldName());
        return lst;
    }

    final UserInputGuideDatabase      db1;
    final UserOverallDatabase         db2;
    final NucleicAcidGroupingDatabase db3;
    private NoneGroupingDatabase(UserInputGuideDatabase db1,UserOverallDatabase db2,NucleicAcidGroupingDatabase db3) {
        super(DatabaseConfig.parse("",new ESONObject(sInit
                .replaceAll("idCard"  ,db1.getIdFieldName())
                .replaceAll("fullName",db1.getNameFiledName())
        )));
        this.db1 = db1;
        this.db2 = db2;
        this.db3 = db3;
    }


    public interface ISyncListener{
        void onStart();
        void onProgress(int progress);
        void onError(String err);
        void onFinish();
    }

    public void sync(ISyncListener listener){
        Log.e(TAG,"begin sync");
        if(listener!=null) post(()->listener.onStart());
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            synchronized (BaseDatabase.class){
                Log.e(TAG,"sync->run");
                int iCount2 = db2.countSync();
                int iCount3 = db3.countSync();
                int iCount4 = count();
                Log.e(TAG,"sync->count2="+iCount2);
                Log.e(TAG,"sync->count3="+iCount3);
                Log.e(TAG,"sync->count4="+iCount4);
                if(iCount4 == iCount2-iCount3){
                    if(listener!=null) post(()->listener.onProgress(100));
                    if(listener!=null) post(()->listener.onFinish());
                    Log.e(TAG,"sync->break and finished!");
                    return;
                }

                ESONArray eQuery2 = db2.query(new ArrayList<>(),new ArrayList<>());
                ESONArray eQuery3 = db3.query(new ArrayList<>(),new ArrayList<>());
                Set<String> s2 = new HashSet<>();
                Set<String> s3 = new HashSet<>();

                Log.e(TAG,"sync->query 0");
                //20%
                for(int i=0,ni=eQuery2.length();i<ni;++i){
                    final int progress = i*20/ni;
                    if(listener!=null) post(()->listener.onProgress(progress));

                    ESONObject e = eQuery2.getArrayValue(i,new ESONObject());
                    String idCardNo = e.getJSONValue(db1.getIdFieldName(),"");
                    if(idCardNo.trim().isEmpty()) continue;
                    s2.add(idCardNo);
                }

                Log.e(TAG,"sync->query 1");
                //20%
                for(int i=0,ni=eQuery3.length();i<ni;++i){
                    final int progress = i*20/ni + 20;
                    if(listener!=null) post(()->listener.onProgress(progress));

                    ESONObject e = eQuery3.getArrayValue(i,new ESONObject());
                    String idCardNo = e.getJSONValue(db1.getIdFieldName(),"");
                    if(idCardNo.trim().isEmpty()) continue;
                    if(!s2.contains(idCardNo)){
                        db3.deletePeople(idCardNo,null);
                        continue;
                    }
                    s3.add(idCardNo);
                }

                Log.e(TAG,"sync->query 2");
                //%20
                List<ESONObject> lstNoneGroup = new ArrayList<>();
                for(int i=0,ni=eQuery2.length();i<ni;++i){
                    final int progress = i*20/ni + 40;
                    if(listener!=null) post(()->listener.onProgress(progress));

                    ESONObject e = eQuery2.getArrayValue(i,new ESONObject());
                    String idCardNo = e.getJSONValue(db1.getIdFieldName(),"");
                    if(idCardNo.trim().isEmpty()) continue;
                    if(s3.contains(idCardNo)) continue;
                    lstNoneGroup.add(e);
                }

                Log.e(TAG,"sync->merge");
                //40%
                SQLiteDatabase db = getDatabase();
                try {
                    db.beginTransaction();

                    int count4 = rawCount(new ArrayList<>(),new ArrayList<>());
                    int delete = delete(new ArrayList<>(),new ArrayList<>());
                    if(count4!=delete) throw new Exception("数据删除失败");

                    for(int i=0,ni=lstNoneGroup.size();i<ni;++i){
                        final int progress = i*40/ni + 40;
                        if(listener!=null) post(()->listener.onProgress(progress));

                        ESONObject e = lstNoneGroup.get(i);
                        List<String> lstKeys = new ArrayList<>();
                        lstKeys.add(db1.getIdFieldName());
                        lstKeys.add(db1.getNameFiledName());

                        List<Object> lstValues = new ArrayList<>();
                        lstValues.add(e.getJSONValue(db1.getIdFieldName(),""));
                        lstValues.add(e.getJSONValue(db1.getNameFiledName(),""));

                        if(!insertOrUpdate(lstKeys,lstValues)){
                            throw new Exception("数据插入失败！");
                        }
                    }

                    if(listener!=null) post(()->listener.onProgress(100));
                    db.setTransactionSuccessful();
                } catch (Exception e) {
                    if(listener!=null) post(()->listener.onError(e.getMessage()));
                    Log.e(TAG,"sync->error:"+e.getMessage());
                }
                finally {
                    db.endTransaction();
                    Log.e(TAG,"sync->finished!");
                    if(listener!=null) post(()->listener.onFinish());
                }
            }
        });
    }

    public boolean log(String idCard, String name){
        List<String> keys = new ArrayList<>();
        keys.add(db1.getIdFieldName());
        keys.add(db1.getNameFiledName());

        List<Object> values = new ArrayList<>();
        values.add(idCard);
        values.add(name);

        return insertOrUpdate(keys,values);
    }

    public int delete(String idCard){
        List<String> keys = new ArrayList<>();
        keys.add(db1.getIdFieldName());

        List<Object> values = new ArrayList<>();
        values.add(idCard);

        return delete(keys,values);
    }

    public int count(){
        return rawCount(new ArrayList<>(),new ArrayList<>());
    }

    public ESONArray ask(){
        return query(new ArrayList<>(),new ArrayList<>());
    }

    public ESONArray ask(int page, int pageNum){
        return query(new ArrayList<>(),new ArrayList<>(),page,pageNum);
    }
}
