package com.dreaming.hscj.template.api;

import android.util.Log;

import com.dreaming.hscj.template.api.impl.Api;
import com.dreaming.hscj.template.api.impl.ApiConfig;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

public class ApiTemplate {

    private static final String TAG = ApiTemplate.class.getSimpleName();

    public static ApiTemplate read(ESONObject data) throws Exception{
        ESONArray api = data.getJSONValue("Api", new ESONArray());
        if(api.length()!=7) throw new InvalidParameterException("ApiTemplate[\"Api\"]数组个数应为7个！");

        ApiTemplate apiTemplate = new ApiTemplate();
        for(int i=0;i<7;++i){
            apiTemplate.lstApi.add(Api.parse(api.getArrayValue(i,new ESONObject())));
            Log.e(TAG,"init Api[" + i + "] success!");
        }
        apiTemplate.apiConfig = ApiConfig.parse(data.getJSONValue("ApiConfig",new ESONObject()));

        List<String> id = apiTemplate.apiConfig.getId().getId();
        for(int i=0,ni=id.size();i<ni;++i){
            String s = id.get(i);
            Log.e(TAG,"ApiConfig[\"locate\"][\"id\"]["+i+"]="+s);
            if(apiTemplate.lstApi.get(3).getResponse().getParamMapper().containsKey(s)) continue;
            throw new InvalidParameterException("ApiConfig[\"locate\"][\"id\"]["+i+"]只能从Api[3]Response中获取！");
        }

        Log.e(TAG,"init ApiConfig success!");

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
