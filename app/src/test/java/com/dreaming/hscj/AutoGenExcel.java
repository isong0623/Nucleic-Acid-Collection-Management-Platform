package com.dreaming.hscj;

import com.dreaming.hscj.template.api.ApiProvider;
import com.dreaming.hscj.utils.ExcelUtils;
import com.dreaming.hscj.utils.FileUtils;
import com.dreaming.hscj.utils.algorithm.EncryptUtils;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.model.HttpHeaders;
import com.lzy.okgo.model.HttpParams;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.json.JSONObject;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.security.Security;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.Response;
import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

public class AutoGenExcel {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static String encryptAES(String data, String secretKey, String algorithm) throws Exception {
        //创建密码器
        Cipher cipher = Cipher.getInstance(algorithm,"BC"); //android 没有BC参数

        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(Charset.forName("utf-8")), algorithm.split("/")[0]);
        //初始化为加密密码器
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
        byte[] encryptByte = cipher.doFinal(data.getBytes(Charset.forName("utf-8")));
        // 将加密以后的数据进行 Base64 编码
        return Base64.getEncoder().encodeToString(encryptByte);
    }

    String token;
    @Test
    public void doCollection() throws Exception{
        String username = "tjcyd";
        username = encryptAES(username,"TheKeyOfmyDatadx","AES/ECB/PKCS7Padding");

        String password = "Qwer@1324";
        password = encryptAES(password,"TheKeyOfmyDatadx","AES/ECB/PKCS7Padding");

        String url = "https://hsjc.qingdao.gov.cn/api/login";
        HttpParams params = new HttpParams();
        params.put("username",username);
        params.put("password",password);
        params.put("point","1");

        Response response = OkGo.<JSONObject>post  (url).params(params).execute();
        ESONObject jResponse = new ESONObject(response.body().string());
        token = jResponse.getJSONValue("data",new ESONObject()).getJSONValue("token","");
        if(token.trim().isEmpty()){
            System.out.println("login failure!!!");
            return;
        }
        if(!token.toLowerCase().startsWith("Bearer ")) token = "Bearer "+token;

        reqRecord(0);
        int count = 0;
        for (Map.Entry<String, Integer> entry : mTubCounter.entrySet()) {
            writer.write(2,count,0,entry.getKey());
            writer.write(2,count,1,entry.getValue().toString());
            ++count;
        }
        writer.save(new FileOutputStream(new File("C:\\Users\\Isidore\\Desktop", new SimpleDateFormat("yyyy_MM_dd__HH_mm_ss").format(new Date())+".xlsx")));
        System.out.println("all done!");
    }

    public void reqRecord(int page) throws Exception{
        String param = "{\"pageNum\":"+page+",\"pageSize\":30,\"startTime\":\""+new SimpleDateFormat("yyyy-MM-dd").format(new Date())+" 00:00:00\"}";
        String url = "https://hsjc.qingdao.gov.cn/api/testResult/findTestResult";
        HttpHeaders headers = new HttpHeaders();
        headers.put("Authorization",token);
        Response response = OkGo.<JSONObject>post  (url).headers(headers).upJson(new ESONObject(param)).execute();
        if(outJSON(response.body().string())){
            reqRecord(page+1);
            Thread.sleep(500);
        }
    }

    ExcelUtils.Writer writer = new ExcelUtils.Writer();
    Map<String,Integer> mTubCounter = new HashMap<>();
    private boolean outJSON(String s) throws Exception{
        ESONObject response = new ESONObject(s);
        ESONObject data = response.getJSONValue("data",new ESONObject());
        int group = data.getJSONValue("pageNum",1);
        ESONArray results = data.getJSONValue("result",new ESONArray());

        for(int i=0,ni=results.length();i<ni;++i){
            ESONObject result = results.getArrayValue(i,new ESONObject());
            String fullName = result.getJSONValue("fullName"     ,"");
            String idCardId = result.getJSONValue("idCard"       ,"");
            String time     = result.getJSONValue("gatheringTime","");
            String testNo   = result.getJSONValue("testNum"      ,"");
            String mobile   = result.getJSONValue("mobile"       ,"");
            String id       = result.getJSONValue("id"           ,"");
            writer.write(0,i+1 +(group-1)*30,0,String.valueOf(i+1 +(group-1)*30));
            writer.write(0,i+1 +(group-1)*30,1,fullName);
            writer.write(0,i+1 +(group-1)*30,2,idCardId);
            writer.write(0,i+1 +(group-1)*30,3,time);
            writer.write(0,i+1 +(group-1)*30,4,testNo);
            writer.write(0,i+1 +(group-1)*30,5,mobile);
            writer.write(0,i+1 +(group-1)*30,6,id);
            System.out.println(String.format("%03d %6s %s %s",i+1 +(group-1)*30,fullName,idCardId,time));

            Integer counter = mTubCounter.get(testNo.trim());
            if(counter == null) counter = 0;
            ++counter;
            mTubCounter.put(testNo.trim(),counter);
        }

        return results.length() == 30;
    }

}
