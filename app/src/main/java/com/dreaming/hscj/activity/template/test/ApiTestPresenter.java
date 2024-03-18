package com.dreaming.hscj.activity.template.test;

import com.dreaming.hscj.App;
import com.dreaming.hscj.BuildConfig;
import com.dreaming.hscj.Constants;
import com.dreaming.hscj.core.ThreadPoolProvider;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.template.api.ApiTestResponse;
import com.dreaming.hscj.template.api.impl.Api;
import com.dreaming.hscj.template.api.impl.ApiListener;
import com.dreaming.hscj.template.api.wrapper.ApiAlgorithm;
import com.dreaming.hscj.template.api.wrapper.ApiParam;
import com.dreaming.hscj.utils.FileUtils;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.model.HttpHeaders;
import com.lzy.okgo.model.HttpParams;

import org.json.JSONObject;

import java.io.File;
import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import okhttp3.Response;
import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

public class ApiTestPresenter extends IApiTestContract.Presenter{

    private int type;
    public int getType() {
        return type;
    }
    public void setType(int type) {
        this.type = type;
    }

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

    void doRequest(ESONObject eHeaders, ESONObject eParams, ApiListener listener){
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            doRequestInternal(eHeaders,eParams,listener,null);
        });
    }

    void doRequestInternal(ESONObject eHeaders, ESONObject eParams, ApiListener listener, StringBuilder sb){
        if(sb == null) sb = new StringBuilder();
        Api api = Template.getCurrentTemplate().apiOf(getType());
        
        long start = System.currentTimeMillis();
        sb.append("Api[").append(getType()).append("]").append("发起请求：").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(start))).append("\n");

        sb.append("开始请求：").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("\n");

        Response response = null;
        try{
            if(listener!=null) App.Post(()->{ try{ listener.onPerform(); }catch (Exception e){e.printStackTrace();} });

            sb.append("\n");

            //region header
            sb.append("开始填充头部信息Header").append("\n");
            HttpHeaders headers = new HttpHeaders();
            List<ApiParam> lstHeaders = api.getRequest().getHeaders();
            for(int i=0;i<lstHeaders.size();++i){
                ApiParam header = lstHeaders.get(i);
                String key   = header.getName();
                String value = header.setter(eHeaders.getJSONValue(key,""));
                if(value == null){
                    String defaultValue = header.getDefaultValue();
                    if(ApiParam.DEFAULT_NO_EMPTY.equals(defaultValue)){
                        throw new InvalidParameterException("参数【"+header.getDescription()+"】不能为空！");
                    }
                    if(ApiParam.DEFAULT_NULL_STR.equals(defaultValue)){
                        value = "null";
                    }
                    else if(defaultValue != null){
                        value = defaultValue;
                    }
                    else{
                        value = "";
                    }
                }

                //不是令牌字段
                if(!header.getName().equals(api.getRequest().getAuthorization())){
                    headers.put(key,value);
                    sb.append("->").append(key).append(":").append(value).append("\n");
                    continue;
                }

                if(api.getRequest().getFillAuthorization()&&!value.toUpperCase().startsWith("BEARER ")){
                    value = "Bearer " + value;
                }
                headers.put(header.getName(), value);
                sb.append("->").append(key).append(":").append(value).append("\n");

            }
            //endregion

            sb.append("\n").append("\n");

            if("JSON".equalsIgnoreCase(api.getRequest().getUpload())){
                sb.append("字段组装方式：").append("JSON\n");
                sb.append("开始组装JSON").append("\n");
                ESONObject json = new ESONObject();
                for(int i=0;i<api.getRequest().getParams().size();++i) {
                    ApiParam param = api.getRequest().getParams().get(i);
                    String key = param.getName();
                    Object value = param.setter(eParams.getJSONValue(key,""));

                    value = parseValue(value==null?param.getDefaultValue():value,param.getType());
                    json.put(key,value);
                    sb.append("->").append(key).append(":").append(value).append("\n");
                }
                sb.append(json).append("\n");
                sb.append("\n");

                sb.append("开始请求服务器!").append("\n");
                sb.append("请求方式：").append(api.getRequest().getType()).append("\n");
                sb.append("请求地址：").append(api.getUrl()).append("\n");

                if(Constants.Net.isDebug){
                    sb.append("DEBUG:true\n");
                    response = ApiTestResponse.getTestResponse(getType());
                    return;
                }

                switch (api.getRequest().getType()){
                    case "GET":
                        throw new InvalidParameterException("GET不支持上传JSON");
                    case "App.Post":
                        response= OkGo.<JSONObject>post  (api.getUrl()).headers(headers).upJson(json).execute();
                        break;
                    case "DELETE":
                        response= OkGo.<JSONObject>delete(api.getUrl()).headers(headers).upJson(json).execute();
                        break;
                    case "PUT":
                        response= OkGo.<JSONObject>put   (api.getUrl()).headers(headers).upJson(json).execute();
                        break;
                }
                return;
            }
            if("PARAM".equalsIgnoreCase(api.getRequest().getUpload())){
                sb.append("字段组装方式：").append("PARAM").append("\n");
                sb.append("开始组装PARAM").append("\n");
                HttpParams params  = new HttpParams();
                for(int i=0;i<api.getRequest().getParams().size();++i){
                    ApiParam param = api.getRequest().getParams().get(i);
                    String key = param.getName();
                    Object value = param.setter(eParams.getJSONValue(key,""));
                    value = parseValue(value == null?param.getDefaultValue():value,param.getType());
                    if(value == null) {
                        String defaultValue = param.getDefaultValue();
                        if(defaultValue!=null && !defaultValue.equals("null")){
                            params.put(param.getName(),defaultValue);
                        }
                        continue;
                    }
                    if(value instanceof String) params.put(param.getName(),convert(param.getName(),(String)value,api.getRequest().getConverts(),api.getRequest().getAlgorithms()));
                    else if(boolean.class.equals(value.getClass()) || value instanceof Boolean) params.put(param.getName(),(boolean)value);
                    else if(int    .class.equals(value.getClass()) || value instanceof Integer) params.put(param.getName(),(int    )value);
                    else if(double .class.equals(value.getClass()) || value instanceof Double ) params.put(param.getName(),(double )value);
                    else if(long   .class.equals(value.getClass()) || value instanceof Long   ) params.put(param.getName(),(long   )value);
                }

                for(Map.Entry<String,List<String>> entry:params.urlParamsMap.entrySet()){
                    sb.append("->").append(entry.getKey()).append(":");
                    switch (entry.getValue().size()){
                        case 0:
                            sb.append("没有该字段！").append("\n");
                            break;
                        case 1:
                            sb.append(entry.getValue().get(0)).append("\n");
                            break;
                        default:
                            sb.append("[");
                            for(int i=0;i<entry.getValue().size();++i){
                                sb.append(i==0?"":",");
                                sb.append(entry.getValue().get(i));
                            }
                            sb.append("]").append("\n");
                            break;
                    }
                }
                sb.append("\n");

                sb.append("开始请求服务器!").append("\n");
                sb.append("请求方式：").append(api.getRequest().getType()).append("\n");
                sb.append("请求地址：").append(api.getUrl()).append("\n");

                if(Constants.Net.isDebug){
                    sb.append("DEBUG:true\n");
                    response = ApiTestResponse.getTestResponse(getType());
                    return;
                }

                switch (api.getRequest().getType()){
                    case "GET":
                        response = OkGo.get   (api.getUrl()).headers(headers).params(params).tag(type).execute();
                        break;
                    case "App.Post":
                        response = OkGo.post  (api.getUrl()).headers(headers).params(params).tag(type).execute();
                        break;
                    case "DELETE":
                        response = OkGo.delete(api.getUrl()).headers(headers).params(params).tag(type).execute();
                        break;
                    case "PUT":
                        response = OkGo.put   (api.getUrl()).headers(headers).params(params).tag(type).execute();
                        break;
                }

                return;
            }

            sb.append("不支持的字段组装方式:").append(api.getRequest().getUpload()).append("\n");
            throw new InvalidParameterException("不支持的字段组装方式");
        }
        catch (final Exception e){
            sb.append("\n"+"请求发生了异常："+e.getMessage()).append("\n");
        }
        finally {
            try { doResponse(response,listener,sb); } catch (Exception e) { sb.append("doResponse err:").append(e.getMessage()); e.printStackTrace(); }
            if(BuildConfig.DEBUG){
                FileUtils.writeToFile(new File(App.sInstance.getCacheDir(),String.format("Api%d_%d.txt",getType(),System.currentTimeMillis())),sb);
            }
        }
    }

    private Object executeExpression(ESONObject root, String expression) throws Exception {
        String left = expression.trim().substring(1);
        while(left.length()>0&&left.startsWith("[")) left = left.substring(1);
        left = left.substring(0,left.length()-1);
        String lefts[] = left.split("\\]\\[");

        ESONObject judge1 = root;
        ESONArray judge2 = new ESONArray();
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

    protected void doResponse(Response response, ApiListener listener, StringBuilder sb){
        Api api = Template.getCurrentTemplate().apiOf(getType());
        sb.append("\n\n开始解析返回数据：\n\n");
        boolean bIsSuccess = false;
        try {
            if(response == null) return;
            String strResponse = response.body().string();
            sb.append("response:").append(strResponse).append("\n");
            ESONObject result = new ESONObject(strResponse);

            sb.append("开始判断响应结果是否正确：\n");

            //region assets
            List<String> lstAssets = api.getResponse().getAsset();
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
                else{
                    int num = Integer.parseInt(right);
                    if(judge==null || !(isEq&&Integer.parseInt(judge.toString())==num || !isEq&&Integer.parseInt(judge.toString())!=num)){
                        sb.append("判断结果：表达式未满足！\n");
                        listener.onFailure(-1,"表达式未满足！",sb);
                        return;
                    }
                }
                sb.append("判断结果：表达式满足！\n\n");
            }
            //endregion

            sb.append("\n开始解析返回实体：").append(api.getResponse().getEntity()).append("\n");
            Object entity = executeExpression(result,api.getResponse().getEntity());

            sb.append("解析结果：").append(entity);

            bIsSuccess = true;
            App.Post(()->listener.onSuccess(0,entity,sb));
        } catch (Exception e) {
            sb.append("doResponse err:").append(e.getMessage());
            e.printStackTrace();
        }
        finally {
            if(!bIsSuccess){
                App.Post(()->listener.onFailure(0,"数据请求失败！",sb));
            }
        }
    }
}
