package com.dreaming.hscj.utils.algorithm;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class EncryptUtils {

    public static String encrypt(String data, String key, String algorithm) throws Exception{
        String switchAlgorithm = algorithm.toUpperCase().trim();
        if(switchAlgorithm.startsWith("AES") || switchAlgorithm.startsWith("DES")){
            return encryptAES(data,key,algorithm);
        }
        if(switchAlgorithm.startsWith("BASE64")){
            return encryptBase64(data);
        }
        if(switchAlgorithm.startsWith("URL")){
            return encryptUrl(data);
        }
        throw new NoSuchAlgorithmException();
    }

    public static String encryptUrl(String data) throws Exception {
        return URLEncoder.encode(data, "utf-8");
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

   public static String encryptBase64(String data){
        return Base64.getEncoder().encodeToString(data.getBytes(Charset.forName("utf-8")));
   }
}
