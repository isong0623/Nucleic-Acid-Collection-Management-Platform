package com.dreaming.hscj.template.api;

import com.dreaming.hscj.template.api.impl.Api;
import com.dreaming.hscj.template.api.impl.ApiConfig;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

public class ApiTemplate {
    static Logger logger = Logger.getLogger(ApiTemplate.class.getName());

    public static ApiTemplate read(ESONObject data) throws Exception{
        ESONArray api = data.getJSONValue("Api", new ESONArray());
        if(api.length()!=7) throw new InvalidParameterException("ApiTemplate[\"Api\"]数组个数应为7个！");

        ApiTemplate apiTemplate = new ApiTemplate();
        for(int i=0;i<7;++i){
            apiTemplate.lstApi.add(Api.parse(api.getArrayValue(i,new ESONObject())));
            logger.info("init Api[" + i + "] success!");
        }
        apiTemplate.apiConfig = ApiConfig.parse(data.getJSONValue("ApiConfig",new ESONObject()));
        logger.info("init ApiConfig success!");

        return apiTemplate;
    }

    private ApiTemplate(){}
    private List<Api> lstApi = new ArrayList<>();
    public Api getApi(int type){
        if(type<0 || type>6) return null;
        return lstApi.get(type);
    }

    private ApiConfig apiConfig;
    public ApiConfig getApiConfig(){
        return apiConfig;
    }
}
