package com.dreaming.hscj.template.api.wrapper;

import org.json.JSONException;

import java.security.InvalidParameterException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

public class ApiResponse {
    private static void autoCheckInteger(Exception e,String string) throws Exception {
        if(string.isEmpty()) throw e;
        if(string.replaceAll("([0-9])|([1-9](0-9)*)","").length()!=0) throw e;
    }

    private static void autoCheckString(String tag,String string) throws Exception {
        string = string.trim();
        Exception e = new InvalidParameterException(tag+"表达式无法解析！");
        if(!string.startsWith("\"")) throw e;
        if(!string.endsWith("\"")) throw e;
        String split = string.substring(0,string.length()-1).substring(1);
        ApiParam.checkField(tag,split);
    }

    public static void autoCheckExpressionLeft(String tag,String left) throws Exception{
        Exception e = new InvalidParameterException(tag+"表达式无法解析！");
        left = left.trim();
        if(!left.startsWith("[") || !left.endsWith("]")) throw e;
        //以"]["分组
        left = left.substring(0,left.length()-1).substring(1);
        //左半部分去掉起始'['和末尾']'
        if(left.length()<2) throw e;
        left = left.substring(0,left.length()-1);

        String leftSplits[] = left.split("\\]\\[");
        if(leftSplits.length==0) throw e;
        for(int i=0,ni=leftSplits.length;i<ni;++i){
            String split = leftSplits[i].trim();
            if(split.startsWith("\"")){
                ApiParam.checkField(tag,split.substring(0,split.length()-1).substring(1));
            }
            else{
                autoCheckInteger(e,split);
            }
        }
    }

    public static void autoCheckExpressionRight(String tag,String right) throws Exception{
        Exception e = new InvalidParameterException(tag+"表达式无法解析！");
        right = right.trim();
        if(right.isEmpty()) throw e;
        if(!right.toLowerCase().equals("null")){
            if(right.startsWith("\"")){
                autoCheckString(tag,right);
            }
            else{
                autoCheckInteger(e,right);
            }
        }
    }

    private static void autoCheckExpression(String tag,String expression) throws Exception {
        Exception e = new InvalidParameterException(tag+"表达式无法解析！");
        if(!expression.startsWith("R")) throw e;
        expression = expression.substring(1);
        String[] splits = expression.contains("!=")? expression.split("!=") : expression.split("==") ;
        if(splits.length!=2) throw e;

        autoCheckExpressionLeft(tag,splits[0]);
        autoCheckExpressionLeft(tag,splits[0]);
    }

    public static ApiResponse parse(String tag, ESONObject data) throws Exception{
        ApiResponse response = new ApiResponse();

        List<String> lstAssert = new ArrayList();
        ESONArray eAssert = data.getJSONValue("assert",new ESONArray());
        if(eAssert.length()==0){
            throw new InvalidParameterException(tag+"[\"assert\"]不能为空！");
        }
        for(int i=0;i<eAssert.length();++i){
            String expression = eAssert.getArrayValue(i,"");
            autoCheckExpression(tag+"[\"assert\"]["+i+"]",expression);
            lstAssert.add(expression);
        }
        response.asset = lstAssert;

        String entity = data.getJSONValue("entity","");
        if(!entity.startsWith("R")) throw new InvalidParameterException(tag+"[\"entity\"]表达式无法解析！");
        autoCheckExpressionLeft(tag+"[\"entity\"]",entity.substring(1));
        response.entity = entity;

        List<ApiParam> lstParam = new ArrayList();
        ESONArray eFields = data.getJSONValue("fields",new ESONArray());
        if(eFields.length()==1){
            ESONArray eField = eFields.getArrayValue(0,new ESONArray());
            if(eField.length()==0){
                throw new InvalidParameterException("");
            }
            ESONArray e = eField.getArrayValue(0,new ESONArray());
            if(e.length()==0){
                lstParam.add(ApiParam.parse("",eField));
            }
            else{
                for(int i=0;i<e.length();++i){
                    lstParam.add(ApiParam.parse(tag+"[\"fields\"][0][0]["+i+"]",e.getArrayValue(i,new ESONArray())));
                }
            }
        }
        else{
            for(int i=0;i<eFields.length();++i){
                lstParam.add(ApiParam.parse(tag+"[\"fields\"]["+i+"]",eFields.getArrayValue(i,new ESONArray())));
            }
        }
        response.fields = lstParam;

        ESONArray eConverts   = data.getJSONValue("converts",new ESONArray());
        List<String> lstConverts = new ArrayList();
        for(int i=0;i<eConverts.length();++i){
            String field = eConverts.getArrayValue(i,"").trim();
            ApiParam.checkField(tag+"[\"converts\"]["+i+"]",field);
            lstConverts.add(field);
        }
        response.converts = lstConverts;

        ESONArray eAlgorithms = data.getJSONValue("algorithms",new ESONArray());
        List<List<ApiAlgorithm>> llstAlgorithms = new ArrayList();
        if(eConverts.length()!=eAlgorithms.length()){
            throw new InvalidParameterException(tag+"[\"converts\"].length != "+tag+"[\"algorithms\"].length！");
        }
        for(int i=0;i<eAlgorithms.length();++i){
            List<ApiAlgorithm> lstAlgorithm = new ArrayList();
            ESONArray eAlgorithm = eAlgorithms.getArrayValue(i,new ESONArray());
            for(int j=0;j<eAlgorithm.length();++j){
                ApiAlgorithm algorithm = ApiAlgorithm.parse(tag+"[\"algorithms\"]["+i+"]["+j+"]",eAlgorithm.getArrayValue(j,new ESONArray()));
                lstAlgorithm.add(algorithm);
            }
            llstAlgorithms.add(lstAlgorithm);
        }
        response.algorithms = llstAlgorithms;

        return response;
    }

    //状态判断语句
    private List<String> asset;
    public List<String> getAsset(){
        return asset;
    }
    public ApiResponse setAsset(List<String> asset){
        this.asset = asset;
        return this;
    }

    //实体判断语句
    private String entity;
    public String getEntity(){
        return entity;
    }
    public ApiResponse setEntity(String entity){
        this.entity = entity;
        return this;
    }

    //实体字段
    private List<ApiParam> fields;
    public List<ApiParam> getFields(){
        return fields;
    }
    public ApiResponse setFields(List<ApiParam> fields){
        this.fields = fields;
        return this;
    }

    //需要用算法转换的字段
    private List<String> converts;
    public List<String> getConverts(){
        return converts;
    }
    public ApiResponse setConverts(List<String> converts){
        this.converts = converts;
        return this;
    }

    //对应转换字段的转换算法
    private List<List<ApiAlgorithm>> algorithms;
    public List<List<ApiAlgorithm>> getAlgorithms(){
        return algorithms;
    }
    public ApiResponse setAlgorithms(List<List<ApiAlgorithm>> algorithms){
        this.algorithms = algorithms;
        return this;
    }

    private Map<String,ApiParam> mParamMapper = new HashMap<>();
    public synchronized Map<String,ApiParam> getParamMapper(){
        if(!mParamMapper.isEmpty()) return mParamMapper;
        if(getFields().isEmpty()) return mParamMapper;
        for(ApiParam param:getFields()){
            mParamMapper.put(param.getName(),param);
        }
        return mParamMapper;
    }

    private Map<String,String> mTypeMapper = new HashMap<>();
    public synchronized Map<String,String> getParamTypeMapper(){
        if(!mTypeMapper.isEmpty()) return mTypeMapper;
        if(getFields().isEmpty()) return mTypeMapper;
        for(ApiParam param:getFields()){
            mTypeMapper.put(param.getName(),param.getType());
        }
        return mTypeMapper;
    }

    private Map<String,String> mNameMapper = new HashMap<>();
    public synchronized Map<String,String> getParamNameMapper(){
        if(!mNameMapper.isEmpty()) return mNameMapper;
        if(getFields().isEmpty()) return mNameMapper;
        for(ApiParam param:getFields()){
            mNameMapper.put(param.getName(),param.getDescription());
        }
        return mNameMapper;
    }

    private Map<String,ApiParam> mFieldMapper = new HashMap<>();
    public synchronized Map<String,ApiParam> getFieldMapper(){
        if(!mFieldMapper.isEmpty()) return mFieldMapper;
        if(getFields().isEmpty()) return mFieldMapper;
        for(ApiParam param:getFields()){
            mFieldMapper.put(param.getName(),param);
        }
        return mFieldMapper;
    }

    public ESONObject parseResponse(Object response){
        ESONObject data = new ESONObject(response);
        ESONObject result = new ESONObject();
        Map<String,ApiParam> mParamMapper = getParamMapper();
        for (Iterator<String> it = data.keys(); it.hasNext(); ) {
            String key  = it.next();
            ApiParam p = mParamMapper.get(key);
            if(p == null) continue;
            String type = p.getType();
            if(type == null) continue;
            Object value = null;
            try { value = data.get(key); } catch (Exception e) { }

            String defaultValue = p.getDefaultValue();
            switch (type.trim().toUpperCase().split(":")[0]){
                case "DATE":
                    String date = "";
                    String fmt  = type.substring(5);
                    SimpleDateFormat format = new SimpleDateFormat(fmt);
                    if(value == null){}
                    else if(long.class.equals(value.getClass()) || value instanceof Long){
                        try { date = format.format(new Date((long)value)); } catch (Exception e) { value = null; }
                    }
                    else if(value instanceof String){
                        try { date = format.format(format.parse((String)value)); } catch (Exception e) { value = null; }
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
                        else if(!ApiParam.DEFAULT_NO_EMPTY.equals(defaultValue)){
                            date = defaultValue;
                        }
                    }

                    result.putValue(key,date);
                    break;
                case "STRING":
                    String str = null;
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
                    result.putValue(key,(String)str);
                    break;
                case "DOUBLE":
                case "FLOAT":
                    Double d = null;
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

                    result.putValue(key,d);
                    break;
                case "BOOL":
                case "BOOLEAN":
                    Boolean b = null;
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
                    result.putValue(key,b);
                    break;
                case "INT":
                case "INTEGER":
                    Integer i = null;
                    if(value == null){ }
                    else if(value.getClass().equals(int.class) || value instanceof Integer){
                        i = (Integer) value;
                    }
                    else {
                        i = Integer.parseInt(value.toString());
                    }
                    if(i == null && !"null".equals(defaultValue)){
                        try { i = Integer.parseInt(defaultValue); } catch (Exception e) { }
                    }
                    result.putValue(key,i);
                    break;
                case "LONG":
                    Long l = null;

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

                    result.putValue(key,l);
                    break;
            }
        }

        return result;
    }
}
