package com.dreaming.hscj.template;

import com.dreaming.hscj.template.api.impl.Api;
import com.dreaming.hscj.template.api.wrapper.ApiParam;
import com.dreaming.hscj.template.database.DatabaseTemplate;
import com.dreaming.hscj.template.database.wrapper.DatabaseConfig;

import org.json.JSONException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

public class DataParser {

    public static Boolean parseToBoolean(Object value, ApiParam param, boolean bIsSetter){
        Boolean b = null;
        String defaultValue = param.getDefaultValue();

        if(bIsSetter && value!=null){
            String setter = param.setter(value.toString());
            if(setter!=null){
                try { return Boolean.valueOf(setter); } catch (Exception e) { }
                return !((setter.equalsIgnoreCase("false") || setter.equalsIgnoreCase("0")));
            }
        }

        if(!bIsSetter && value != null){
            String getter = param.getter(value.toString());
            if(getter!=null){
                try { return Boolean.valueOf(getter); } catch (Exception e) { }
                return !((getter.equalsIgnoreCase("false") || getter.equalsIgnoreCase("0")));
            }
        }

        if(value == null){ }
        else if(value.getClass().equals(boolean.class) || value instanceof Boolean){
            b = (boolean) value;
        }
        else if(value.toString().equals("FALSE") || value.toString().equals("0")){
            b = false;
        }

        if(b!=null && !"null".equals(defaultValue)){
            try { b = Boolean.parseBoolean(value.toString()); } catch (Exception e) { }
        }

        return b;
    }

    public static String parseToString(Object value, ApiParam param, boolean bIsSetter){
        String str = null;
        String defaultValue = param.getDefaultValue();

        if(bIsSetter && value!=null){
            String setter = param.setter(value.toString());
            if(setter!=null){
                return setter;
            }
        }

        if(!bIsSetter && value != null){
            String getter = param.getter(value.toString());
            if(getter!=null){
                return getter;
            }
        }

        if(value == null || !(value instanceof String)){
            if(defaultValue == null){}
            else if(ApiParam.DEFAULT_NULL_STR.equals(defaultValue)){
                str = "null";
            }
            else if(!ApiParam.DEFAULT_NO_EMPTY.equals(defaultValue)){
                str = defaultValue;
            }
        }
        else{
            str = value.toString();
        }

        if("null".equals(str) && !ApiParam.DEFAULT_NULL_STR.equals(defaultValue)){
            str = defaultValue;
        }

        return str;
    }

    public static Integer parseToInteger(Object value, ApiParam param, boolean bIsSetter){
        Integer i = null;
        String defaultValue = param.getDefaultValue();

        if(bIsSetter && value!=null){
            String setter = param.setter(value.toString());
            if(setter!=null){
                try { return Integer.parseInt(setter); } catch (Exception e) { }
            }
        }

        if(!bIsSetter && value != null){
            String getter = param.getter(value.toString());
            if(getter!=null){
                try { return Integer.parseInt(getter); } catch (Exception e) { }
            }
        }

        if(value == null){ }
        else if(value.getClass().equals(int.class) || value instanceof Integer){
            i = (Integer) value;
        }
        else {
            try { i = Integer.parseInt(value.toString()); } catch (Exception e) { }
        }
        if(i == null && !"null".equals(defaultValue)){
            try { i = Integer.parseInt(defaultValue); } catch (Exception e) { }
        }
        return i;
    }

    public static Double parseToDouble(Object value, ApiParam param, boolean bIsSetter){
        Double d = null;
        String defaultValue = param.getDefaultValue();

        if(bIsSetter && value!=null){
            String setter = param.setter(value.toString());
            if(setter!=null){
                try { return Double.parseDouble(setter); } catch (Exception e) { }
            }
        }

        if(!bIsSetter && value != null){
            String getter = param.getter(value.toString());
            if(getter!=null){
                try { return Double.parseDouble(getter); } catch (Exception e) { }
            }
        }

        if(value == null){}
        else if(value instanceof String){
            try { d = Double.parseDouble(value.toString()); } catch (Exception e) { }
        }
        else if(value.getClass().equals(double.class) || value instanceof Double){
            d = (Double) value;
        }

        if(d == null && !"null".equals(defaultValue)){
            try { d = Double.parseDouble(defaultValue); } catch (Exception e) { }
        }

        return d;
    }

    public static Long parseToLong(Object value, ApiParam param, boolean bIsSetter){
        Long l = null;
        String defaultValue = param.getDefaultValue();

        if(bIsSetter && value!=null){
            String setter = param.setter(value.toString());
            if(setter!=null){
                try { return Long.parseLong(setter); } catch (Exception e) { }
            }
        }

        if(!bIsSetter && value != null){
            String getter = param.getter(value.toString());
            if(getter!=null){
                try { return Long.parseLong(getter); } catch (Exception e) { }
            }
        }

        if(value == null){ }
        else if(value.getClass().equals(long.class) || value instanceof Long){
            l = (Long) value;
        }
        else {
            l = Long.parseLong(value.toString());
        }

        if(l == null && !"null".equals(defaultValue)){
            try { l = Long.parseLong(defaultValue); } catch (Exception e) { }
        }

        return l;
    }

    public static String parseToDate(Object value, ApiParam param){
        String date = "";
        String type = param.getType();
        String defaultValue = param.getDefaultValue();
        String fmt = type.substring(5);
        SimpleDateFormat format = new SimpleDateFormat(fmt);
        if(value == null){}
        else if(long.class.equals(value.getClass()) || value instanceof Long){
            try { date = format.format(new Date((long)value)); } catch (Exception e) { value = null; }
        }
        else if(value instanceof String){
            String str = (String)value;
            if("null".equals(str) && !ApiParam.DEFAULT_NULL_STR.equals(defaultValue)){
                str = defaultValue;
            }

            try { date = format.format(new Date(Long.parseLong(String.valueOf(value)))); } catch (Exception e) {}

            if(date == null || date.isEmpty()){
                try { date = format.format(format.parse(str)); } catch (Exception e) { value = null; }
            }
        }

        if(value == null){
            if(defaultValue == null){
                date = "";
            }
            else if("NOW".equalsIgnoreCase(defaultValue) || "CURRENT_TIMESTAMP".equalsIgnoreCase(defaultValue)){
                try { date = format.format(new Date(System.currentTimeMillis())); } catch (Exception e) { value = null; }
            }
            else if(ApiParam.DEFAULT_NULL_STR.equals(defaultValue)){
                date = "null";
            }
            else if(ApiParam.DEFAULT_NULL.equals(defaultValue)){
                date = null;
            }
        }

        return date;
    }

    public static Object parseObject(Object value, ApiParam p, boolean bIsSetter){
        switch (p.getType().split(":")[0]){
            case ApiParam.TYPE_STRING:
                return parseToString(value,p,bIsSetter);
            case ApiParam.TYPE_DATE:
                return parseToDate(value,p);
            case ApiParam.TYPE_INTEGER:
                return parseToInteger(value,p,bIsSetter);
            case ApiParam.TYPE_DOUBLE:
                return parseToDouble(value,p,bIsSetter);
            case ApiParam.TYPE_BOOLEAN:
                return parseToBoolean(value,p,bIsSetter);
            case ApiParam.TYPE_LONG:
                return parseToLong(value,p,bIsSetter);
        }

        return null;
    }


    private static DatabaseConfig getDatabaseConfig(int dbType){
        DatabaseConfig config = null;
        switch (dbType){
            case 0:
                config = Template.getCurrentTemplate().getUserInputGuideDatabase().getConfig();
                break;
            case 1:
                config = Template.getCurrentTemplate().getUserOverallDatabase().getConfig();
                break;
            case 2:
                config = Template.getCurrentTemplate().getNucleicAcidGroupingDatabase().getConfig();
                break;
            case 3:
                config = Template.getCurrentTemplate().getNoneGroupingDatabase().getConfig();
                break;
            case 4:
                config = Template.getCurrentTemplate().getDaySamplingLogDatabase().getConfig();
                break;
        }
        return config;
    }

    //
    public static ESONObject parseDatabaseToApi(int dbType, int apiType, ESONObject data){
        DatabaseConfig config = getDatabaseConfig(dbType);
        ESONObject result = new ESONObject();
        Map<String,ApiParam> mFieldsMapper = config.getFieldMapper();
        if(mFieldsMapper==null) mFieldsMapper = new HashMap<>();
        Map<String,String> mApiKeyMapper = config.getGetMapper().get(apiType);
        if(mApiKeyMapper == null) mApiKeyMapper = new HashMap<>();
        for (Iterator<String> it = data.keys(); it.hasNext(); ) {
            String key = it.next();
            ApiParam p = mFieldsMapper.get(key);
            if(p == null) continue;

            Object value = null;
            try { value = data.get(key); } catch (Exception e) { }

            String apiKey = mApiKeyMapper.get(key);
            if(apiKey == null) continue;

            result.putValue(apiKey,value);
        }

        return result;
    }

    public static ESONObject parseObjects(ESONObject data, List<ApiParam> params, boolean bIsSetter){
        ESONObject obj = new ESONObject();

        for(ApiParam p : params){
            String key = p.getName();
            Object value = null;
            try { value = data.get(key); } catch (Exception e) { }
            obj.putValue(key,parseObject(value,p,bIsSetter));
        }

        return obj;
    }
    
    //
    public static ESONObject parseApiToDatabase(int dbType, int apiType,ESONObject data){
        DatabaseConfig config = getDatabaseConfig(dbType);
        ESONObject result = new ESONObject();
        Map<String,ApiParam> mFieldsMapper = config.getFieldMapper();
        Map<String,String> mDBKeyMapper = config.getSetMapper().get(apiType);
        for (Iterator<String> it = data.keys(); it.hasNext(); ) {
            String key = it.next();
            ApiParam p = mFieldsMapper.get(key);
            if(p == null) continue;

            Object value = null;
            try { value = data.get(key); } catch (Exception e) { }

            String dbKey = mDBKeyMapper.get(key);
            if(dbKey==null) continue;

            String type = p.getType();
            if(type == null) continue;

            if(value == null){
                value = p.getDefaultValue();
            }

            if(ApiParam.TYPE_STRING.equals(type) || ApiParam.TYPE_DATE.equals(type.split(":")[0])){
                if(value != null && value instanceof String){
                    if("null".equals(value) && !ApiParam.DEFAULT_NULL_STR.equals(p.getDefaultValue())){
                        value = p.getDefaultValue();
                    }
                }
            }

            result.putValue(dbKey,value);
        }

        return result;
    }

    //
    public static ESONObject parseDatabaseToShown(int dbType, ESONObject data){
        DatabaseConfig config = getDatabaseConfig(dbType);
        ESONObject result = new ESONObject();
        Map<String,ApiParam> mFieldsMapper = config.getFieldMapper();
        for (Iterator<String> it = data.keys(); it.hasNext(); ) {
            String key = it.next();
            ApiParam p = mFieldsMapper.get(key);
            if(p == null) continue;

            Object value = null;
            try { value = data.get(key); } catch (Exception e) { }

            if(value!=null){
                String getter = p.getter(value.toString());
                if(getter!=null){
                    result.putValue(key,getter);
                    continue;
                }
            }

            if(value == null){
                value = p.getDefaultValue();
                if(p.getType().startsWith("DATE")){
                    if("NOW".equalsIgnoreCase(p.getDefaultValue())||"CURRENT_TIMESTAMP".equalsIgnoreCase(p.getDefaultValue())){
                        value = new SimpleDateFormat(p.getType().substring(5)).format(new Date());
                    }
                }
            }

            result.putValue(key,value);
        }

        return result;
    }

    //
    public static ESONObject parseShownToDatabase(int dbType, ESONObject data){
        DatabaseConfig config = getDatabaseConfig(dbType);
        ESONObject result = new ESONObject();
        Map<String,ApiParam> mFieldsMapper = config.getFieldMapper();
        for (Iterator<String> it = data.keys(); it.hasNext(); ) {
            String key = it.next();

            ApiParam p = mFieldsMapper.get(key);
            if(p == null) continue;
            Object value = null;
            try { value = data.get(key); } catch (Exception e) { }

            switch (p.getType().split(":")[0]){
                case ApiParam.TYPE_STRING:
                    result.putValue(key,parseToString(value,p,true));
                    break;
                case ApiParam.TYPE_DATE:
                    result.putValue(key,parseToDate(value,p));
                    break;
                case ApiParam.TYPE_INTEGER:
                    result.putValue(key,parseToInteger(value,p,true));
                    break;
                case ApiParam.TYPE_DOUBLE:
                    result.putValue(key,parseToDouble(value,p,true));
                    break;
                case ApiParam.TYPE_BOOLEAN:
                    result.putValue(key,parseToBoolean(value,p,true));
                    break;
                case ApiParam.TYPE_LONG:
                    result.putValue(key,parseToLong(value,p,true));
                    break;
            }
        }

        return result;
    }
}
