package com.dreaming.hscj.template.api.impl;

import com.dreaming.hscj.template.api.wrapper.ApiResponse;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.dreaming.hscj.utils.CheckUtils;
import com.dreaming.hscj.utils.JsonUtils;
import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

public class ApiConfig {

    static Logger logger = Logger.getLogger(ApiConfig.class.getName());

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

    public static ApiConfig parse(ESONObject data) throws Exception {
        ApiConfig apiConfig = new ApiConfig();

        apiConfig.authorization = Locate.Authorization.parse(
                "ApiConfig[\"locate\"][\"authorization\"]",
                data.getJSONValue("locate",new ESONObject())
                        .getJSONValue("authorization",new ESONObject())
        );
        logger.info("init Authorization success!");

        apiConfig.scan          = Locate.Scan.parse(
                "ApiConfig[\"locate\"][\"scan\"]",
                data.getJSONValue("locate",new ESONObject())
                        .getJSONValue("scan",new ESONObject())
        );
        logger.info("init Scan success!");

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

        apiConfig.test          = Test.parse(
                "ApiConfig[\"test\"]",
                data.getJSONValue("test",new ESONObject())
        );
        logger.info("init Test success!");

        apiConfig.permission    = Permission.parse(
                "ApiConfig[\"permission\"]",
                data.getJSONValue("permission",new ESONObject())
        );
        logger.info("init Permission success!");

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
                for(int i=0,ni=normal.size();i<ni;++i){
                    String item = normal.get(i);
                    if("ID Card".equalsIgnoreCase(item.trim())){
                        support.add(CheckUtils.CardType.IdCard);
                        continue;
                    }
                    if("Passport Card".equalsIgnoreCase(item.trim())){
                        support.add(CheckUtils.CardType.Passport);
                        continue;
                    }
                    if("HM Card".equalsIgnoreCase(item.trim())){
                        support.add(CheckUtils.CardType.HMCard);
                        continue;
                    }
                    if("TW Card".equalsIgnoreCase(item.trim())){
                        support.add(CheckUtils.CardType.TWCard);
                        continue;
                    }
                    if("Officer Card".equalsIgnoreCase(item.trim())){
                        support.add(CheckUtils.CardType.Officer);
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

                    config.name = name;
                    config.regex = regex;
                    return config;
                }

                private Config(){}
                private String name;
                private String regex;
                public String getName() {
                    return name;
                }
                public String getRegex() {
                    return regex;
                }
            }

            public List<CheckUtils.CardType> normal;
            public List<Config> other;
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
            private List<String> id;
            public List<String> getId() {
                return id;
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
