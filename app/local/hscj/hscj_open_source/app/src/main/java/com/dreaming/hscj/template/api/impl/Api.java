package com.dreaming.hscj.template.api.impl;

import com.dreaming.hscj.Constants;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.template.api.wrapper.ApiAlgorithm;
import com.dreaming.hscj.template.api.wrapper.ApiParam;
import com.dreaming.hscj.template.api.wrapper.ApiRequest;
import com.dreaming.hscj.template.api.wrapper.ApiResponse;

import java.net.URLEncoder;
import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.dreaming.hscj.utils.JsonUtils;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

public class Api {
    public static final int TYPE_LOGIN              = 0;
    public static final int TYPE_GET_COMMUNITY_INFO = 1;
    public static final int TYPE_NC_PEOPLE_INFO     = 2;
    public static final int TYPE_GET_PEOPLE_INFO    = 3;
    public static final int TYPE_NC_PEOPLE_SAMPLING = 4;
    public static final int TYPE_NC_DELETE          = 5;
    public static final int TYPE_NC_HISTORY_SEARCH  = 6;

    private static String TAG = Api.class.getSimpleName();

    public static Api parse(ESONObject data) throws Exception {
        Api api = new Api();

        int type = data.getJSONValue("type",-1);
        if(type<0||type>6) throw new InvalidParameterException("不支持的Api类型："+type);
        api.type = type;

        String tag = "Api["+type+"]";

        api.interval = Math.max(0,data.getJSONValue("interval",0L));

        api.url = data.getJSONValue("url","").trim();
        if(api.url.isEmpty()) throw new InvalidParameterException("url不能为空！");

        api.request  = ApiRequest .parse(tag+"[\"request\"]" ,data.getJSONValue("request" ,new ESONObject()));
        api.response = ApiResponse.parse(tag+"[\"response\"]",data.getJSONValue("response",new ESONObject()));

        return api;
    }

    //api类型
    protected int type;
    public int getType(){
        return type;
    }
    protected Api setType(int type){
        this.type = type;
        return this;
    }

    //请求最少间隔时间
    protected long interval;
    protected long getInterval(){
        return interval;
    }
    protected Api setInterval(long interval){
        this.interval = interval;
        return this;
    }

    //接口请求地址 eg:https://www.xxx.cn/api/xxxx
    protected String url;
    public String getUrl(){
        return url;
    }
    protected Api setUrl(String url){
        this.url = url;
        return this;
    }

    //接口请求配置
    protected ApiRequest request;
    public ApiRequest getRequest(){
        return request;
    }
    protected Api setRequest(ApiRequest request){
        this.request = request;
        return this;
    }

    //接口应答配置
    protected ApiResponse response;
    public ApiResponse getResponse(){
        return response;
    }
    protected Api setResponse(ApiResponse response){
        this.response = response;
        return this;
    }

    //region template

    private Object parseValue(Object value, String type){
        if(value == null) return null;
        String splits[] = type.split(":");
        switch (splits[0].trim().toUpperCase()){
            case "STRING":
                return value.toString();
            case "INTEGER":
                if(value.getClass().equals(int.class)) return value;
                return Integer.parseInt(value.toString());
            case "DOUBLE":
                if(value.getClass().equals(double.class)) return value;
                return Double.parseDouble(value.toString());
            case "LONG":
                if(value.getClass().equals(long.class)) return value;
                return Long.parseLong(value.toString());
            case "BOOLEAN":
                if(value.getClass().equals(boolean.class)) return value;
                return "TRUE".equalsIgnoreCase(value.toString())||!"0".equalsIgnoreCase(value.toString())&&!"FALSE".equalsIgnoreCase(value.toString());
            case "DATE":
                if(value instanceof String) return value;
                return new SimpleDateFormat(type.substring(5)).format(Long.parseLong(value.toString()));
        }
        return value;
    }

    private Object toValueString(Object value){
        if(value == null) return "null";
        if(value instanceof Integer) return String.valueOf((int    )value);
        if(value instanceof Boolean) return String.valueOf((boolean)value);
        if(value instanceof Double ) return String.valueOf((double )value);
        if(value instanceof Long   ) return String.valueOf((long   )value);
        return value.toString();
    }

    //加解密签名等变幻数据
    private String convert(String field,String data,List<String> lstConverts,List<List<ApiAlgorithm>> llstAlgorithms) throws Exception {
        for(int i=0;i<lstConverts.size();++i){
            if(field.equals(lstConverts.get(i))){
                List<ApiAlgorithm> lstAlgorithms = llstAlgorithms.get(i);
                for(int j=0;j<lstAlgorithms.size();++j){
                    data = lstAlgorithms.get(j).convert(data);
                }
            }
        }
        return data;
    }

    private OkHttpClient client = new OkHttpClient();
    private AtomicLong atmLastRequestTimestamp = new AtomicLong(0);
    public void doRequest(List<Object> requestParams, ApiListener listener, StringBuilder sb){
        if(sb == null) sb = new StringBuilder();
        long start = System.currentTimeMillis();
        sb.append("Api[").append(getType()).append("]").append("发起请求：").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(start))).append("\n");
        while(true){
            long now = System.currentTimeMillis();
            long last = atmLastRequestTimestamp.get();
            if(now-last>getInterval()&&atmLastRequestTimestamp.compareAndSet(last,now)) break;
            try { Thread.sleep(100); } catch (Exception e) { }
        }

        sb.append("开始请求：").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("\n");

        Response response = null;
        try{
            sb.append("\n");

            //region header
            sb.append("开始填充头部信息Header").append("\n");
            Map<String,String> headers = new HashMap<>();
            List<ApiParam> lstHeaders = getRequest().getHeaders();
            Headers.Builder builder = new Headers.Builder();
            for(int i=0,z=4;i<lstHeaders.size();++i){
                ApiParam header = lstHeaders.get(i);
                String key   = header.getName();
                String value = header.getDefaultValue();

                //不是令牌字段
                if(!header.getName().equals(getRequest().getAuthorization())){
                    headers.put(key,value);
                    sb.append("->").append(key).append(":").append(value).append("\n");
                    continue;
                }

                value = Constants.User.getToken();
                if(value != null && value.trim().length() != 0 || Constants.User.isLogin()){
                    if(getRequest().getFillAuthorization()&&!value.toUpperCase().startsWith("BEARER ")){
                        value = "Bearer " + value;
                    }
                    headers.put(header.getName(), value);
                    sb.append("->").append(key).append(":").append(value).append("\n");
                    continue;
                }

                //自动登录
                ApiConfig.Permission permission = Template.getCurrentTemplate().getApiConfig().getPermission();
                if(permission.getBAllowRememberPassword() &&
                        Constants.User.isRememberPassword() &&
                        permission.getBAllowAutoRefreshToken() &&
                        Constants.User.isAutoLogin() &&
                        !Constants.User.getAccount().trim().isEmpty() &&
                        !Constants.User.getPassword().trim().isEmpty()
                ){
                    AtomicBoolean isLoginSuccess = new AtomicBoolean(false);
                    List<Object> lst = new ArrayList();
                    lst.add(Constants.User.getAccount());
                    lst.add(Constants.User.getPassword());
                    Api api = Template.getCurrentTemplate().apiOf(0);
                    for(int j=2;j<api.request.getParams().size();++j){
                        lst.add(api.request.getParams().get(j).getDefaultValue());
                    }
                    for(int j=0;j<3 && !isLoginSuccess.get();++j){
                        sb.append("\n");
                        sb.append("第").append(j).append("次尝试重新登录！\n");
                        Template.getCurrentTemplate().apiOf(0).doRequest(lst, new ApiListener() {
                            @Override
                            public void onSuccess(int code, Object msg, StringBuilder sb) {
                                isLoginSuccess.set(true);
                            }
                        },sb);
                    }

                    if(!isLoginSuccess.get()){
                        sb.append("\n");
                        sb.append("请求中断！\n");
                        sb.append("需要重新登录，请检查密码是否正确以及网络连接状况！\n");
                        throw new Exception("登录失败！");
                    }

                    sb.append("\n");
                    sb.append("登录成功！");
                    if(--z>0){
                        --i;
                        continue;
                    }

                    sb.append("\n");
                    sb.append("请求中断！\n");
                    sb.append("需要重新登录！\n");
                    sb.append("1、服务端多次返回正确判定数据，但接收数据为空！\n");
                    sb.append("2、本地数据存储可能出现问题！\n");
                    throw new Exception("登录异常！");
                }

                sb.append("\n");
                sb.append("请求中断！\n");
                sb.append("需要重新登录，未记住密码或Api禁止自动登录！\n");
                throw new Exception("需要重新登录！");
            }
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.add(entry.getKey(),entry.getValue());
            }
            //endregion

            sb.append("\n").append("\n");

            if("JSON".equalsIgnoreCase(getRequest().getUpload())){
                sb.append("字段组装方式：").append("JSON\n");
                sb.append("开始组装JSON").append("\n");
                ESONObject json = new ESONObject();
                for(int i=0;i<getRequest().getParams().size();++i) {
                    ApiParam param = getRequest().getParams().get(i);
                    Object value = requestParams.get(i);
                    String key = param.getName();
                    value = parseValue(value==null?param.getDefaultValue():value,param.getType());
                    json.put(key,value);
                    sb.append("->").append(key).append(":").append(value).append("\n");
                }
                sb.append(json).append("\n");
                sb.append("\n");

                sb.append("开始请求服务器!").append("\n");
                sb.append("请求方式：").append(getRequest().getType()).append("\n");
                sb.append("请求地址：").append(getUrl()).append("\n");

                Request request = null;


                switch (getRequest().getType()){
                    case "GET":
                        throw new InvalidParameterException("不支持的请求类型！");
                    case "POST":
                        request = new Request.Builder().url(getUrl()).headers(builder.build()).post(RequestBody.create(MediaType.parse("application/json"),json.toString())).tag(type).build();
                        break;
                    case "DELETE":
                        request = new Request.Builder().url(getUrl()).headers(builder.build()).delete(RequestBody.create(MediaType.parse("application/json"),json.toString())).tag(type).build();
                        break;
                    case "PUT":
                        request = new Request.Builder().url(getUrl()).headers(builder.build()).put(RequestBody.create(MediaType.parse("application/json"),json.toString())).tag(type).build();
                        break;
                }
                response = client.newCall(request).execute();
                return;
            }
            if("PARAM".equalsIgnoreCase(getRequest().getUpload())){
                sb.append("字段组装方式：").append("PARAM").append("\n");
                sb.append("开始组装PARAM").append("\n");
                Map<String,Object> params  = new HashMap<>();
                for(int i=0;i<getRequest().getParams().size();++i){
                    ApiParam param = getRequest().getParams().get(i);
                    Object value = parseValue(requestParams.get(i),param.getType());
                    if(value == null) {
                        String defaultValue = param.getDefaultValue();
                        if(defaultValue!=null && !defaultValue.equals("null")){
                            params.put(param.getName(),defaultValue);
                        }
                        continue;
                    }
                    if(value instanceof String) params.put(param.getName(),convert(param.getName(),(String)value,getRequest().getConverts(),getRequest().getAlgorithms()));
                    else if(boolean.class.equals(value.getClass()) || value instanceof Boolean) params.put(param.getName(),(boolean)value);
                    else if(int    .class.equals(value.getClass()) || value instanceof Integer) params.put(param.getName(),(int    )value);
                    else if(double .class.equals(value.getClass()) || value instanceof Double ) params.put(param.getName(),(double )value);
                    else if(long   .class.equals(value.getClass()) || value instanceof Long   ) params.put(param.getName(),(long   )value);
                }

                for(Map.Entry<String, Object> entry:params.entrySet()){
                    sb.append("->").append(entry.getKey()).append(":").append(entry.getValue()).append("\n");
                }
                sb.append("\n");

                sb.append("开始请求服务器!").append("\n");
                sb.append("请求方式：").append(getRequest().getType()).append("\n");
                sb.append("请求地址：").append(getUrl()).append("\n");

                Request request = null;
                switch (getRequest().getType()){
                    case "GET":
                        request = new Request.Builder().url(parseParamToUrl(getUrl(),params)).headers(builder.build()).get().tag(type).build();
                        break;
                    case "POST":
                        request = new Request.Builder().url(parseParamToUrl(getUrl(),params)).headers(builder.build()).post(RequestBody.create(MediaType.parse("application/json"),"")).tag(type).build();
                        break;
                    case "DELETE":
                        request = new Request.Builder().url(parseParamToUrl(getUrl(),params)).headers(builder.build()).delete().tag(type).build();
                        break;
                    case "PUT":
                        request = new Request.Builder().url(parseParamToUrl(getUrl(),params)).headers(builder.build()).put(RequestBody.create(MediaType.parse("application/json"),"")).tag(type).build();
                        break;
                }
                response = client.newCall(request).execute();
                return;
            }

            sb.append("不支持的字段组装方式:").append(getRequest().getUpload()).append("\n");
            throw new InvalidParameterException("不支持的字段组装方式");
        }
        catch (final Exception e){
            sb.append("\n"+"请求发生了异常："+e.getMessage()).append("\n");
        }
        finally {
            try { doResponse(response,listener,sb); } catch (Exception e) { sb.append("doResponse err:").append(e.getMessage()); e.printStackTrace(); }
        }
    }

    private Object executeExpression(ESONObject root, String expression) throws Exception {
        String left = expression.trim().substring(1);
        while(left.length()>0&&left.startsWith("[")) left = left.substring(1);
        left = left.substring(0,left.length()-1);
        String lefts[] = left.split("\\]\\[");

        ESONObject judge1 = root;
        ESONArray  judge2 = new ESONArray();
        Object     judge3 = new Object();
        for(int j=0;j<lefts.length;++j){
            String field = lefts[j];
            if(field.startsWith("\"")){
                field = field.substring(0,field.length()-1).substring(1);
                if(judge1.length()==0 || !judge1.has(field)) throw new Exception("表达式未满足！");
                judge3 = judge1.get(field);
                judge2 = judge1.getJSONValue(field,new ESONArray());
                judge1 = judge1.getJSONValue(field,new ESONObject());
            }
            else{
                int index = Integer.valueOf(field);
                if(judge2.length()>=index) throw new Exception("表达式未满足！");

                judge3 = judge2.get(index);
                judge1 = judge2.getArrayValue(index,new ESONObject());
                judge2 = judge2.getArrayValue(index,new ESONArray());
            }
        }

        return judge3;
    }

    protected void doResponse(Response body, ApiListener listener, StringBuilder sb){
        sb.append("\n\n开始解析返回数据：\n\n");
        boolean bIsSuccess = false;
        try {
            String strResponse = body.body().string();
            sb.append("response:").append(strResponse).append("\n");
            ESONObject result = new ESONObject(strResponse);

            sb.append("开始判断响应结果是否正确：\n");

            //region assets
            List<String> lstAssets = getResponse().getAsset();
            for(int i=0;i<lstAssets.size();++i){
                sb.append("解析判断式：").append(lstAssets.get(i)).append("\n");
                String asset = lstAssets.get(i).trim().substring(1);

                String splits[] = asset.split("(==)|(!=)");

                Object judge = executeExpression(result,splits[0].trim());

                sb.append("左解析结果为：").append(judge).append("\n");

                String right = splits[1].trim();
                sb.append("右解析结果为：").append(right).append("\n");
                boolean isEq = asset.contains("==");
                sb.append("判断条件：").append(isEq?"==":"!=").append("\n");
                if(right.equalsIgnoreCase("null")){
                    if(!(isEq&&judge==null || !isEq && judge!=null)){
                        sb.append("判断结果：表达式未满足！\n");
                        listener.onFailure(-1,"表达式未满足！",sb);
                        return;
                    }
                }
                else if(right.startsWith("\"")){
                    right = right.substring(right.length()-1).substring(1);
                    if(judge==null || !(isEq&&right.equals(judge.toString()) || !isEq && !right.equals(judge))){
                        sb.append("判断结果：表达式未满足！\n");
                        listener.onFailure(-1,"表达式未满足！",sb);
                        return;
                    }
                }
                else if("true".equalsIgnoreCase(right) || "false".equalsIgnoreCase(right)){
                    Boolean b = null;
                    try { b = Boolean.valueOf(String.valueOf(judge)); } catch (Exception e) { }

                    if(b == null || judge == null || b!="true".equalsIgnoreCase(right) ){
                        sb.append("判断结果：表达式未满足！\n");
                        listener.onFailure(-1,"表达式未满足！",sb);
                        return;
                    }
                }
                else{
                    Integer num = null;
                    try { num = Integer.parseInt(right); } catch (Exception e) { }
                    if(num == null || judge==null || !(isEq&&Integer.parseInt(judge.toString())==num || !isEq&&Integer.parseInt(judge.toString())!=num)){
                        sb.append("判断结果：表达式未满足！\n");
                        listener.onFailure(-1,"表达式未满足！",sb);
                        return;
                    }
                }
                sb.append("判断结果：表达式满足！\n\n");
            }
            //endregion

            sb.append("\n开始解析返回实体：").append(getResponse().getEntity()).append("\n");
            Object entity = executeExpression(result,getResponse().getEntity());

            sb.append("解析结果：").append(entity);

            if(entity instanceof JSONObject){
                ESONObject objResult = getResponse().parseResponse(entity);
            }
            else if(entity instanceof JSONArray){
                List<ESONObject> lstResponse = JsonUtils.parseToList(new ESONArray(entity));
                List<ESONObject> lstResult   = new ArrayList<>();

                for(ESONObject obj:lstResponse){
                    lstResult.add(getResponse().parseResponse(obj));
                }
            }
            else{
                Object oResult = entity;
            }

            bIsSuccess = true;

            //onSuccess
        } catch (Exception e) {
            sb.append("doResponse err:").append(e.getMessage());
            e.printStackTrace();
        }
        finally {
            if(!bIsSuccess){
                //onFailure
            }
        }
    }
    //endregion

    public static String parseParamToUrl(String url, Map<String, Object> params) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(url);
            if (url.indexOf('&') > 0 || url.indexOf('?') > 0) sb.append("&");
            else sb.append("?");
            for (Map.Entry<String, Object> urlParams : params.entrySet()) {
                if(urlParams.getValue() == null) continue;
                String urlValue = URLEncoder.encode(String.valueOf(urlParams.getValue()), "UTF-8");
                sb.append(urlParams.getKey()).append("=").append(urlValue).append("&");
            }
            sb.deleteCharAt(sb.length() - 1);
            return sb.toString();
        } catch (Exception e) {
        }
        return url;
    }
}
