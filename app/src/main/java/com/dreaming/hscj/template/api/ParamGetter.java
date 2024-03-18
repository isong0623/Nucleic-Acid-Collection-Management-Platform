package com.dreaming.hscj.template.api;

import com.dreaming.hscj.template.DataParser;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.template.api.wrapper.ApiParam;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import priv.songxusheng.easyjson.ESONObject;

public class ParamGetter{
    int type;
    int subType;
    ESONObject data;
    List<ApiParam> params;

    Map<String,ApiParam> mParamMapper = new HashMap<>();

    public static ParamGetter fromApi(int type, ESONObject data){
        return new ParamGetter(0,type,data);
    }

    public static ParamGetter fromDb(int type, ESONObject data){
        return new ParamGetter(1,type,data);
    }

    //input for db1
    public static ParamGetter fromInput(ESONObject data){
        return new ParamGetter(2,1, data);
    }

    private static List<ApiParam> getParams(int type, int subType, ESONObject data){
        switch (type){
            case 0:
                return Template.getCurrentTemplate().apiOf(subType).getRequest().getParams();
            case 1:
                switch (subType){
                    case 0:
                        return Template.getCurrentTemplate().getUserInputGuideDatabase().getConfig().getFields();
                    case 1:
                        return Template.getCurrentTemplate().getUserOverallDatabase().getConfig().getFields();
                    case 2:
                        return Template.getCurrentTemplate().getNucleicAcidGroupingDatabase().getConfig().getFields();
                    case 3:
                        return Template.getCurrentTemplate().getNoneGroupingDatabase().getConfig().getFields();
                    case 4:
                        return Template.getCurrentTemplate().getDaySamplingLogDatabase().getConfig().getFields();
                }
                break;
            case 2:
            case 3:
                Map<String,ApiParam> mMapper = Template.getCurrentTemplate().getUserOverallDatabase().getConfig().getFieldMapper();
                List<ApiParam> result = new ArrayList<>();
                for (Iterator<String> it = data.keys(); it.hasNext(); ) {
                    String key = it.next();
                    ApiParam p = mMapper.get(key);
                    if(p == null) continue;
                    result.add(p);
                }
                return result;
        }

        return new ArrayList<>();
    }

    public ParamGetter(int type, int subType, ESONObject data){
        this.type    = type;
        this.subType = subType;
        this.params  = getParams(type, subType, data);
        this.data    = DataParser.parseObjects(data,this.params,true);
        for(ApiParam p : params){
            mParamMapper.put(p.getName(),p);
        }
    }

    public ParamGetter(int type, int subType, ESONObject data, List<ApiParam> params){
        this.type    = type;
        this.subType = subType;
        this.params  = params;
        this.data    = DataParser.parseObjects(data,this.params,true);
        for(ApiParam p : params){
            mParamMapper.put(p.getName(),p);
        }
    }

    public boolean containsV(String key){
        if(!data.has(key)) return false;
        ApiParam p = mParamMapper.get(key);
        if(p == null) return false;
        try {
            Object v = data.get(key);
            if(v == null && !p.canEmpty()) return false;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Object getValue(String key) {
        try {
            return data.get(key);
        } catch (JSONException e) {
            return null;
        }
    }

    public static boolean containsApiT(List<ParamGetter> getters, int type){
        return containsT(getters,0,type);
    }

    public static boolean containsDbT(List<ParamGetter> getters, int type){
        return containsT(getters,1,type);
    }

    public static boolean containsInput(List<ParamGetter> getters){
        return containsT(getters,2,1);
    }

    public static boolean containsExcelT(List<ParamGetter> getters){
        return containsT(getters,3,0);
    }

    private static boolean containsT(List<ParamGetter> getters, int type, int subType){
        if(getters == null) return false;
        for(ParamGetter getter:getters){
            if(getter.type == type && getter.subType == subType) return true;
        }
        return false;
    }

    public static ParamGetter getApiT(List<ParamGetter> getters, int type){
        return getT(getters,0,type);
    }

    public static ParamGetter getDbT(List<ParamGetter> getters, int type){
        return getT(getters,1,type);
    }

    public static ParamGetter getInputT(List<ParamGetter> getters){
        return getT(getters,2,1);
    }

    public static ParamGetter getExcelT(List<ParamGetter> getters){
        return getT(getters,3,0);
    }

    private static ParamGetter getT(List<ParamGetter> getters, int type, int subType){
        if(getters == null) return null;
        for(ParamGetter getter:getters){
            if(getter.type == type && getter.subType == subType) return getter;
        }
        return null;
    }
}
