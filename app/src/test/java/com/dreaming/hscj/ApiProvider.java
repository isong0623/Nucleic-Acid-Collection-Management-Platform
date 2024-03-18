package com.dreaming.hscj;

import com.lzy.okgo.OkGo;
import com.lzy.okgo.model.HttpHeaders;
import com.lzy.okgo.model.HttpParams;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.json.JSONObject;

import java.nio.charset.Charset;
import java.security.Security;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.Response;
import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

public class ApiProvider {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    final String username;
    final String password;
    public ApiProvider(String username,String password) throws Exception{
        this.username = encryptAES(username,"TheKeyOfmyDatadx","AES/ECB/PKCS7Padding");
        this.password = encryptAES(password,"TheKeyOfmyDatadx","AES/ECB/PKCS7Padding");
    }

    private static String encryptAES(String data, String secretKey, String algorithm) throws Exception {
        //创建密码器
        Cipher cipher = Cipher.getInstance(algorithm,"BC"); //android 没有BC参数

        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(Charset.forName("utf-8")), algorithm.split("/")[0]);
        //初始化为加密密码器
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
        byte[] encryptByte = cipher.doFinal(data.getBytes(Charset.forName("utf-8")));
        // 将加密以后的数据进行 Base64 编码
        return Base64.getEncoder().encodeToString(encryptByte);
    }

    String token = "";

    public boolean login() {
        String url = "https://hsjc.qingdao.gov.cn/api/login";
        HttpParams params = new HttpParams();
        params.put("username",username);
        params.put("password",password);
        params.put("point","1");

        try {
            Response response = OkGo.<JSONObject>post  (url).params(params).execute();
            ESONObject jResponse = new ESONObject(response.body().string());
            token = jResponse.getJSONValue("data",new ESONObject()).getJSONValue("token","");
            if(token.trim().isEmpty()){
                System.out.println("login failure!!!");
                return false;
            }
            if(!token.toLowerCase().startsWith("Bearer ")) token = "Bearer "+token;
        } catch (Exception e) {
            return false;
        }
        System.out.println("login success!!!");
        return true;
    }

    private boolean autoLogin() throws Exception{
        if(token == null || token.trim().isEmpty()) {
            boolean result = login();
            if(!result) throw new Exception("账号或密码错误");
        }
        return true;
    }

    public ESONObject findUserInfoById(String idCardNo) throws Exception{
        autoLogin();
        String url = "https://hsjc.qingdao.gov.cn/api/people/findPeopleListForInput";
        ESONObject params = new ESONObject()
                .putValue("idCard",idCardNo)
                .putValue("pageNum",1)
                .putValue("pageSize",30);

        HttpHeaders headers = new HttpHeaders();
        headers.put("Authorization",token);
        Response response = OkGo.<JSONObject>post(url).headers(headers).upJson(params).execute();
        ESONObject jResponse = new ESONObject(response.body().string());
        ESONArray array = jResponse.getJSONValue("data", new ESONObject()).getJSONValue("result",new ESONArray());
        if(array.length()==0) return new ESONObject();

        return array.getArrayValue(0,new ESONObject());
    }

    public boolean deleteUserInTubNo(String ids) throws Exception{
        autoLogin();
        String url = "https://hsjc.qingdao.gov.cn/api/testResult/delTestResult";
        HttpHeaders headers = new HttpHeaders();
        headers.put("Authorization",token);
        ESONObject params = new ESONObject()
                .putValue("ids",ids)
                .putValue("reason","条码错误");
        Response response = OkGo.<JSONObject>post(url).headers(headers).upJson(params).execute();
        ESONObject jResponse = new ESONObject(response.body().string());
        return jResponse.getJSONValue("code",-1) == 0 && jResponse.getJSONValue("data",false);
    }

    public ESONArray queryHistory(String tubNo) throws Exception{
        autoLogin();
        String url = "https://hsjc.qingdao.gov.cn/api/testResult/delTestResult";
        HttpHeaders headers = new HttpHeaders();
        headers.put("Authorization",token);
        ESONObject params = new ESONObject()
                .putValue("pageNum",1)
                .putValue("pageSize",30)
                .putValue("testNum",tubNo)
                ;
        Response response = OkGo.<JSONObject>post(url).headers(headers).upJson(params).execute();
        ESONObject jResponse = new ESONObject(response.body().string());
        if(jResponse.getJSONValue("code",-1) != 0){
            throw new Exception("访问错误！");
        }
        return jResponse.getJSONValue("data",new ESONArray());
    }

    public boolean samplingUser(String tubNo, ESONObject userInfo) throws Exception{
        autoLogin();

        String url = "https://hsjc.qingdao.gov.cn/api/testNum/confirmed";

        String idType = userInfo.getJSONValue("idType"        ,"1");
        if(idType==null || idType.isEmpty() || idType.equalsIgnoreCase("null")) idType = "1";

        String isPc = userInfo.getJSONValue("isPc"          ,"0");
        if(isPc == null || isPc.equalsIgnoreCase("null") || !(isPc.equalsIgnoreCase("1")||isPc.equalsIgnoreCase("0"))) isPc = "1";

        ESONObject params = new ESONObject()
                .putValue("tubeCapacity"  ,"20")
                .putValue("fullName"      ,userInfo.getJSONValue("fullName"      ,""))
                .putValue("idCard"        ,userInfo.getJSONValue("idCard"        ,""))
                .putValue("idType"        ,idType)
                .putValue("mobile"        ,userInfo.getJSONValue("mobile"        ,""))
                .putValue("streetId"      ,"")
                .putValue("communityId"   ,"")
                .putValue("zoneId"        ,"")
                .putValue("category"      ,userInfo.getJSONValue("category"      ,""))
                .putValue("address"       ,userInfo.getJSONValue("address"       ,""))
                .putValue("remark"        ,userInfo.getJSONValue("remark"        ,""))
                .putValue("gridName"      ,userInfo.getJSONValue("gridName"      ,""))
                .putValue("secondGridName",userInfo.getJSONValue("secondGridName",""))
                .putValue("thirdGridName" ,userInfo.getJSONValue("thirdGridName" ,""))
                .putValue("testNum"       ,tubNo)
                .putValue("status"        ,null)
                .putValue("primaryId"     ,userInfo.getJSONValue("primaryId"     ,""))
                .putValue("secondaryId"   ,userInfo.getJSONValue("secondaryId"   ,""))
                .putValue("thirdId"       ,userInfo.getJSONValue("thirdId"       ,""))
                .putValue("isNew"         ,"0")
                .putValue("isPc"          ,isPc)
                ;

        HttpHeaders headers = new HttpHeaders();
        headers.put("Authorization",token);
        Response response = OkGo.<JSONObject>post(url).headers(headers).upJson(params).execute();
        ESONObject jResponse = new ESONObject(response.body().string());

        return jResponse.getJSONValue("code",-1) == 0 && jResponse.getJSONValue("data",new ESONObject()).getJSONValue("testNum","").equalsIgnoreCase(tubNo);
    }
}
