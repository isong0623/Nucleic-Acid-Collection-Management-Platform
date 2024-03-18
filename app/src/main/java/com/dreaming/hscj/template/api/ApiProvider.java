package com.dreaming.hscj.template.api;

import android.util.Log;

import com.dreaming.hscj.App;
import com.dreaming.hscj.Constants;
import com.dreaming.hscj.core.ThreadPoolProvider;
import com.dreaming.hscj.dialog.loading.LoadingDialog;
import com.dreaming.hscj.template.DataParser;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.template.api.impl.Api;
import com.dreaming.hscj.template.api.impl.ApiConfig;
import com.dreaming.hscj.template.api.impl.ApiListener;
import com.dreaming.hscj.template.api.wrapper.ApiParam;
import com.dreaming.hscj.template.api.wrapper.ApiRequest;
import com.dreaming.hscj.template.database.impl.UserOverallDatabase;
import com.dreaming.hscj.template.database.wrapper.DatabaseConfig;
import com.dreaming.hscj.utils.CheckUtils;
import com.dreaming.hscj.utils.JsonUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

/**
 * 网络模板接口请求
 */
public class ApiProvider {
    private static final String TAG = ApiProvider.class.getSimpleName();

    //获取需要保存的字段仅限Api[0]和Api[1]
    private static Set<String> getSavingKeys(int type){
        Set<String> sSavingKeys = new HashSet<>();
        String saving = "Api["+type+"]";
        for(int i=0;i<7;++i){
            if(i==type) continue;
            Api api = Template.getCurrentTemplate().apiOf(i);
            ApiRequest req = api.getRequest();
            List<ApiParam> lst = req.getParams();
            for(int j=0,nj=lst.size();j<nj;++j){
                ApiParam p = lst.get(j);
                List<String> d = p.getDataGetter();
                if(d==null || d.isEmpty()) continue;
                for(int k=0,nk=d.size();k<nk;++k){
                    String f = d.get(k);
                    if(f==null || f.isEmpty()) continue;
                    if(f.startsWith(saving)){
                        sSavingKeys.add(p.getName());
                        break;
                    }
                }
            }
        }

        return sSavingKeys;
    }

    //region 登录
    public interface ILoginListener {
        void onLoginSuccess();
        void onLoginFailure();
    }
    public static void requestLogin(String userName, String userPassword, ILoginListener listener){
        Api loginApi = Template.getCurrentTemplate().apiOf(0);
        List<Object> reqParams = new ArrayList<>();
        ApiRequest request = loginApi.getRequest();

        List<ApiParam> lstParams = request.getParams();
        reqParams.add(userName);
        reqParams.add(userPassword);

        for(int i=2,ni=lstParams.size();i<ni;++i){
            reqParams.add(lstParams.get(i).getDefaultValue());
        }

        if(Template.getCurrentTemplate().getDatabaseSetting().getUnifySocialCreditCodes().equals("000000000000000000")){
            reqParams.remove(reqParams.size()-1);
            reqParams.add(lstParams.get(3).getDefaultValue()+";"+System.currentTimeMillis());
        }

        loginApi.doRequest(reqParams, new ApiListener() {
            @Override
            public void onSuccess(int code, Object msg, StringBuilder sbLog)  {
                ThreadPoolProvider.getFixedThreadPool().execute(()->{
                    try {
                        ESONObject data = new ESONObject(msg);
                        if(Constants.Net.isDebug){
                            String sExpired = Template.getCurrentTemplate().getApiConfig().getAuthorization().getExpired();
                            String sType     = Template.getCurrentTemplate().apiOf(0).getResponse().getParamTypeMapper().get(sExpired);
                            switch (sType.split(":")[0]){
                                case "LONG":
                                    data.putValue(sExpired,System.currentTimeMillis()+24*3600*1000);
                                    break;
                                case "DATE":
                                    data.putValue(sExpired,new SimpleDateFormat(sType.substring(5)).format(new Date(System.currentTimeMillis()+24*3600*1000)));
                                    break;
                            }
                        }

                        ApiConfig.Locate.Authorization authorization = Template.getCurrentTemplate().getApiConfig().getAuthorization();
                        Map<String,String> communityInfo = Template.getCurrentTemplate().getApiConfig().getCommunityInfo();
                        final ESONObject eCI = new ESONObject();
                        final ESONObject eSaving = new ESONObject();
                        Set<String> sSavingKeys = getSavingKeys(0);

                        boolean bIsSetToken = false;
                        for (Iterator<String> it = data.keys(); it.hasNext(); ) {
                            String key  = it.next();
                            String type = loginApi.getResponse().getParamTypeMapper().get(key);
                            if(type == null) continue;

                            if(key.equals(authorization.getToken())){
                                String token = data.getJSONValue(key,"");
                                if(token.isEmpty()){
                                    listener.onLoginFailure();
                                    return;
                                }
                                bIsSetToken = true;
                                Constants.User.setToken(token);
                            }
                            else if(key.equals(authorization.getExpired())){
                                if("LONG".equals(type)){
                                    long value = data.getJSONValue(key,0L);
                                    if(value == 0L) continue;
                                    Constants.User.setExpired(value);
                                }
                                else if(type.startsWith("DATE:")){
                                    String value = data.getJSONValue(key,"").trim();
                                    if(value.isEmpty()) continue;

                                    String strDateFormat = type.substring(5);
                                    SimpleDateFormat sdf = new SimpleDateFormat(strDateFormat);
                                    try { Constants.User.setExpired(sdf.parse(value).getTime()); } catch (Exception e) { }
                                }
                                else{
                                    String value = data.getJSONValue(key,"").trim();
                                    if(value.isEmpty()) continue;
                                    String strDateFormat = "yyyy-MM-DD HH:mm:ss";
                                    SimpleDateFormat sdf = new SimpleDateFormat(strDateFormat);
                                    try { Constants.User.setExpired(sdf.parse(value).getTime()); } catch (Exception e) { }
                                }
                            }
                            else{
                                if(sSavingKeys.contains(key)){
                                    eSaving.putValue(key,data.getJSONValue(key,"").trim());
                                }

                                String ciMapper = communityInfo.get(key);
                                if(ciMapper == null) continue;
                                if(!ciMapper.startsWith("Api[0]")) continue;
                                String value = data.getJSONValue(key,"").trim();
                                if(value.isEmpty()) continue;
                                eCI.putValue(key,value);
                            }
                        }
                        if(!bIsSetToken){
                            App.Post(()->listener.onLoginFailure());
                            return;
                        }

                        Constants.DBConfig.savingApi(0, eSaving.toString());

                        Constants.Community.setInfo(eCI);

                        App.Post(()->listener.onLoginSuccess());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            @Override
            public void onFailure(int code, Object msg, StringBuilder sbLog) {
                App.Post(()->listener.onLoginFailure());
            }
        });
    }
    //endregion

    //region 获取社区信息
    public interface IUpdateCommunityInfoListener{
        void onSuccess();
        void onFailure();
    }
    public static void requestCommunityInfo(IUpdateCommunityInfoListener listener){
        Api apiRequestCommunityInfo = Template.getCurrentTemplate().apiOf(1);
        ApiRequest request = apiRequestCommunityInfo.getRequest();
        List<ApiParam> lstParams = request.getParams();
        ESONObject eSavingApi0 = Constants.DBConfig.getApiSaving(0);

        List<Object> lstReq = new ArrayList<>();
        for(int i=0,ni=lstParams.size();i<ni;++i){
            ApiParam p = lstParams.get(i);
            if(eSavingApi0.has(p.getName())) {
                lstReq.add(eSavingApi0.getJSONValue(p.getName(),""));
            }
            else{
                lstReq.add(p.getDefaultValue());
            }
        }

        apiRequestCommunityInfo.doRequest(lstReq, new ApiListener() {
            @Override
            public void onSuccess(int code, Object msg, StringBuilder sbLog) {
                ThreadPoolProvider.getFixedThreadPool().execute(()->{
                    ESONObject data = new ESONObject(msg);

                    Set<String> sSavingKeys = getSavingKeys(1);
                    ESONObject eSaving = new ESONObject();

                    Map<String,String> communityInfo = Template.getCurrentTemplate().getApiConfig().getCommunityInfo();
                    ESONObject eCI = new ESONObject();

                    for (Iterator<String> it = data.keys(); it.hasNext(); ) {
                        String key = it.next();
                        String type = request.getParamTypeMapper().get(key);
                        if(type == null) continue;
                        if(sSavingKeys.contains(key)){
                            eSaving.putValue(key,data.getJSONValue(key,"").trim());
                        }

                        String ciMapper = communityInfo.get(key);
                        if(ciMapper == null) continue;
                        if(!ciMapper.startsWith("Api[1]")) continue;
                        String value = data.getJSONValue(key,"").trim();
                        if(value.isEmpty()) continue;
                        eCI.putValue(key,value);
                    }

                    Constants.DBConfig.savingApi(1, eSaving.toString());

                    Constants.Community.setInfo(eCI);

                    App.Post(()->listener.onSuccess());
                });
            }

            @Override
            public void onFailure(int code, Object msg, StringBuilder sbLog) {
                App.Post(()->listener.onFailure());
            }
        });
    }
    //endregion

    private static List<ParamSetter> getDefaultSetter(){
        List<ParamSetter> setters = new ArrayList<>();
        setters.add(new ParamSetter(Api.TYPE_LOGIN             ,Constants.DBConfig.getApiSaving(0)));
        setters.add(new ParamSetter(Api.TYPE_GET_COMMUNITY_INFO,Constants.DBConfig.getApiSaving(1)));
        return setters;
    }

    private static Integer getApiType(String field){
        int idx1 = field.indexOf("[");
        if(idx1<0) return null;
        idx1 = idx1 +1;
        int idx2 = field.indexOf("]",idx1);
        if(idx2<0) return null;
        Integer type = null;
        try { type = Integer.valueOf(field.substring(idx1,idx2)); } catch (Exception e) { }
        return type;
    }
    private static String getApiKey(String field){
        int idx1 = field.indexOf("\"");
        int idx2 = field.lastIndexOf("\"");
        if(idx1<0 || idx2<0 || idx1 == idx2) return null;

        return field.substring(idx1+1,idx2);
    }
    private static void autoCallApi(List<ParamGetter> allGetters, List<ParamSetter> apiSetters, Api target, StringBuilder sb, List<Integer> reqApiList){
        Log.e(TAG,"call autoCallApi");
        for(ParamGetter getter:allGetters){
            Log.e(TAG,"autoCallApi->getter"+getter + " " + getter.type +" "+getter.subType+" "+getter.data);
        }

        for(ParamSetter getter:apiSetters){
            Log.e(TAG,"autoCallApi->setter"+getter + " " + getter.type +getter.data1 + " "+ getter.data2 + " "+ getter.data3);
        }

        Log.e(TAG,"autoCallApi->target->"+target.getType());

        for(Integer type:reqApiList){
            Log.e(TAG,"autoCallApi->reqList->"+type);
        }

        ApiRequest request = target.getRequest();
        List<ApiParam> lstParams = request.getParams();

        UserOverallDatabase db = Template.getCurrentTemplate().getUserOverallDatabase();
        DatabaseConfig config = db.getConfig();

        //response -> db
        Map<Integer,Map<String,String>> mAllSetterMapper = config.getSetMapper();
        if(mAllSetterMapper == null) mAllSetterMapper = new HashMap<>();



        //查找缺失字段
        List<ApiParam> lstNeeds = new ArrayList<>();
        for(int i=0,ni=lstParams.size();i<ni;++i) {
            ApiParam p = lstParams.get(i);
            List<String> dataGetters = p.getDataGetter();

            Log.e(TAG,"autoCallApi->finding key:"+p.getName());

            boolean bIsFind = false;
            for(String dataGetter:dataGetters){
                if(dataGetter == null) continue;
                Log.e(TAG,"autoCallApi->dataGetter:"+dataGetter);
                if("Api".startsWith(dataGetter)){
                    Integer type = getApiType(dataGetter);
                    if(type == null) continue;

                    String key = getApiKey(dataGetter);
                    if(key == null) continue;

                    ParamSetter setter = ParamSetter.getT(apiSetters,type);
                    if(setter == null) continue;

                    if(setter.isMap()){
                        if(setter.containsV(key)){
                            Log.e(TAG,"autoCallApi->find value in map");
                            bIsFind = true;
                            break;
                        }
                        continue;
                    }

                    if(setter.isArray()){
                        Map<String,String> mResponseSetterMapper = mAllSetterMapper.get(type);
                        if(mResponseSetterMapper == null) mResponseSetterMapper = new HashMap<>();

                        //response -> input 定位
                        Map<String,ESONObject> mGlobal = new HashMap<>();
                        Map<String,Integer> mGlobalCounter  = new HashMap<>();
                        String maxGlobalIdCardNo = null;
                        int maxGlobalCount = -1;
                        for(ESONObject item:setter.data2){
                            Map<String,ESONObject> mLocal = new HashMap<>();
                            Map<String,Integer> mCounter  = new HashMap<>();
                            int maxCount = -1;
                            String maxIdCardNo = null;

                            Log.e(TAG,"autoCallApi->finding in "+item);

                            for (Map.Entry<String, String> entry : mResponseSetterMapper.entrySet()) {
                                String dbKey = entry.getValue();
                                ApiParam p1 = config.getFieldMapper().get(dbKey);
                                if(p1 == null) continue;
                                Object value = null;
                                try { value = item.get(entry.getKey()); } catch (Exception e) { }


                                List<String> lstK = new ArrayList<>();
                                lstK.add(entry.getKey());

                                List<Object> lstV = new ArrayList<>();
                                lstV.add(value);
                                int queryCount = -1;
                                for(int ii=0;ii<5 && queryCount < 0;++ii){
                                    queryCount = db.countSync(lstK,lstV);
                                }
                                if(queryCount != 1) continue;
                                ESONArray array = null;
                                for(int ii=0;ii<5 || array==null || array.length()!=1;++ii){
                                    array = db.query(lstK,lstV);
                                }
                                if(array == null || array.length() != 1) continue;
                                ESONObject result = array.getArrayValue(0,new ESONObject());

                                String idCardNo = result.getJSONValue(db.getIdFieldName(),"");
                                if(idCardNo.trim().isEmpty()) continue;

                                mLocal.put(idCardNo, result);
                                Integer count = mCounter.get(idCardNo);
                                if(count == null) count = 0;
                                count = count +1;
                                if(maxCount<count){
                                    maxCount = count;
                                    maxIdCardNo = idCardNo;

                                    Log.e(TAG,"autoCallApi->sub searching result "+maxIdCardNo + " " + result);
                                }
                                mCounter.put(idCardNo, count);
                            }

                            Log.e(TAG,"autoCallApi->sub search end");

                            //response -> db 定位
                            if(maxIdCardNo!=null){
                                ESONObject e = mLocal.get(maxIdCardNo);
                                ParamGetter localGetter = ParamGetter.fromDb(DatabaseConfig.TYPE_USER_OVERALL,e);
                                ParamGetter inputGetter = ParamGetter.getInputT(allGetters);

                                Integer count = mGlobalCounter.get(maxIdCardNo);
                                if(count == null) count = 0;
                                count = count +1;
                                if(maxGlobalCount<count){
                                    maxGlobalCount = count;
                                    maxGlobalIdCardNo = maxIdCardNo;
                                    Log.e(TAG,"autoCallApi->global searching result "+maxGlobalIdCardNo + " " + item);
                                }
                                mGlobalCounter.put(maxIdCardNo, count);
                                mGlobal.put(maxGlobalIdCardNo, item);

                                //判断身份号是否一致
                                if(localGetter.containsV(db.getIdFieldName()) && inputGetter.containsV(db.getIdFieldName())){
                                    Object localValue = localGetter.getValue(db.getIdFieldName());
                                    Object inputValue = inputGetter.getValue(db.getIdFieldName());
                                    if(localValue != null && inputValue!=null){
                                        String localIdCardNo = String.valueOf(localValue);
                                        String inputIdCardNo = String.valueOf(inputValue);
                                        if(localIdCardNo.equalsIgnoreCase(inputIdCardNo)){
                                            ParamSetter itemSetter = new ParamSetter(setter.type,item);
                                            if(itemSetter.containsV(key)){
                                                bIsFind = true;
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if(bIsFind) break;

                        Log.e(TAG,"autoCallApi->global search end "+maxGlobalIdCardNo);

                        if(maxGlobalIdCardNo != null){
                            ESONObject e = mGlobal.get(maxGlobalIdCardNo);
                            ParamSetter itemSetter = new ParamSetter(setter.type,e);
                            if(itemSetter.containsV(key)){
                                bIsFind = true;
                                Log.e(TAG,"autoCallApi->find value in array");
                                break;
                            }
                        }
                    }

                }
                if("EXCEL".equalsIgnoreCase(dataGetter)){
                    ParamGetter excelGetter = ParamGetter.getExcelT(allGetters);
                    if(excelGetter == null) continue;
                    if(!excelGetter.containsV(p.getName())) continue;

                    Log.e(TAG,"autoCallApi->find value in excel");
                    bIsFind = true;
                    break;
                }
                if("INPUT".equalsIgnoreCase(dataGetter)){
                    ParamGetter inputGetter = ParamGetter.getInputT(allGetters);
                    if(inputGetter == null) continue;
                    if(!inputGetter.containsV(p.getName())) continue;

                    Log.e(TAG,"autoCallApi->find value in input");
                    bIsFind = true;
                    break;
                }

                if(!dataGetter.startsWith("datablock@")) continue;
                String key = dataGetter.substring("datablock@".length());
                ParamGetter dbGetter = ParamGetter.getDbT(allGetters,DatabaseConfig.TYPE_USER_OVERALL);

                if(dbGetter == null) continue;
                if(!dbGetter.containsV(key)) continue;
                bIsFind = true;
                Log.e(TAG,"autoCallApi->find value in database");
            }

            if(!bIsFind){
                ParamGetter dbGetter = ParamGetter.getDbT(allGetters,DatabaseConfig.TYPE_USER_OVERALL);
                if(dbGetter != null && dbGetter.containsV(p.getName())){
                    bIsFind = true;
                    Log.e(TAG,"autoCallApi->find value in database");
                }
            }

            if(bIsFind) continue;

            Log.e(TAG,"autoCallApi->not find "+p.getName());
            lstNeeds.add(p);
        }

        //查找缺失字段的值
        if(!lstNeeds.isEmpty()){
            Log.e(TAG,"autoCallApi->starting fill field");
            //补全db参数
            if(!ParamGetter.containsDbT(allGetters,DatabaseConfig.TYPE_USER_OVERALL)){
                Log.e(TAG,"autoCallApi->starting fill field with database");
                //input -> db
                ParamGetter inputGetter = ParamGetter.getInputT(allGetters);
                if(inputGetter != null){
                    Log.e(TAG,"autoCallApi->starting fill field with database by input");
                    Map<String,ESONObject> mLocal = new HashMap<>();
                    Map<String,Integer> mCounter  = new HashMap<>();
                    int maxCount = -1;
                    String maxIdCardNo = null;

                    for (Iterator<String> it = inputGetter.data.keys(); it.hasNext(); ) {
                        String key = it.next();
                        ApiParam p = config.getFieldMapper().get(key);
                        if(p == null) continue;
                        Object value = inputGetter.getValue(key);

                        List<String> lstK = new ArrayList<>();
                        lstK.add(key);

                        List<Object> lstV = new ArrayList<>();
                        lstV.add(value);
                        int queryCount = -1;
                        for(int i=0;i<5 && queryCount < 0;++i){
                            queryCount = db.countSync(lstK,lstV);
                        }
                        if(queryCount != 1) continue;
                        ESONArray array = null;
                        for(int i=0;i<5 || array==null || array.length()!=1;++i){
                            array = db.query(lstK,lstV);
                        }
                        if(array == null || array.length() != 1) continue;
                        ESONObject item = array.getArrayValue(0,new ESONObject());

                        String idCardNo = item.getJSONValue(db.getIdFieldName(),"");
                        if(idCardNo.trim().isEmpty()) continue;

                        mLocal.put(idCardNo, item);
                        Integer count = mCounter.get(idCardNo);
                        if(count == null) count = 0;
                        count = count +1;
                        if(maxCount<count){
                            maxCount = count;
                            maxIdCardNo = idCardNo;
                        }
                        mCounter.put(idCardNo, count);
                    }

                    if(maxIdCardNo!=null){
                        Log.e(TAG,"autoCallApi->fill field with database by input success " + maxIdCardNo);
                        ESONObject e = mLocal.get(maxIdCardNo);
                        allGetters.add(ParamGetter.fromDb(DatabaseConfig.TYPE_USER_OVERALL,e));
                    }
                }

                //response -> db
                ParamGetter dbGetter = ParamGetter.getDbT(allGetters,DatabaseConfig.TYPE_USER_OVERALL);
                if(dbGetter == null){
                    Log.e(TAG,"autoCallApi->starting fill field with database by api getter");
                    for(ParamSetter apiSetter : apiSetters){
                        Map<String,String> apiMapper = mAllSetterMapper.get(apiSetter.type);
                        if(apiMapper == null || apiMapper.isEmpty()) continue;
                        Map<String,String> temp = new HashMap<>();
                        for (Map.Entry<String, String> entry : apiMapper.entrySet()) {
                            temp.put(entry.getValue(),entry.getKey());
                        }
                        apiMapper = temp;

                        Map<String,ESONObject> mLocal = new HashMap<>();
                        Map<String,Integer> mCounter  = new HashMap<>();
                        int maxCount = -1;
                        String maxIdCardNo = null;

                        for (Iterator<String> it = apiSetter.data1.keys(); it.hasNext(); ) {
                            String originKey = it.next();//api response key
                            String key = apiMapper.get(originKey);//db key
                            if(key == null) continue;

                            ApiParam p = config.getFieldMapper().get(key);
                            if(p == null) continue;

                            Object value = apiSetter.getValue(originKey);

                            List<String> lstK = new ArrayList<>();
                            lstK.add(key);

                            List<Object> lstV = new ArrayList<>();
                            lstV.add(value);
                            int queryCount = -1;
                            for(int i=0;i<5 && queryCount < 0;++i){
                                queryCount = db.countSync(lstK,lstV);
                            }
                            if(queryCount != 1) continue;
                            ESONArray array = null;
                            for(int i=0;i<5 || array==null || array.length()!=1;++i){
                                array = db.query(lstK,lstV);
                            }
                            if(array == null || array.length() != 1) continue;
                            ESONObject item = array.getArrayValue(0,new ESONObject());

                            String idCardNo = item.getJSONValue(db.getIdFieldName(),"");
                            if(idCardNo.trim().isEmpty()) continue;

                            mLocal.put(idCardNo, item);
                            Integer count = mCounter.get(idCardNo);
                            if(count == null) count = 0;
                            count = count +1;
                            if(maxCount<count){
                                maxCount = count;
                                maxIdCardNo = idCardNo;
                            }
                            mCounter.put(idCardNo, count);
                        }

                        if(maxIdCardNo!=null){
                            ESONObject e = mLocal.get(maxIdCardNo);
                            allGetters.add(ParamGetter.fromDb(DatabaseConfig.TYPE_USER_OVERALL,e));
                            Log.e(TAG,"autoCallApi->fill field with database by api getter success " + maxIdCardNo);
                        }
                    }
                }
            }

            Log.e(TAG,"autoCallApi->starting fill field with api");
            //补全Api参数(Api依赖)
            for(ApiParam p: lstNeeds){
                List<String> dataGetter = p.getDataGetter();
                for(String field:dataGetter){
                    if(field == null) continue;
                    if(!field.startsWith("Api")) continue;

                    Integer type = getApiType(field);
                    if(type == null) continue;

                    String key = getApiKey(field);
                    if(key == null) continue;

                    //发起调用补全
                    if(reqApiList.contains(type)) {
                        throw new InvalidParameterException("请求Api失败，Api调用已形成闭环！");
                    }

                    Log.e(TAG,"autoCallApi->recursive api");
                    autoCallApi(allGetters, apiSetters, Template.getCurrentTemplate().apiOf(type), sb, reqApiList);
                }
            }
        }

        Log.e(TAG,"autoCallApi->starting fill params");
        //填充请求参数
        List<Object> lstReq = new ArrayList<>();
        for(int i=0,ni=lstParams.size();i<ni;++i) {
            ApiParam p = lstParams.get(i);
            List<String> dataGetters = p.getDataGetter();
            Object value = null;

            boolean bIsFind = false;
            for(String dataGetter:dataGetters){
                if(dataGetter == null) continue;
                if(dataGetter.startsWith("Api")){
                    Integer type = getApiType(dataGetter);
                    if(type == null) continue;

                    String key = getApiKey(dataGetter);
                    if(key == null) continue;

                    ParamSetter setter = ParamSetter.getT(apiSetters,type);
                    if(setter == null) continue;

                    if(setter.isMap()){
                        if(setter.containsV(key)){
                            bIsFind = true;
                            value = setter.getValue(key);
                            break;
                        }
                        continue;
                    }

                    if(setter.isArray()){
                        Map<String,String> mResponseSetterMapper = mAllSetterMapper.get(type);
                        if(mResponseSetterMapper == null) mResponseSetterMapper = new HashMap<>();

                        //response -> input 定位
                        Map<String,ESONObject> mGlobal = new HashMap<>();
                        Map<String,Integer> mGlobalCounter  = new HashMap<>();
                        String maxGlobalIdCardNo = null;
                        int maxGlobalCount = -1;
                        for(ESONObject item:setter.data2){
                            Map<String,ESONObject> mLocal = new HashMap<>();
                            Map<String,Integer> mCounter  = new HashMap<>();
                            int maxCount = -1;
                            String maxIdCardNo = null;

                            for (Map.Entry<String, String> entry : mResponseSetterMapper.entrySet()) {
                                String dbKey = entry.getKey();
                                ApiParam p1 = config.getFieldMapper().get(dbKey);
                                if(p1 == null) continue;
                                Object v = null;
                                try { v = item.get(entry.getValue()); } catch (Exception e) { }


                                List<String> lstK = new ArrayList<>();
                                lstK.add(dbKey);

                                List<Object> lstV = new ArrayList<>();
                                lstV.add(v);
                                int queryCount = -1;
                                for(int ii=0;ii<5 && queryCount < 0;++ii){
                                    queryCount = db.countSync(lstK,lstV);
                                }
                                if(queryCount != 1) continue;
                                ESONArray array = null;
                                for(int ii=0;ii<5 || array==null || array.length()!=1;++ii){
                                    array = db.query(lstK,lstV);
                                }
                                if(array == null || array.length() != 1) continue;
                                ESONObject result = array.getArrayValue(0,new ESONObject());

                                String idCardNo = result.getJSONValue(db.getIdFieldName(),"");
                                if(idCardNo.trim().isEmpty()) continue;

                                mLocal.put(idCardNo, result);
                                Integer count = mCounter.get(idCardNo);
                                if(count == null) count = 0;
                                count = count +1;
                                if(maxCount<count){
                                    maxCount = count;
                                    maxIdCardNo = idCardNo;
                                }
                                mCounter.put(idCardNo, count);
                            }

                            //response -> db 定位
                            if(maxIdCardNo!=null){
                                ESONObject e = mLocal.get(maxIdCardNo);
                                ParamGetter localGetter = ParamGetter.fromDb(DatabaseConfig.TYPE_USER_OVERALL,e);
                                ParamGetter inputGetter = ParamGetter.getInputT(allGetters);

                                Integer count = mGlobalCounter.get(maxIdCardNo);
                                if(count == null) count = 0;
                                count = count +1;
                                if(maxGlobalCount<count){
                                    maxGlobalCount = count;
                                    maxGlobalIdCardNo = maxIdCardNo;
                                }
                                mGlobalCounter.put(maxIdCardNo, count);
                                mGlobal.put(maxGlobalIdCardNo, item);

                                //判断身份号是否一致
                                if(localGetter.containsV(db.getIdFieldName()) && inputGetter.containsV(db.getIdFieldName())){
                                    Object localValue = localGetter.getValue(db.getIdFieldName());
                                    Object inputValue = inputGetter.getValue(db.getIdFieldName());
                                    if(localValue != null && inputValue!=null){
                                        String localIdCardNo = String.valueOf(localValue);
                                        String inputIdCardNo = String.valueOf(inputValue);
                                        if(localIdCardNo.equalsIgnoreCase(inputIdCardNo)){
                                            ParamSetter itemSetter = new ParamSetter(setter.type,item);
                                            if(itemSetter.containsV(key)){
                                                bIsFind = true;
                                                value = itemSetter.getValue(key);
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if(bIsFind) break;

                        if(maxGlobalIdCardNo != null){
                            ESONObject e = mGlobal.get(maxGlobalIdCardNo);
                            ParamSetter itemSetter = new ParamSetter(setter.type,e);
                            if(itemSetter.containsV(key)){
                                bIsFind = true;
                                value = itemSetter.getValue(key);
                                break;
                            }
                        }
                    }

                }
                if("EXCEL".equalsIgnoreCase(dataGetter)){
                    ParamGetter excelGetter = ParamGetter.getExcelT(allGetters);
                    if(excelGetter == null) continue;
                    if(!excelGetter.containsV(p.getName())) continue;

                    value = excelGetter.getValue(p.getName());
                    bIsFind = true;
                    break;
                }
                if("INPUT".equalsIgnoreCase(dataGetter)){
                    ParamGetter inputGetter = ParamGetter.getInputT(allGetters);
                    if(inputGetter == null) continue;
                    if(!inputGetter.containsV(p.getName())) continue;

                    value = inputGetter.getValue(p.getName());
                    bIsFind = true;
                    break;
                }

                if(!dataGetter.startsWith("datablock@")) continue;
                String key = dataGetter.substring("datablock@".length());
                ParamGetter dbGetter = ParamGetter.getDbT(allGetters,DatabaseConfig.TYPE_USER_OVERALL);

                if(dbGetter == null) continue;
                if(!dbGetter.containsV(key)) continue;
                value = dbGetter.getValue(p.getName());
            }

            if(!bIsFind){
                ParamGetter dbGetter = ParamGetter.getDbT(allGetters,DatabaseConfig.TYPE_USER_OVERALL);
                if(dbGetter != null && dbGetter.containsV(p.getName())){
                    bIsFind = true;
                    value = dbGetter.getValue(p.getName());
                }
            }

            if(!bIsFind && !p.canEmpty()){
                throw new InvalidParameterException("["+p.getDescription()+"]不能为空！");
            }

            if(!bIsFind){
                if(p.getDefaultValue()!=null){
                    value = DataParser.parseObject(p.getDefaultValue(),p,true);
                }
            }

            Log.e(TAG,"autoCallApi->fill field "+p.getName() +":"+value);
            lstReq.add(value);
        }

        //发起调用
        Log.e(TAG,"autoCallApi->starting call api");
        AtomicBoolean bIsSuccess = new AtomicBoolean(false);
        final String[] errMsg = {null};
        for(int i=0;i<3 && !bIsSuccess.get();++i){
            target.doRequestInternal(lstReq, new ApiListener() {
                @Override
                public void onSuccess(int code, Object msg, StringBuilder sbLog) {
                    bIsSuccess.set(true);
                    Log.e(TAG,"autoCallApi->api result ok:"+msg);
                    ParamSetter setter = null;
                    if(msg == null){
                        Object value = null;
                        setter = new ParamSetter(target.getType(),value);
                        Log.e(TAG,"autoCallApi->api result setter:"+setter.itemType + " NULL");
                    }
                    else if(msg instanceof JSONObject){
                        setter = new ParamSetter(target.getType(),new ESONObject(msg));
                        Log.e(TAG,"autoCallApi->api result setter:"+setter.itemType+" "+setter.data1);
                    }
                    else if(msg instanceof JSONArray){
                        setter = new ParamSetter(target.getType(),new ESONArray(msg));
                        Log.e(TAG,"autoCallApi->api result setter:"+setter.itemType+" "+setter.data2);
                    }
                    else {
                        setter = new ParamSetter(target.getType(),msg);
                        Log.e(TAG,"autoCallApi->api result setter:"+setter.itemType+" "+setter.data3);
                    }
                    apiSetters.add(setter);
                }

                @Override
                public void onFailure(int code, Object msg, StringBuilder sbLog) {
                    Log.e(TAG,"autoCallApi->api result err:"+msg);
                    if(msg != null && msg instanceof String) errMsg[0] = (String) msg;
                }
            },sb);
        }
        if(!bIsSuccess.get()){
            throw new InvalidParameterException("请求Api["+target.getType()+"]失败："+errMsg[0]);
        }
    }

    //region 获取身份信息
    public interface IPeopleInfoListener{
        void onSuccess(ESONObject data);
        void onFailure(String err);
    }
    public static void requestPeopleInfoByIdCard(String idCardNo, IPeopleInfoListener listener){
        ThreadPoolProvider.getFixedThreadPool().execute(()->requestPeopleInfoByIdCardSync(idCardNo, new IPeopleInfoListener() {
            @Override
            public void onSuccess(ESONObject data) {
                App.Post(()->listener.onSuccess(data));
            }

            @Override
            public void onFailure(String err) {
                App.Post(()->listener.onFailure(err));
            }
        }));
    }
    public static void requestPeopleInfoByIdCardSync(String idCardNo, IPeopleInfoListener listener){
        ApiListener apiListener = new ApiListener() {
            @Override
            public void onSuccess(int code, Object msg, StringBuilder sbLog) {
                ESONObject data = null;
                if(msg instanceof JSONArray){
                    ESONArray arr = new ESONArray(msg);
                    if(arr.length()==0){
                        if(!idCardNo.toLowerCase().equals(idCardNo)){
                            requestPeopleInfoByIdCardSync(idCardNo.toLowerCase(),listener);
                            return;
                        }
                        data = new ESONObject();
                    }
                    else{
                        data = arr.getArrayValue(0,new ESONObject());
                    }
                }
                else{
                    data = new ESONObject(msg);
                }
                final ESONObject result = data;

                if(Constants.Net.isDebug && data!=null && data.length()>0){
                    if(idCardNo.trim().length()==18){
                        data.putValue(Template.getCurrentTemplate().getUserOverallDatabase().getIdFieldName(),idCardNo);
                    }
                }

                try {
                    String id = data.getJSONValue(Template.getCurrentTemplate().getIdCardNoFieldName(),"");
                    StringBuilder sb = new StringBuilder();
                    sb.append(id);
                    String type = CheckUtils.isValidCard(Template.getCurrentTemplate().getApiConfig().getCard(), sb);
                    type = CheckUtils.typeOfCard(type);
                    if(type != null) {
                        data.putValue(Template.getCurrentTemplate().getApiConfig().getCard().field.get(0),type);
                    }
                } catch (Exception e) {}

                listener.onSuccess(result);
            }

            @Override
            public void onFailure(int code, Object msg, StringBuilder sbLog) {
                listener.onFailure(msg == null ? "数据请求失败！" : msg.toString());
            }
        };

        StringBuilder sb = new StringBuilder();
        try {
            List<ParamGetter> getters = new ArrayList<>();
                              getters.add(ParamGetter.fromDb(DatabaseConfig.TYPE_USER_OVERALL,new ESONObject().putValue(Template.getCurrentTemplate().getUserOverallDatabase().getIdFieldName(),idCardNo)));
            List<ParamSetter> setters = getDefaultSetter();
            List<Integer>     request = new ArrayList<>();
                              request.add(Api.TYPE_GET_PEOPLE_INFO);
                              
            autoCallApi(getters,setters,Template.getCurrentTemplate().apiOf(3),sb, request);
            ParamSetter result = ParamSetter.getT(setters,Api.TYPE_GET_PEOPLE_INFO);
            if(result == null) throw new InvalidParameterException("请求失败！");

            switch (result.itemType){
                case "MAP":
                    break;
                case "ARRAY":
                    if(result.data2 == null || result.data2.isEmpty()){
                        result = new ParamSetter(Api.TYPE_GET_PEOPLE_INFO, new ESONObject());
                    }
                    else{
                        result = new ParamSetter(Api.TYPE_GET_PEOPLE_INFO, result.data2.get(0));
                    }
                    break;
                default:
                    throw new InvalidParameterException("请求失败！");
            }

            final ESONObject response = result.data1;
            apiListener.onSuccess(0,response,sb);
        } catch (Exception e) {
            apiListener.onFailure(0,e.getMessage(),sb);
            e.printStackTrace();
        }
    }
    //endregion

    //region 提交核酸采样
    public interface INCSamplingListener{
        void onSuccess();
        void onFailure(String err);
    }
    public static void requestNucleicAcidSampling(String barcode, ESONObject dbData, INCSamplingListener listener){
        ThreadPoolProvider.getFixedThreadPool().execute(()->requestNucleicAcidSamplingSync(barcode, dbData, new INCSamplingListener() {
            @Override
            public void onSuccess() {
                App.Post(()->listener.onSuccess());
            }

            @Override
            public void onFailure(String err) {
                App.Post(()->listener.onFailure(err));
            }
        }));
    }
    public static void requestNucleicAcidSamplingSync(String barcode, ESONObject dbData, INCSamplingListener listener){
        ApiListener apiListener = new ApiListener() {
            @Override
            public void onSuccess(int code, Object msg, StringBuilder sbLog) {
                listener.onSuccess();
            }
            @Override
            public void onFailure(int code, Object msg, StringBuilder sbLog) {
                listener.onFailure(msg == null ? "数据请求失败！" : msg.toString());
            }
        };

        StringBuilder sb = new StringBuilder();
        try {
            List<ParamGetter> getters = new ArrayList<>();
            List<String> lstBarcode = Template.getCurrentTemplate().getApiConfig().getScan().getLstBarcodeFields();
            for(String field: lstBarcode){
                Integer type = getApiType(field);
                if(type == null) continue;
                if(type != Api.TYPE_NC_PEOPLE_SAMPLING) continue;
                String name = getApiKey(field);
                if(name == null) continue;
                ParamGetter inputGetter = ParamGetter.fromInput(new ESONObject().putValue(name,barcode));
                getters.add(inputGetter);
            }

            if(getters.isEmpty()) throw new InvalidParameterException("条码字段不能为空！");

            getters.add(ParamGetter.fromDb(DatabaseConfig.TYPE_USER_OVERALL,dbData));
            List<ParamSetter> setters = getDefaultSetter();
            List<Integer>     request = new ArrayList<>();
            request.add(Api.TYPE_NC_PEOPLE_SAMPLING);

            autoCallApi(getters,setters,Template.getCurrentTemplate().apiOf(Api.TYPE_NC_PEOPLE_SAMPLING),sb, request);
            ParamSetter result = ParamSetter.getT(setters,Api.TYPE_NC_PEOPLE_SAMPLING);
            if(result == null) throw new InvalidParameterException("请求失败！");

            apiListener.onSuccess(0,null,sb);
        } catch (Exception e) {
            apiListener.onFailure(0,e.getMessage(),sb);
        }

    }
    //endregion

    //region 获取试管采样记录
    public interface ISamplingResultListener{
        void onSuccess(ESONArray data);
        void onFailure(String err);
    }
    public static void requestSamplingResult(String barcode, ISamplingResultListener listener){
        ThreadPoolProvider.getFixedThreadPool().execute(()->requestSamplingResultSync(barcode, new ISamplingResultListener() {
            @Override
            public void onSuccess(ESONArray data) {
                App.Post(()->listener.onSuccess(data));
            }

            @Override
            public void onFailure(String err) {
                App.Post(()->listener.onFailure(err));
            }
        }));
    }
    public static void requestSamplingResultSync(String barcode, ISamplingResultListener listener){
        Api api = Template.getCurrentTemplate().apiOf(Api.TYPE_NC_PEOPLE_INFO);

        ApiListener apiListener = new ApiListener() {
            @Override
            public void onSuccess(int code, Object msg, StringBuilder sbLog) {
                ESONArray data = null;
                if(msg instanceof JSONArray){
                    data =new ESONArray(msg);
                }
                else if(msg instanceof JSONObject){
                    data = new ESONArray();
                    data.putValue(msg);
                }
                else{
                    data = new ESONArray(msg);
                }
                final ESONArray result = data;
                listener.onSuccess(result);
            }

            @Override
            public void onFailure(int code, Object msg, StringBuilder sbLog) {
                listener.onFailure(msg == null ? "数据请求失败！" : msg.toString());
            }
        };

        StringBuilder sb = new StringBuilder();
        try {
            List<ParamGetter> getters = new ArrayList<>();
            ESONObject eInput =  new ESONObject().putValue(api.getRequest().getParams().get(0).getName(),barcode);
            ParamGetter getter = new ParamGetter(2,1,eInput,api.getRequest().getParams());
            getters.add(getter);

            List<ParamSetter> setters = getDefaultSetter();
            List<Integer>     request = new ArrayList<>();
            request.add(Api.TYPE_NC_PEOPLE_INFO);

            autoCallApi(getters,setters,Template.getCurrentTemplate().apiOf(Api.TYPE_NC_PEOPLE_INFO), sb, request);
            ParamSetter result = ParamSetter.getT(setters,Api.TYPE_NC_PEOPLE_INFO);
            if(result == null) throw new InvalidParameterException("请求失败！");

            switch (result.itemType){
                case "MAP":
                    result = new ParamSetter(Api.TYPE_NC_PEOPLE_INFO, new ESONArray().putValue(result.data1));
                    break;
                case "ARRAY":
                    break;
                default:
                    throw new InvalidParameterException("请求失败！");
            }

            final ESONArray response = JsonUtils.parse2Array(result.data2);
            apiListener.onSuccess(0,response,sb);
        } catch (Exception e) {
            apiListener.onFailure(0,e.getMessage(),sb);
        }
    }
    //endregion

    //region 获取核酸历史采样记录
    public interface ISamplingRecordQueryListener{
        void onSuccess(ESONArray data);
        void onFailure(String err);
    }
    public static void requestQuerySamplingHistoryRecord(ESONObject input, ISamplingRecordQueryListener listener){
        ThreadPoolProvider.getFixedThreadPool().execute(()->requestQuerySamplingHistoryRecordSync(input, new ISamplingRecordQueryListener() {
            @Override
            public void onSuccess(ESONArray data) {
                App.Post(()->listener.onSuccess(data));
            }

            @Override
            public void onFailure(String err) {
                App.Post(()->listener.onFailure(err));
            }
        }));
    }
    public static void requestQuerySamplingHistoryRecordSync(ESONObject input, ISamplingRecordQueryListener listener){

        ApiListener apiListener = new ApiListener() {
            @Override
            public void onSuccess(int code, Object msg, StringBuilder sbLog) {
                ESONArray data = null;
                if(msg instanceof JSONArray){
                    data =new ESONArray(msg);
                }
                else if(msg instanceof JSONObject){
                    data = new ESONArray();
                    data.putValue(msg);
                }
                else{
                    data = new ESONArray(msg);
                }
                final ESONArray result = data;
                listener.onSuccess(result);
            }

            @Override
            public void onFailure(int code, Object msg, StringBuilder sbLog) {
                listener.onFailure(msg == null ? "数据请求失败！" : msg.toString());
            }
        };

        StringBuilder sb = new StringBuilder();
        try {
            List<ParamGetter> getters = new ArrayList<>();
            ParamGetter getter = new ParamGetter(2,1,input,Template.getCurrentTemplate().apiOf(Api.TYPE_NC_HISTORY_SEARCH).getRequest().getParams());

            getters.add(getter);

            List<ParamSetter> setters = getDefaultSetter();
            List<Integer>     request = new ArrayList<>();
            request.add(Api.TYPE_NC_HISTORY_SEARCH);

            autoCallApi(getters,setters,Template.getCurrentTemplate().apiOf(Api.TYPE_NC_HISTORY_SEARCH), sb, request);
            ParamSetter result = ParamSetter.getT(setters,Api.TYPE_NC_HISTORY_SEARCH);
            if(result == null) throw new InvalidParameterException("请求失败！");

            switch (result.itemType){
                case "MAP":
                    result = new ParamSetter(Api.TYPE_NC_HISTORY_SEARCH, new ESONArray().putValue(result.data1));
                    break;
                case "ARRAY":
                    break;
                default:
                    throw new InvalidParameterException("请求失败！");
            }

            final ESONArray response = JsonUtils.parse2Array(result.data2);
            apiListener.onSuccess(0,response,sb);
        } catch (Exception e) {
            apiListener.onFailure(0,e.getMessage(),sb);
        }
    }
    //endregion

    //region 删除采样记录
    public interface ISamplingRecordDeleteListener{
        void onSuccess();
        void onFailure(String err);
    }
    public static void requestDeleteSamplingRecordWithTubeNo(ParamSetter responseSetter, ISamplingRecordDeleteListener listener){
        requestDeleteSamplingRecordWithTubeNo(null,responseSetter,listener);
    }
    public static void requestDeleteSamplingRecordWithTubeNo(String barcode, ParamSetter responseSetter, ISamplingRecordDeleteListener listener){
        ThreadPoolProvider.getFixedThreadPool().execute(()->requestDeleteSamplingRecordWithTubeNoSnyc(barcode, responseSetter, new ISamplingRecordDeleteListener() {
            @Override
            public void onSuccess() {
                App.Post(()->listener.onSuccess());
            }

            @Override
            public void onFailure(String err) {
                App.Post(()->listener.onFailure(err));
            }
        }));
    }
    public static void requestDeleteSamplingRecordWithTubeNoSnyc(String barcode, ParamSetter responseSetter, ISamplingRecordDeleteListener listener){
        App.Post(()-> LoadingDialog.showDialog("Call delete api",App.sInstance.getCurrentActivity()));
        ApiListener apiListener = new ApiListener() {
            @Override
            public void onSuccess(int code, Object msg, StringBuilder sbLog) {
                App.Post(()-> LoadingDialog.dismissDialog("Call delete api"));
                listener.onSuccess();
            }
            @Override
            public void onFailure(int code, Object msg, StringBuilder sbLog) {
                App.Post(()-> LoadingDialog.dismissDialog("Call delete api"));
                listener.onFailure(msg == null ? "数据请求失败！" : msg.toString());
            }
        };

        StringBuilder     sb      = new StringBuilder();
        try {
            List<ParamGetter> getters = new ArrayList<>();
            if(barcode != null && !barcode.isEmpty()){
                List<String> lstBarcode = Template.getCurrentTemplate().getApiConfig().getScan().getLstBarcodeFields();
                for(String field: lstBarcode){
                    Integer type = getApiType(field);
                    if(type == null) continue;
                    if(type != Api.TYPE_NC_HISTORY_SEARCH) continue;
                    String name = getApiKey(field);
                    if(name == null) continue;
                    ParamGetter inputGetter = ParamGetter.fromInput(new ESONObject().putValue(name,barcode));
                    getters.add(inputGetter);
                }
            }
            List<ParamSetter> setters = getDefaultSetter();
                              setters.add(responseSetter);
            List<Integer>     request = new ArrayList<>();
            request.add(Api.TYPE_NC_PEOPLE_SAMPLING);

            autoCallApi(getters,setters,Template.getCurrentTemplate().apiOf(Api.TYPE_NC_DELETE),sb, request);
            ParamSetter result = ParamSetter.getT(setters,Api.TYPE_NC_DELETE);
            if(result == null) throw new InvalidParameterException("请求失败！");

            apiListener.onSuccess(0,null, sb);

            ParamGetter dbGetter = ParamGetter.getDbT(getters,DatabaseConfig.TYPE_USER_OVERALL);
            if(dbGetter!=null){
                try {
                    String idCardNo = String.valueOf(dbGetter.getValue(Template.getCurrentTemplate().getIdCardNoFieldName()));
                    for(int i=0;i<5;++i){
                        Log.e(TAG,"delete->"+idCardNo+" "+i);
                        if(Template.getCurrentTemplate().getDaySamplingLogDatabase().del(idCardNo)) break;
                    }
                } catch (Exception e) {}
            }
        } catch (Exception e) {
            apiListener.onFailure(0,e.getMessage(),sb);
        }
    }
    //endregion
}
