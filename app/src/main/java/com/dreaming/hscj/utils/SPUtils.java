package com.dreaming.hscj.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import com.dreaming.hscj.App;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.template.database.DatabaseTemplate;
import com.dreaming.hscj.utils.algorithm.DecryptUtils;
import com.dreaming.hscj.utils.algorithm.EncryptUtils;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SPUtils {

    private static final Map<String,SharedPreferences> mMapper = new HashMap<>();
    private static SharedPreferences getOrCreate(String name){
        if(name == null) name = "default";
        SharedPreferences result = mMapper.get(name);
        if(result == null) return App.sInstance.getSharedPreferences(name, Context.MODE_PRIVATE);
        return result;
    }

    public static SPUtils withDB(){
        return new SPUtils(Template.getCurrentTemplate().getDatabaseSetting().getUnifySocialCreditCodes());
    }

    public static SPUtils with(String group){
        return new SPUtils(group);
    }

    public static SPUtils Sys = new SPUtils("settings");

    private static final String secret = "yloNF86Uwz4ePpcTsGIRXE72f51RTQa3";

    private String encodeValue(String key, String value){
        String length = String.valueOf(key.length());
        String keySecret = secret.substring(0,secret.length()-length.length()) + length;
        try {
            return EncryptUtils.encryptAES(value,keySecret,"AES/ECB/PKCS7Padding");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Base64.encodeToString(value.getBytes(Charset.forName("utf-8")),Base64.DEFAULT);
    }
    private String decodeValue(String key, String value){
        String length = String.valueOf(key.length());
        String keySecret = secret.substring(0,secret.length()-length.length()) + length;
        try {
            return DecryptUtils.decryptAES(value,keySecret,"AES/ECB/PKCS7Padding");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new String(Base64.decode(value,Base64.DEFAULT),Charset.forName("utf-8"));
    }


    private String encodeKey(String key){
        String length = String.valueOf(key.length());
        String keySecret = secret.substring(0,secret.length()-length.length()) + length;
        try {
            return EncryptUtils.encryptAES(key,keySecret,"AES/ECB/PKCS7Padding");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Base64.encodeToString(key.getBytes(Charset.forName("utf-8")),Base64.DEFAULT);
    }

    private final SharedPreferences sSharedPreferences;
    public SPUtils(String name){
        sSharedPreferences = mMapper.get(name) == null ? App.sInstance.getSharedPreferences(name, Context.MODE_PRIVATE) : mMapper.get(name);
        mMapper.put(name,sSharedPreferences);
    }

    public boolean commitString(String key, String value) {
        if(key==null){ key = ""; }
        synchronized (key){
            return sSharedPreferences.edit().putString(encodeKey(key), encodeValue(key,value)).commit();
        }
    }

    public String getString(String key) {
        return getString(key,null);
    }
    public String getString(String key, String defaultValue) {
        if(key==null) key = "";
        String value = sSharedPreferences.getString(encodeKey(key),null);
        if(value == null) return defaultValue;

        return decodeValue(key,value);
    }

    public boolean commitInt(String key, int value) {
        if(key == null){
            synchronized (sSharedPreferences){
                return sSharedPreferences.edit().putInt(key, value).commit();
            }
        }
        key = key==null ? null : encodeKey(key);
        synchronized (key){
            return sSharedPreferences.edit().putInt(key, value).commit();
        }
    }

    public int getInt(String key) {
        key = key==null ? null : encodeKey(key);
        return sSharedPreferences.getInt(key, -1);
    }
    public int getInt(String key, int defaultValue) {
        key = key==null ? null : encodeKey(key);
        return sSharedPreferences.getInt(key, defaultValue);
    }

    public boolean commitLong(String key, long value) {
        if(key == null){
            synchronized (sSharedPreferences){
                return sSharedPreferences.edit().putLong(key, value).commit();
            }
        }
        key = key==null ? null : encodeKey(key);
        synchronized (key){
            return sSharedPreferences.edit().putLong(key, value).commit();
        }
    }
    public long getLong(String key) {
        key = key==null ? null : encodeKey(key);
        return sSharedPreferences.getLong(key, -1);
    }
    public long getLong(String key, long defaultValue) {
        key = key==null ? null : encodeKey(key);
        return sSharedPreferences.getLong(key, defaultValue);
    }

    public boolean commitBoolean(String key, boolean value) {
        if(key==null){
            synchronized (sSharedPreferences){
                return sSharedPreferences.edit().putBoolean(key, value).commit();
            }
        }
        key = key==null ? null : encodeKey(key);
        synchronized (key){
            return sSharedPreferences.edit().putBoolean(key, value).commit();
        }
    }

    public boolean getBoolean(String key) {
        key = key==null ? null : encodeKey(key);
        return getBoolean(key, false);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        key = key==null ? null : encodeKey(key);
        return sSharedPreferences.getBoolean(key, defaultValue);
    }


}
