package com.dreaming.hscj.template.api.impl;

import android.util.Log;

import com.dreaming.hscj.template.api.wrapper.ApiResponse;
import com.dreaming.hscj.utils.CheckUtils;
import com.dreaming.hscj.utils.JsonUtils;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

public class ApiConfig {
    private static final String TAG = ApiConfig.class.getSimpleName();

    private Locate.Authorization authorization;
    public Locate.Authorization getAuthorization(){
        return authorization;
    }

    private Test test;
    public Test getTest(){
        return test;
    }

    private Permission permission;
    public Permission getPermission(){
        return permission;
    }

    private Locate.Scan scan;
    public Locate.Scan getScan(){
        return scan;
    }

    private Locate.Card card;
    public Locate.Card getCard() {
        return card;
    }

    private Locate.Id id;
    public Locate.Id getId() {
        return id;
    }

    private Locate.Phone phone;
    public Locate.Phone getPhone() {
        return phone;
    }

    private Locate.Query query;
    public Locate.Query getQuery() {
        return query;
    }

    public static ApiConfig parse(ESONObject data) throws Exception {
        ApiConfig apiConfig = new ApiConfig();

        apiConfig.authorization = Locate.Authorization.parse(
                "ApiConfig[\"locate\"][\"authorization\"]",
                data.getJSONValue("locate",new ESONObject())
                    .getJSONValue("authorization",new ESONObject())
        );
        Log.e(TAG,"init Authorization success!");

        apiConfig.scan          = Locate.Scan.parse(
                "ApiConfig[\"locate\"][\"scan\"]",
                data.getJSONValue("locate",new ESONObject())
                        .getJSONValue("scan",new ESONObject())
        );
        Log.e(TAG,"init Scan success!");

        apiConfig.card          = Locate.Card.parse(
                "ApiConfig[\"locate\"][\"card\"]",
                data.getJSONValue("locate",new ESONObject())
                        .getJSONValue("card",new ESONObject())
        );

        apiConfig.id            = Locate.Id.parse(
                "ApiConfig[\"locate\"][\"id\"]",
                data.getJSONValue("locate",new ESONObject())
                        .getJSONValue("id",new ESONArray())
        );

        apiConfig.phone         = Locate.Phone.parse(
                "ApiConfig[\"locate\"][\"phone\"]",
                data.getJSONValue("locate",new ESONObject())
                        .getJSONValue("phone",new ESONArray())
        );

        apiConfig.query         = Locate.Query.parse(
                "ApiConfig[\"locate\"][\"query\"]",
                data.getJSONValue("locate",new ESONObject())
                        .getJSONValue("query",new ESONObject())
        );

        apiConfig.test          = Test.parse(
                "ApiConfig[\"test\"]",
                data.getJSONValue("test",new ESONObject())
        );
        Log.e(TAG,"init Test success!");

        apiConfig.permission    = Permission.parse(
                "ApiConfig[\"permission\"]",
                data.getJSONValue("permission",new ESONObject())
        );
        Log.e(TAG,"init Permission success!");

        Map<String,String> mCI = new HashMap<>();
        ESONObject obj = data.getJSONValue("locate",new ESONObject())
                .getJSONValue("community_info",new ESONObject());

        for (Iterator<String> it = obj.keys(); it.hasNext(); ) {
            String key  = it.next();
            String value= obj.getJSONValue(key,"");
            String tag  = "ApiConfig[\"Locate\"][\"community_info\"]["+key+"]";
            if(value.isEmpty()){
                throw new InvalidParameterException(tag +"不能为空！");
            }
            if(value.isEmpty() || value.replaceAll("Api\\[[0-6]\\]\\[\\\"[a-zA-Z_]([a-zA-Z0-9_]*)\\\"\\]","").length()!=0){
                throw new InvalidParameterException(tag+"无法解析！");
            }
            mCI.put(key,value);
        }
        apiConfig.mCommunityInfo = mCI;

        return apiConfig;
    }

    private ApiConfig(){}

    public static class Locate{
        public static class Authorization{
            private static Authorization parse(String tag, ESONObject data) throws Exception {
                Authorization authorization = new Authorization();

                String token = data.getJSONValue("token","");
                if(token.isEmpty()||!token.startsWith("Api")) throw new InvalidParameterException(tag+"[\"token\"]表达式无法解析！");
                ApiResponse.autoCheckExpressionLeft(tag+"[\"token\"]",token.substring(3));
                token = token.substring(6);
                token = token.substring(2,token.length()-2);
                authorization.token = token;

                String expired= data.getJSONValue("expired","");
                if(expired.isEmpty()||!expired.startsWith("Api")) throw new InvalidParameterException(tag+"[\"expired\"]表达式无法解析！");
                ApiResponse.autoCheckExpressionLeft(tag+"[\"expired\"]",expired.substring(3));
                expired = expired.substring(6);
                expired = expired.substring(2,expired.length()-2);

                authorization.expired = expired;

                return authorization;
            }
            private String token;
            public Authorization setToken(String token){
                this.token = token;
                return this;
            }
            public String getToken(){
                return token;
            }

            private String expired;
            public Authorization setExpired(String expired){
                this.expired = expired;
                return this;
            }
            public String getExpired(){
                return expired;
            }
        }

        public static class Scan{
            public static Scan parse(String tag, ESONObject data){
                Scan scan = new Scan();

                ESONArray arrBarcode = data.getJSONValue("barcode",new ESONArray());
                List<String> lstBarcode = new ArrayList<>();
                for(int i=0,ni=arrBarcode.length();i<ni;++i){
                    String field = arrBarcode.getArrayValue(i,"");
                    if(field.isEmpty() || field.replaceAll("Api\\[[0-6]\\]\\[\\\"[a-zA-Z_]([a-zA-Z0-9_]*)\\\"\\]","").length()!=0){
                        throw new InvalidParameterException(tag+"[\"barcode\"]["+i+"]无法解析！");
                    }
                    lstBarcode.add(field);
                }
                scan.lstBarcodeFields = lstBarcode;

                ESONArray arrICId    = data.getJSONValue("id card",new ESONArray());
                List<String> lstICId = new ArrayList<>();
                for(int i=0,ni=arrICId.length();i<ni;++i){
                    String field = arrICId.getArrayValue(i,"");
                    if(field.isEmpty() || field.replaceAll("Api\\[[0-6]\\]\\[\\\"[a-zA-Z_]([a-zA-Z0-9_]*)\\\"\\]","").length()!=0){
                        throw new InvalidParameterException(tag+"[\"barcode\"]["+i+"]无法解析！");
                    }
                    lstICId.add(field);
                }
                scan.lstIdCardIdFields = lstICId;

                return scan;
            }

            private Scan(){}

            List<String> lstBarcodeFields;
            public List<String> getLstBarcodeFields() {
                return lstBarcodeFields;
            }

            List<String> lstIdCardIdFields;
            public List<String> getLstIdCardIdFields() {
                return lstIdCardIdFields;
            }
        }

        public static class Card{
            public static Card parse(String tag, ESONObject data){
                Card card = new Card();
                List<String> normal = JsonUtils.parse2List(data.getJSONValue("normal",new ESONArray()),"");
                List<CheckUtils.CardType> support = new ArrayList<>();
                Map<String,String> mapper = new HashMap<>();
                ESONObject eQuery = data.getJSONValue("type mapper",new ESONObject());
                for(int i=0,ni=normal.size();i<ni;++i){
                    String item = normal.get(i);
                    if("ID Card".equalsIgnoreCase(item.trim())){
                        support.add(CheckUtils.CardType.IdCard);
                        String type = eQuery.getJSONValue("ID Card","");
                        if(type.trim().isEmpty()) throw new InvalidParameterException(tag+"[\"normal\"]["+i+"]没有查找到对应的映射Type！");
                        mapper.put(item.trim(),type);
                        continue;
                    }
                    if("Passport Card".equalsIgnoreCase(item.trim())){
                        support.add(CheckUtils.CardType.Passport);
                        String type = eQuery.getJSONValue("Passport Card","");
                        if(type.trim().isEmpty())  throw new InvalidParameterException(tag+"[\"normal\"]["+i+"]没有查找到对应的映射Type！");
                        mapper.put(item.trim(),type);
                        continue;
                    }
                    if("HM Card".equalsIgnoreCase(item.trim())){
                        support.add(CheckUtils.CardType.HMCard);
                        String type = eQuery.getJSONValue("HM Card","");
                        if(type.trim().isEmpty())  throw new InvalidParameterException(tag+"[\"normal\"]["+i+"]没有查找到对应的映射Type！");
                        mapper.put(item.trim(),type);
                        continue;
                    }
                    if("TW Card".equalsIgnoreCase(item.trim())){
                        support.add(CheckUtils.CardType.TWCard);
                        String type = eQuery.getJSONValue("TW Card","");
                        if(type.trim().isEmpty())  throw new InvalidParameterException(tag+"[\"normal\"]["+i+"]没有查找到对应的映射Type！");
                        mapper.put(item.trim(),type);
                        continue;
                    }
                    if("Officer Card".equalsIgnoreCase(item.trim())){
                        support.add(CheckUtils.CardType.Officer);
                        String type = eQuery.getJSONValue("Officer Card","");
                        if(type.trim().isEmpty())  throw new InvalidParameterException(tag+"[\"normal\"]["+i+"]没有查找到对应的映射Type！");
                        mapper.put(item.trim(),type);
                        continue;
                    }
                    throw new InvalidParameterException(tag+"[\"normal\"]["+i+"]非法的支持类型！");
                }

                if(support.isEmpty()){
                    throw new InvalidParameterException(tag + "[\"normal\"]不能为空！");
                }
                card.normal = support;

                List<ESONObject> config = JsonUtils.parseToList(data.getJSONValue("other",new ESONArray()));
                List<Config> other = new ArrayList<>();
                for(int i=0,ni=config.size();i<ni;++i){
                    Config c = Config.parse(tag + "[\"other\"]["+i+"]",config.get(i));
                    other.add(c);
                }
                card.other = other;
                for (Config cfg : card.other) {
                    if(mapper.containsKey(cfg.name)) throw new InvalidParameterException(tag+"[\"other\"]名称有重复！");
                    mapper.put(cfg.name,cfg.type);
                }

                ESONArray eField = data.getJSONValue("type field", new ESONArray());
                List<String> lst = new ArrayList<>();
                for(int i=0,ni=eField.length();i<ni;++i){
                    String item = eField.getArrayValue(i,"");
                    if(item.replaceAll("Api\\[3\\]\\[\\\"[a-zA-Z_]([a-zA-Z0-9_]*)\\\"\\]","").length()!=0) throw new InvalidParameterException(tag+"["+i+"]只能从Api[3]Response中获取!");

                    try {
                        String s = item.substring(item.indexOf("\"")+1,item.lastIndexOf("\""));
                        lst.add(s);
                    } catch (Exception e) {
                        throw new InvalidParameterException(tag+"["+i+"]只能从Api[3]Response中获取!");
                    }
                }
                card.field = lst;

                card.mapper = mapper;

                return card;
            }

            public static class Config{

                public static Config parse(String tag, ESONObject data){
                    Config config = new Config();

                    String name = data.getJSONValue("name","");
                    if(name == null || name.trim().isEmpty()) throw new InvalidParameterException(tag+"[\"name\"]不能为空！");

                    for(CheckUtils.CardType type : CheckUtils.CardType.values()){
                        if(type.text.equals(name)){
                            throw new InvalidParameterException(tag+"[\"name\"]不能为【"+name+"】！");
                        }
                    }

                    String regex = data.getJSONValue("regex","");
                    if(regex == null || regex.isEmpty()) throw new InvalidParameterException(tag+"[\"regex\"]不能为空！");

                    String type = data.getJSONValue("type","");
                    if(type == null || type.isEmpty()) throw new InvalidParameterException(tag+"[\"type\"]不能为空！");

                    config.name = name;
                    config.regex = regex;
                    config.type = type;

                    return config;
                }

                private Config(){}
                private String name;
                private String regex;
                private String type;
                public String getName() {
                    return name;
                }
                public String getRegex() {
                    return regex;
                }
                public String getType() {
                    return type;
                }
            }

            public List<CheckUtils.CardType> normal;
            public List<Config> other;
            public List<String> field;
            public Map<String,String> mapper;
            private Card(){}
        }

        public static class Id{
            public static Id parse(String tag,ESONArray data){
                Id id = new Id();

                List<String> lst = new ArrayList<>();
                for(int i=0,ni=data.length();i<ni;++i){
                    String item = data.getArrayValue(i,"");
                    if(item.replaceAll("Api\\[3\\]\\[\\\"[a-zA-Z_]([a-zA-Z0-9_]*)\\\"\\]","").length()!=0) throw new InvalidParameterException(tag+"["+i+"]只能从Api[3]Response中获取!");

                    try {
                        String s = item.substring(item.indexOf("\"")+1,item.lastIndexOf("\""));
                        lst.add(s);
                    } catch (Exception e) {
                        throw new InvalidParameterException(tag+"["+i+"]只能从Api[3]Response中获取!");
                    }
                }
                id.id = lst;

                return id;
            }

            private Id(){}

            private List<String> id;
            public List<String> getId() {
                return id;
            }
        }

        public static class Phone{
            public static Phone parse(String tag,ESONArray data){
                Phone phone = new Phone();

                List<String> lst = new ArrayList<>();
                for(int i=0,ni=data.length();i<ni;++i){
                    String item = data.getArrayValue(i,"");
                    if(item.replaceAll("Api\\[4\\]\\[\\\"[a-zA-Z_]([a-zA-Z0-9_]*)\\\"\\]","").length()!=0) throw new InvalidParameterException(tag+"["+i+"]只能从Api[4]Request中获取!");

                    try {
                        String s = item.substring(item.indexOf("\"")+1,item.lastIndexOf("\""));
                        lst.add(s);
                    } catch (Exception e) {
                        throw new InvalidParameterException(tag+"["+i+"]只能从Api[4]Request中获取!");
                    }
                }
                if(lst.isEmpty()) throw new InvalidParameterException(tag+"不能为空！");
                phone.phone = lst;

                return phone;
            }

            private Phone(){}

            private List<String> phone;
            public List<String> getPhone() {
                return phone;
            }
        }

        public static class Query{
            public static Query parse(String tag,ESONObject data){
                Query query = new Query();

                query.request  = Request .parse(tag+"[\"request\"]" ,data.getJSONValue("request",new ESONObject()));
                query.response = Response.parse(tag+"[\"response\"]",data.getJSONValue("response",new ESONObject()));

                return query;
            }

            private Query(){}

            private Request request;
            public Request getRequest() {
                return request;
            }
            public static class Request{
                private static Request parse(String tag,ESONObject data){
                    Request request = new Request();
                    request.pageIndex = data.getJSONValue("page index","");
                    if(request.pageIndex.trim().isEmpty()) throw new InvalidParameterException(tag+"[\"page index\"]不能为空!");
                    if(request.pageIndex.replaceAll("Api\\[6\\]\\[\\\"[a-zA-Z_]([a-zA-Z0-9_]*)\\\"\\]","").length()!=0)
                        throw new InvalidParameterException(tag+"[\"page index\"]只能从Api[6]Request中获取!");
                    request.pageIndex = Api.fieldOf(request.pageIndex);

                    request.pageSize  = data.getJSONValue("page size" ,"");
                    if(request.pageSize.trim().isEmpty()) throw new InvalidParameterException(tag+"[\"page size\"]不能为空!");
                    if(request.pageSize.replaceAll("Api\\[6\\]\\[\\\"[a-zA-Z_]([a-zA-Z0-9_]*)\\\"\\]","").length()!=0)
                        throw new InvalidParameterException(tag+"[\"page size\"]只能从Api[6]Request中获取!");
                    request.pageSize = Api.fieldOf(request.pageSize);

                    request.startDate = data.getJSONValue("start date","");
                    if(request.startDate.trim().isEmpty()) throw new InvalidParameterException(tag+"[\"start date\"]不能为空!");
                    if(request.startDate.replaceAll("Api\\[6\\]\\[\\\"[a-zA-Z_]([a-zA-Z0-9_]*)\\\"\\]","").length()!=0)
                        throw new InvalidParameterException(tag+"[\"start date\"]只能从Api[6]Request中获取!");
                    request.startDate = Api.fieldOf(request.startDate);

                    request.endDate   = data.getJSONValue("end date"  ,"");
                    if(request.endDate.trim().isEmpty()) throw new InvalidParameterException(tag+"[\"end date\"]不能为空!");
                    if(request.endDate.replaceAll("Api\\[6\\]\\[\\\"[a-zA-Z_]([a-zA-Z0-9_]*)\\\"\\]","").length()!=0)
                        throw new InvalidParameterException(tag+"[\"end date\"]只能从Api[6]Request中获取!");
                    request.endDate = Api.fieldOf(request.endDate);

                    request.tubNo     = data.getJSONValue("tub no"    ,"");
                    if(request.tubNo.trim().isEmpty()) throw new InvalidParameterException(tag+"[\"tub no\"]不能为空!");
                    if(request.tubNo.replaceAll("Api\\[6\\]\\[\\\"[a-zA-Z_]([a-zA-Z0-9_]*)\\\"\\]","").length()!=0)
                        throw new InvalidParameterException(tag+"[\"tub no\"]只能从Api[6]Request中获取!");
                    request.tubNo = Api.fieldOf(request.tubNo);

                    request.idNo      = data.getJSONValue("id no"     ,"");
                    if(request.idNo.trim().isEmpty()) throw new InvalidParameterException(tag+"[\"id no\"]不能为空!");
                    if(request.idNo.replaceAll("Api\\[6\\]\\[\\\"[a-zA-Z_]([a-zA-Z0-9_]*)\\\"\\]","").length()!=0)
                        throw new InvalidParameterException(tag+"[\"id no\"]只能从Api[6]Request中获取!");
                    request.idNo = Api.fieldOf(request.idNo);

                    return request;
                }
                private String pageIndex = "";
                public String getPageIndex() {
                    return pageIndex;
                }

                private String pageSize = "";
                public String getPageSize() {
                    return pageSize;
                }

                private String startDate = "";
                public String getStartDate() {
                    return startDate;
                }

                private String endDate = "";
                public String getEndDate() {
                    return endDate;
                }

                private String tubNo = "";
                public String getTubNo() {
                    return tubNo;
                }

                private String idNo = "";
                public String getIdNo() {
                    return idNo;
                }

                private Request(){}
            }

            private Response response;
            public Response getResponse() {
                return response;
            }

            public static class Response{
                private static Response parse(String tag, ESONObject data){
                    Response response = new Response();

                    response.idNo = data.getJSONValue("id no","");
                    if(response.idNo.trim().isEmpty()) throw new InvalidParameterException(tag+"[\"id no\"]不能为空!");
                    if(response.idNo.replaceAll("Api\\[6\\]\\[\\\"[a-zA-Z_]([a-zA-Z0-9_]*)\\\"\\]","").length()!=0)
                        throw new InvalidParameterException(tag+"[\"id no\"]只能从Api[6]Response中获取!");
                    response.idNo = Api.fieldOf(response.idNo);

                    response.name = data.getJSONValue("name","");
                    if(response.name.trim().isEmpty()) throw new InvalidParameterException(tag+"[\"name\"]不能为空!");
                    if(response.name.replaceAll("Api\\[6\\]\\[\\\"[a-zA-Z_]([a-zA-Z0-9_]*)\\\"\\]","").length()!=0)
                        throw new InvalidParameterException(tag+"[\"name\"]只能从Api[6]Response中获取!");
                    response.name = Api.fieldOf(response.name);

                    response.samplingTime = data.getJSONValue("sampling time","");
                    if(response.samplingTime.trim().isEmpty()) throw new InvalidParameterException(tag+"[\"sampling time\"]不能为空!");
                    if(response.samplingTime.replaceAll("Api\\[6\\]\\[\\\"[a-zA-Z_]([a-zA-Z0-9_]*)\\\"\\]","").length()!=0)
                        throw new InvalidParameterException(tag+"[\"sampling time\"]只能从Api[6]Response中获取!");
                    response.samplingTime = Api.fieldOf(response.samplingTime);

                    response.phone = data.getJSONValue("phone","");
                    if(response.phone.trim().isEmpty()) throw new InvalidParameterException(tag+"[\"phone\"]不能为空!");
                    if(response.phone.replaceAll("Api\\[6\\]\\[\\\"[a-zA-Z_]([a-zA-Z0-9_]*)\\\"\\]","").length()!=0)
                        throw new InvalidParameterException(tag+"[\"phone\"]只能从Api[6]Response中获取!");
                    response.phone = Api.fieldOf(response.phone);

                    response.address = data.getJSONValue("address","");
                    if(response.address.trim().isEmpty()) throw new InvalidParameterException(tag+"[\"address\"]不能为空!");
                    if(response.address.replaceAll("Api\\[6\\]\\[\\\"[a-zA-Z_]([a-zA-Z0-9_]*)\\\"\\]","").length()!=0)
                        throw new InvalidParameterException(tag+"[\"address\"]只能从Api[6]Response中获取!");
                    response.address = Api.fieldOf(response.address);

                    response.tubNo = data.getJSONValue("tub no","");
                    if(response.tubNo.trim().isEmpty()) throw new InvalidParameterException(tag+"[\"tub no\"]不能为空!");
                    if(response.tubNo.replaceAll("Api\\[6\\]\\[\\\"[a-zA-Z_]([a-zA-Z0-9_]*)\\\"\\]","").length()!=0)
                        throw new InvalidParameterException(tag+"[\"tub no\"]只能从Api[6]Response中获取!");
                    response.tubNo = Api.fieldOf(response.tubNo);

                    return response;
                }
                private Response(){}
                private String idNo;
                public String getIdNo() {
                    return idNo;
                }

                private String name;
                public String getName() {
                    return name;
                }

                private String samplingTime;
                public String getSamplingTime() {
                    return samplingTime;
                }

                private String phone;
                public String getPhone() {
                    return phone;
                }

                private String address;
                public String getAddress() {
                    return address;
                }

                private String tubNo;
                public String getTubNo() {
                    return tubNo;
                }
            }
        }
    }

    public static class Test{
        private static Test parse(String tag, ESONObject data){
            Test test = new Test();

            String account  = data.getJSONValue("account","");
            test.account = account;

            String password = data.getJSONValue("password","");
            test.password = password;

            return test;
        }
        private String account;
        public Test setAccount(String account){
            this.account = account;
            return this;
        }
        public String getAccount(){
            return account;
        }

        private String password;
        public Test setPassword(String password){
            this.password = password;
            return this;
        }
        public String getPassword(){
            return password;
        }
    }

    public static class Permission{
        private static Permission parse(String tag, ESONObject data){
            Permission permission = new Permission();

            permission.bAllowRememberPassword = data.getJSONValue("allow remember password",false);
            permission.bAllowAutoLogin        = data.getJSONValue("allow auto login",false);
            permission.bAllowAutoRefreshToken = data.getJSONValue("allow auto refresh token",false);

            return permission;
        }
        private boolean bAllowRememberPassword = false;
        public Permission setBAllowRememberPassword(boolean bAllowRememberPassword){
            this.bAllowRememberPassword = bAllowRememberPassword;
            return this;
        }
        public boolean getBAllowRememberPassword(){
            return bAllowRememberPassword;
        }

        private boolean bAllowAutoLogin        = false;
        public Permission setBAllowAutoLogin(boolean bAllowAutoLogin){
            this.bAllowAutoLogin = bAllowAutoLogin;
            return this;
        }
        public boolean getBAllowAutoLogin(){
            return bAllowAutoLogin;
        }

        private boolean bAllowAutoRefreshToken = false;
        public Permission setBAllowAutoRefreshToken(boolean bAllowAutoRefreshToken){
            this.bAllowAutoRefreshToken = bAllowAutoRefreshToken;
            return this;
        }
        public boolean getBAllowAutoRefreshToken(){
            return bAllowAutoRefreshToken;
        }
    }

    private Map<String,String> mCommunityInfo = new HashMap<>();
    public Map<String, String> getCommunityInfo() {
        return mCommunityInfo;
    }
}
