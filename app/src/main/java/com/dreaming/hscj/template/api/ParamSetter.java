package com.dreaming.hscj.template.api;

import com.dreaming.hscj.template.DataParser;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.template.api.wrapper.ApiParam;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

public class ParamSetter{
    int type;
    ESONObject data1;
    List<ESONObject> data2;
    Object data3;
    String itemType;
    List<ApiParam> params;


    Map<String,ApiParam> mParamMapper = new HashMap<>();

    public ParamSetter(int type, Object data3){
        this.type    = type;
        this.params  = Template.getCurrentTemplate().apiOf(type).getResponse().getFields();
        if(data3 == null){
            itemType = "NULL";
        }
        else if(data3 instanceof String) itemType = ApiParam.TYPE_STRING;
        else if(boolean.class.equals(data3.getClass()) || data3 instanceof Boolean) itemType = ApiParam.TYPE_BOOLEAN;
        else if(int    .class.equals(data3.getClass()) || data3 instanceof Integer) itemType = ApiParam.TYPE_INTEGER;
        else if(double .class.equals(data3.getClass()) || data3 instanceof Double ) itemType = ApiParam.TYPE_DOUBLE;
        else if(long   .class.equals(data3.getClass()) || data3 instanceof Long   ) itemType = ApiParam.TYPE_LONG;
        this.data3 = data3;
    }

    public ParamSetter(int type,  ESONObject data1){
        this.type    = type;
        this.itemType= "MAP";
        this.params  = Template.getCurrentTemplate().apiOf(type).getResponse().getFields();
        if(data1 == null || data1.length() == 0){
            this.data1 = data1;
        }
        else{
            this.data1 = DataParser.parseObjects(data1,this.params,true);
        }
        for(ApiParam p : params){
            mParamMapper.put(p.getName(),p);
        }
    }

    public ParamSetter(int type,  ESONArray data2){
        this.type    = type;
        this.itemType= "ARRAY";
        this.params  = Template.getCurrentTemplate().apiOf(type).getResponse().getFields();
        this.data2   = new ArrayList<>();
        for(int i=0,ni=data2.length();i<ni;++i){
            ESONObject obj = data2.getArrayValue(i,new ESONObject());
            this.data2.add(DataParser.parseObjects(obj,this.params,true));
        }
        for(ApiParam p : params){
            mParamMapper.put(p.getName(),p);
        }
    }

    public boolean isMap(){
        return "MAP".equals(itemType);
    }

    public boolean isArray(){
        return "ARRAY".equals(itemType);
    }

    public boolean containsV(int index, String key){
        ApiParam p = mParamMapper.get(key);
        if(p == null) return false;
        if(index<0 || index>=data2.size()) return false;
        ESONObject data = data2.get(index);
        if(!data.has(key)) return false;
        try {
            Object v = data.get(key);
            if(v == null && !p.canEmpty()) return false;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean containsV(String key){
        if(!data1.has(key)) return false;
        ApiParam p = mParamMapper.get(key);
        if(p == null) return false;
        try {
            Object v = data1.get(key);
            if(v == null && !p.canEmpty()) return false;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean containsV(){
        return data3 != null;
    }

    public Object getValue(int index, String key) {
        try {
            return data2.get(index).get(key);
        } catch (Exception e) {
            return null;
        }
    }

    public Object getValue(String key) {
        try {
            return data1.get(key);
        } catch (Exception e) {
            return null;
        }
    }

    public Object getValue() {
        return data3;
    }

    public static boolean containsT(List<ParamSetter> setters, int type){
        if(setters == null) return false;
        for(ParamSetter getter:setters){
            if(getter.type == type ) return true;
        }
        return false;
    }

    public static ParamSetter getT(List<ParamSetter> setters, int type){
        if(setters == null) return null;
        for(ParamSetter setter:setters){
            if(setter.type == type ) return setter;
        }
        return null;
    }

}
