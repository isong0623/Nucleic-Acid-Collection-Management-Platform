package com.dreaming.hscj.template.api.wrapper;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

public class ApiRequest {
    public static ApiRequest parse(String tag, ESONObject data) throws Exception{
        ApiRequest request = new ApiRequest();
        String type = data.getJSONValue("type","").trim().toUpperCase();
        switch (type){
            case "POST":
            case "GET":
            case "DELETE":
            case "PUT":
                request.type = type;
                break;
            default:
                throw new InvalidParameterException(tag+"[\"type\"]不受支持！");
        }

        String upload = data.getJSONValue("uploading","").toUpperCase();
        switch (upload){
            case "PARAM":
            case "JSON":
                request.upload = upload;
                break;
            default:
                throw new InvalidParameterException(tag+"[\"upload\"]不受支持！");
        }

        if("GET".equals(type) && "JSON".equals(upload)){
            throw new InvalidParameterException(tag+"GET请求不支持上传JSON！");
        }

        List<ApiParam> lstParams = new ArrayList();
        ESONArray eParams = data.getJSONValue("params",new ESONArray());
        for(int i=0;i<eParams.length();++i){
            lstParams.add(ApiParam.parse(String.format("%s[%s][%d]",tag,"\"params\"",i),eParams.getArrayValue(i,new ESONArray())));
        }
        request.params = lstParams;

        List<ApiParam> lstHeaders = new ArrayList();
        ESONArray eHeaders = data.getJSONValue("headers",new ESONArray());
        for(int i=0;i<eHeaders.length();++i){
            lstHeaders.add(ApiParam.parse(String.format("%s[%s][%d]",tag,"\"headers\"",i),eHeaders.getArrayValue(i,new ESONArray())));
        }
        request.headers = lstHeaders;

        ESONArray eConverts   = data.getJSONValue("converts",new ESONArray());
        List<String> lstConverts = new ArrayList();
        for(int i=0;i<eConverts.length();++i){
            String field = eConverts.getArrayValue(i,"").trim();
            ApiParam.checkField(tag+"[\"converts\"]["+i+"]",field);
            lstConverts.add(field);
        }
        request.converts = lstConverts;

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
        request.algorithms = llstAlgorithms;

        request.authorization = data.getJSONValue("authorization","");

        request.fillAuthorization = data.getJSONValue("fillAuthorization",false);

        return request;
    }

    private ApiRequest(){}

    //请求类型 eg:POST
    private String type;
    public String getType(){
        return type;
    }
    public ApiRequest setType(String type){
        this.type = type;
        return this;
    }

    //上传方式 eg:JSON
    private String upload;
    public String getUpload(){
        return upload;
    }
    public ApiRequest setUpload(String upload){
        this.upload = upload;
        return this;
    }

    //请求参数
    private List<ApiParam> params;
    public List<ApiParam> getParams(){
        return params;
    }
    public ApiRequest setParams(List<ApiParam> params){
        this.params = params;
        return this;
    }

    //请求头
    private List<ApiParam> headers;
    public List<ApiParam> getHeaders(){
        return headers;
    }
    public ApiRequest setHeaders(List<ApiParam> headers){
        this.headers = headers;
        return this;
    }

    //如果headers中有Authorization，是否前缀补充Bearer
    private boolean fillAuthorization;
    public boolean getFillAuthorization(){
        return fillAuthorization;
    }
    public ApiRequest setFillAuthorization(boolean fillAuthorization){
        this.fillAuthorization = fillAuthorization;
        return this;
    }

    //Authorization字段名
    private String authorization;
    public String getAuthorization(){
        return authorization;
    }
    public ApiRequest setAuthorization(String authorization){
        this.authorization = authorization;
        return this;
    }

    //需要用算法转换的字段
    private List<String> converts;
    public List<String> getConverts(){
        return converts;
    }
    public ApiRequest setConverts(List<String> converts){
        this.converts = converts;
        return this;
    }

    //对应转换字段的转换算法
    private List<List<ApiAlgorithm>> algorithms;
    public List<List<ApiAlgorithm>> getAlgorithms(){
        return algorithms;
    }
    public ApiRequest setAlgorithms(List<List<ApiAlgorithm>> algorithms){
        this.algorithms = algorithms;
        return this;
    }

    private Map<String,String> mTypeMapper = new HashMap<>();
    public synchronized Map<String,String> getParamTypeMapper(){
        if(!mTypeMapper.isEmpty()) return mTypeMapper;
        if(getParams().isEmpty()) return mTypeMapper;
        for(ApiParam param:getParams()){
            mTypeMapper.put(param.getName(),param.getType());
        }
        return mTypeMapper;
    }
}
