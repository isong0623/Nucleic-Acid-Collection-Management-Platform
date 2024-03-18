package com.dreaming.hscj.template.database.impl;

import android.database.Cursor;
import android.util.Pair;

import com.dreaming.hscj.core.ThreadPoolProvider;
import com.dreaming.hscj.template.database.DatabaseTemplate;
import com.dreaming.hscj.template.database.wrapper.BaseDatabase;
import com.dreaming.hscj.template.database.wrapper.DatabaseConfig;
import com.dreaming.hscj.template.database.wrapper.ISetterListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

//核酸本地采样记录
public class DaySamplingLogDatabase extends BaseDatabase {
    private static final String sInit = "{\n" +
            "        \"type\": 0,\n" +
            "        \"description\": \"核酸本地采样记录\",\n" +
            "        \"password\": \"cypzB6mTmPM177jk\",\n" +
            "        \"fields\": [\n" +
            "          [\"idCard\"  ,\"STRING\",\"身份号\"  ,\"NOEMPTY\"],\n" +
            "          [\"fullName\",\"STRING\",\"身份证姓名\",\"NOEMPTY\"],\n" +
            "          [\"update\",\"DATE:yyyy-MM-dd HH:mm:ss\",\"更新时间\",\"NOEMPTY\"]\n" +
            "        ],\n" +
            "        \"usage\": {\n" +
            "          \"setter\": {\n" +
            "            \"fullName\"    : [\"INPUT\"],\n" +
            "            \"idCard\"  : [\"INPUT\"],\n" +
            "            \"update\"      : [\"INPUT\"]\n" +
            "          },\n" +
            "          \"getter\": {\n" +
            "            \"idCard\"    : [\"OUTPUT\"],\n" +
            "            \"fullName\"  : [\"OUTPUT\"],\n" +
            "            \"update\"    : [\"OUTPUT\"]\n" +
            "          }\n" +
            "        }\n" +
            "      }";

    public static DaySamplingLogDatabase parse(UserInputGuideDatabase db){
        return new DaySamplingLogDatabase(db);
    }

    @Override
    protected String getPassword() {
        return getConfig().getPassword();
    }

    @Override
    protected String getDatabaseName() {
        return "_sampling_log.db";
    }

    @Override
    protected String getTableName() {
        return "logger";
    }

    @Override
    protected List<String> getPrimaryKey() {
        List<String> lst = new ArrayList<>();
        lst.add(db.getIdFieldName());
        return lst;
    }
    final UserInputGuideDatabase db;
    private DaySamplingLogDatabase(UserInputGuideDatabase db) {
        super(DatabaseConfig.parse("",new ESONObject(sInit
                .replaceAll("idCard", db.getIdFieldName())
                .replaceAll("fullName",db.getNameFiledName())
        )));
        this.db = db;
    }

    public String getCardIdFieldName(){
        return db.getIdFieldName();
    }

    public boolean log(String idCard, String name){
        List<String> keys = new ArrayList<>();
        keys.add(db.getIdFieldName());
        keys.add(db.getNameFiledName());
        keys.add("update");

        List<Object> values = new ArrayList<>();
        values.add(idCard);
        values.add(name);
        values.add(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(System.currentTimeMillis()));

        return insertOrUpdate(keys,values);
    }

    public void log(ESONObject data, ISetterListener listener){
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            Pair<List<String>,List<Object>> pKV = null;
            try {
                pKV = parseJsonToKV(data);
            } catch (Exception e) {
                post(()->listener.onFailure(e.getMessage()));
                return;
            }

            try {
                boolean result = insertOrUpdate(pKV.first,pKV.second);
                if(result){
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

    public ESONArray ask(long interval){
        Cursor cursor = getDatabase().query("SELECT * FROM `logger` WHERE `update` > '"+ new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(System.currentTimeMillis()-interval) + "';");
        return parseCursorToArray(cursor);
    }

    public boolean del(String idCardNo){
        List<String> keys = new ArrayList<>();
        keys.add(db.getIdFieldName());

        List<Object> values = new ArrayList<>();
        values.add(idCardNo);
        return delete(keys,values) == 1;
    }

    public int countSync(){
        return rawCount(new ArrayList<>(),new ArrayList<>());
    }

    public int count(long interval){
        Cursor cursor = getDatabase().query("SELECT COUNT(1) FROM `logger` WHERE `update` > '"+ new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(System.currentTimeMillis()-interval) + "';");
        try {
            cursor.moveToNext();
            return cursor.getInt(0);
        } finally {
            try { cursor.close(); } catch (Exception e) { }
        }
    }
}
