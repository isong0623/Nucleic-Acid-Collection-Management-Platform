package com.dreaming.hscj.template.api.wrapper;

import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

//Api参数
public class ApiParam {
    public static final String DEFAULT_NULL_STR = "NULL";
    public static final String DEFAULT_NULL     = null;
    public static final String DEFAULT_NO_EMPTY = "NOEMPTY";

    public static final String TYPE_INTEGER = "INTEGER";
    public static final String TYPE_STRING  = "STRING";
    public static final String TYPE_DATE    = "DATE";
    public static final String TYPE_BOOLEAN = "BOOLEAN";
    public static final String TYPE_DOUBLE  = "DOUBLE";
    public static final String TYPE_LONG    = "LONG";

    public static void checkField(String tag,String field){
        if(field.length()==0) throw new InvalidParameterException(tag+"字段名必须非空！");
        if(field.contains(" ")) throw new InvalidParameterException(tag+"字段不能含有空格！");
        if(field.replaceAll("[a-zA-Z_0-9]*","").length()!=0) throw new InvalidParameterException(tag+"字段仅能包含大小写字母、下划线、数字！");
        if(field.charAt(0)>='0' && field.charAt(0)<='9') throw new InvalidParameterException(tag+"字段不能以数字开头！");
    }

    public static Map<String,String> parseTypeMapper(List<ApiParam> lstParams){
        Map<String,String> mTypeMapper = new HashMap<>();

        for(ApiParam param:lstParams){
            mTypeMapper.put(param.getName(),param.getType());
        }

        return mTypeMapper;
    }

    public static ApiParam parse(String tag, ESONArray data){
        if(data.length()<4) throw new InvalidParameterException(tag+"数组长度至少为4个！");

        ApiParam param = new ApiParam();

        String name = data.getArrayValue(0,"").trim();
        checkField(tag+"[0]",name);
        param.name = name;

        String type = data.getArrayValue(1,"").trim();
        boolean bIsString = false;
        boolean bIsDate   = false;
        boolean bIsBoolean= false;
        boolean bIsDouble = false;
        boolean bIsInteger= false;
        boolean bIsLong   = false;
        switch (type.split(":")[0].toUpperCase()){
            case "INT":
            case "INTEGER":
                type = TYPE_INTEGER;
                bIsInteger = true;
                break;
            case "STR":
            case "STRING":
                type = TYPE_STRING;
                bIsString = true;
                break;
            case "BOOL":
            case "BOOLEAN":
                type = TYPE_BOOLEAN;
                bIsBoolean = true;
                break;
            case "DATE":
                bIsDate = true;
                break;
            case "DOUBLE":
            case "FLOAT":
                type = TYPE_DOUBLE;
                bIsDouble = true;
                break;
            case "LONG":
                type = TYPE_LONG;
                bIsLong = true;
                break;
            default:
                throw new InvalidParameterException(tag+"[1]不支持的数据类型！");
        }
        param.type = type;

        param.description = data.getArrayValue(2,"").trim();

        String defaultValue = data.getArrayValue(3,"").trim();
        if(bIsString){
            switch (defaultValue.toUpperCase()){
                case "NULL":
                    param.defaultValue = DEFAULT_NULL;
                    break;
                case "NULLSTR":
                    param.defaultValue = DEFAULT_NULL_STR;
                    break;
                case "NOEMPTY":
                    param.defaultValue = DEFAULT_NO_EMPTY;
                    break;
                default:
                    param.defaultValue = defaultValue;
            }
        }
        if(bIsBoolean){
            if("0".equals(defaultValue) || "false".equalsIgnoreCase(defaultValue) || defaultValue.isEmpty()){
                param.defaultValue = "false";
            }
            else{
                param.defaultValue = "true";
            }
        }
        if(bIsDate){
            if("CURRENT_TIMESTAMP".equalsIgnoreCase(defaultValue) || "NOW".equalsIgnoreCase(defaultValue)){
                param.defaultValue = "CURRENT_TIMESTAMP";
            }
        }
        if(bIsDouble && !defaultValue.isEmpty()){
            try {
                Double.parseDouble(defaultValue);
                param.defaultValue = defaultValue;
            } catch (NumberFormatException e) {
                throw new InvalidParameterException(tag+"[3]默认值无法转换到绑定类型！");
            }
        }
        if(bIsInteger && !defaultValue.isEmpty()){
            try {
                Integer.parseInt(defaultValue);
                param.defaultValue = defaultValue;
            } catch (NumberFormatException e) {
                throw new InvalidParameterException(tag+"[3]默认值无法转换到绑定类型！");
            }
        }
        if(bIsLong && !defaultValue.isEmpty()){
            try {
                Long.parseLong(defaultValue);
                param.defaultValue = defaultValue;
            } catch (NumberFormatException e) {
                throw new InvalidParameterException(tag+"[3]默认值无法转换到绑定类型！");
            }
        }
        if(param.defaultValue == null && DEFAULT_NO_EMPTY.equalsIgnoreCase(defaultValue)){
            param.defaultValue = DEFAULT_NO_EMPTY;
        }

        if(data.length()>4){
            ESONObject formatter =  new ESONObject(data.getArrayValue(4,"").trim());

            param.getter = formatter;
            param.setter = new ESONObject();

            for(Iterator<String> it = formatter.keys();it.hasNext();){
                String key = it.next();
                if("String.format".equalsIgnoreCase(key)){
                    param.sStringFormatter = param.getter.getJSONValue(key,"");
                    String sStringFormatter = param.sStringFormatter;
                    int start = -1;
                    while(sStringFormatter.indexOf("%",start + 1)>-1){
                        start = sStringFormatter.indexOf("%",start + 1);

                        if(start + 1 >= sStringFormatter.length()) break;

                        if(start > 1 && sStringFormatter.charAt(start-1) == '%') continue;
                        if(start + 1 < sStringFormatter.length() && sStringFormatter.charAt(start+1) == '%') continue;
                        break;
                    }
                    if(start <0 || start>=sStringFormatter.length() ||
                            '%'!=sStringFormatter.charAt(start) ||
                            start>1&& '%' == sStringFormatter.charAt(start-1) ||
                            start+1<sStringFormatter.length() && '%'== sStringFormatter.charAt(start+1)){
                        throw new InvalidParameterException(tag+"[4]不支持的字符串格式化表达式！");
                    }

                    int end = 0;
                    for(int i=start,ni=param.sStringFormatter.length();i<ni;++i){
                        char ch = param.sStringFormatter.charAt(i);
                        if(ch>='a' && ch<='z' || ch>='A' && ch<='Z'){
                            end = i;
                            break;
                        }
                    }
                    String a = param.sStringFormatter.replaceAll("%%","");
                    String b = a.replaceAll("%","");
                    if(b.length() != a.length()-1 || start <0 || end ==0){
                        throw new InvalidParameterException(tag+"[4]不支持的字符串格式化表达式！");
                    }

                    switch (param.getType().split(":")[0]){
                        case TYPE_INTEGER:
                            try { String.format(param.sStringFormatter,1); } catch (Exception e) { throw new InvalidParameterException(tag+"[4]不支持的字符串格式化表达式！"); }
                            break;
                        case TYPE_BOOLEAN:
                            try { String.format(param.sStringFormatter,false); } catch (Exception e) { throw new InvalidParameterException(tag+"[4]不支持的字符串格式化表达式！"); }
                            break;
                        case TYPE_DOUBLE:
                            try { String.format(param.sStringFormatter,1.00d); } catch (Exception e) { throw new InvalidParameterException(tag+"[4]不支持的字符串格式化表达式！"); }
                            break;
                        case TYPE_LONG:
                            try { String.format(param.sStringFormatter,1L); } catch (Exception e) { throw new InvalidParameterException(tag+"[4]不支持的字符串格式化表达式！"); }
                            break;
                        case TYPE_STRING:
                        case TYPE_DATE:
                            try { String.format(param.sStringFormatter,""); } catch (Exception e) { throw new InvalidParameterException(tag+"[4]不支持的字符串格式化表达式！"); }
                            break;
                    }

                    continue;
                }
                param.setter.putValue(param.getter.getJSONValue(key,""),key);
            }

        }

        if(data.length()>5){
            List<String> lstDataGetter = new ArrayList<>();
            for(int i=5,ni=data.length();i<ni;++i){
                String value = data.getArrayValue(i,"").trim();
                if("INPUT".equals(value)){
                    lstDataGetter.add(value);
                    continue;
                }
                if(value.isEmpty()||value.replaceAll("(datablock@[a-zA-Z_]([a-zA-Z0-9_]*))|(Api\\[[0-6]\\]\\[\\\"[a-zA-Z_]([a-zA-Z0-9_]*)\\\"\\])","").length()!=0){
                    throw new InvalidParameterException(tag+"["+i+"]无法解析成datablock@本地类型或Api网络获取类型！");
                }
                lstDataGetter.add(value);
            }
            param.lstDataGetter = lstDataGetter;
        }

        return param;
    }

    private ApiParam(){}
    public ApiParam(String name,String type,String description,String defaultValue){
        this.name = name;
        this.type = type;
        this.description = description;
        this.defaultValue = defaultValue;
    }

    //参数名 eg:address
    private String name;
    public String getName(){
        return name;
    }
    public ApiParam setName(String name){
        this.name = name;
        return this;
    }

    //参数类型 eg:STRING
    private String type;
    public String getType(){
        return type;
    }
    public ApiParam setType(String type){
        this.type = type;
        return this;
    }

    //参数作用描述 eg:地址
    private String description;
    public String getDescription(){
        return description==null||"?".equals(description) || description.isEmpty() ? name : description;
    }
    public ApiParam setDescription(String description){
        this.description = description;
        return this;
    }

    //参数默认值 eg:null
    private String defaultValue;
    public String getDefaultValue(){
        return defaultValue;
    }
    public ApiParam setDefaultValue(String defaultValue){
        this.defaultValue = defaultValue;
        return this;
    }

    public boolean canEmpty(){
        return !DEFAULT_NO_EMPTY.equals(defaultValue);
    }

    String sStringFormatter;

    //当展示数据时会将原始数据进行转换
    //例如["sex","INTEGER","性别","","{\"0\":\"女\",{\"1\":\"男\"}"]
    //getter("0").equals("女") == TRUE
    private ESONObject getter = new ESONObject();
    public String getter(String value){
        if(value == null) return null;
        String result = getter.getJSONValue(value,value);
        if(result == null){
            if(sStringFormatter!=null){
                switch (getType().split(":")[0]){
                    case TYPE_INTEGER:
                        try { return String.format(sStringFormatter,Integer.valueOf(value)); } catch (Exception e) { }
                        break;
                    case TYPE_BOOLEAN:
                        try { return String.format(sStringFormatter,Boolean.valueOf(value) || !("0".equals(value) || "FALSE".equalsIgnoreCase(value))); } catch (Exception e) { }
                        break;
                    case TYPE_DOUBLE:
                        try { return String.format(sStringFormatter,Double.valueOf(value)); } catch (Exception e) { }
                        break;
                    case TYPE_LONG:
                        try { return String.format(sStringFormatter,Long.valueOf(value)); } catch (Exception e) { }
                        break;
                    case TYPE_STRING:
                    case TYPE_DATE:
                        try { return String.format(sStringFormatter,value); } catch (Exception e) { }
                        break;
                }
            }
        }
        if("null".equals(result) && !DEFAULT_NULL_STR.equals(getDefaultValue()) && !DEFAULT_NO_EMPTY.equals(getDefaultValue())){
            result = getDefaultValue();
        }
        return result == null ? value : result;
    }

    //当保存数据时会将数据进行转换
    //例如["sex","INTEGER","性别","","{\"0\":\"女\",{\"1\":\"男\"}"]
    //setter("女").equals("0") == TRUE
    private ESONObject setter = new ESONObject();
    public String setter(String value){
        if(value == null) return null;
        String result = setter.getJSONValue(value,value);
        if(result == null){
            if(sStringFormatter!=null) {
                int start = -1;
                while(sStringFormatter.indexOf("%",start + 1)>-1){
                    start = sStringFormatter.indexOf("%",start + 1);

                    if(start + 1 >= sStringFormatter.length()) break;

                    if(start > 1 && sStringFormatter.charAt(start-1) == '%') continue;
                    if(start + 1 < sStringFormatter.length() && sStringFormatter.charAt(start+1) == '%') continue;
                    break;
                }
                //sStringFormatter[start] == '%'
                int end = 0;

                for(int i=start+1,ni=sStringFormatter.length();i<ni;++i){
                    char ch = sStringFormatter.charAt(i);
                    if(ch>='a' && ch<='z' || ch>='A' && ch<='Z'){
                        end = i;
                        break;
                    }
                }

                int vEnd = value.length() - sStringFormatter.substring(end).length();
                if(sStringFormatter.length()>end && value.length()>vEnd &&
                        value.substring(0,start).equals(sStringFormatter.substring(0,start)) &&
                        value.substring(vEnd).equals(sStringFormatter.substring(end)) ){
                    return value.substring(start,vEnd);
                }
            }
        }

        if(result == null && getDefaultValue()!=null || "null".equals(result) && !DEFAULT_NULL_STR.equals(getDefaultValue()) && !DEFAULT_NO_EMPTY.equals(getDefaultValue())){
            result = getDefaultValue();
        }

        return result == null ? defaultValue : result;
    }

    //当Api上传数据为空时会根据这些字段来获取值
    //例如["sex","INTEGER","性别","","{\"0\":\"女\",{\"1\":\"男\"}","datablock@sex","Api[2][\"sex\"]"]
    //录入数据时，先从数据库检索，没有则调起Api[2]
    List<String> lstDataGetter = new ArrayList<>();

    public List<String> getDataGetter() {
        return lstDataGetter;
    }
}