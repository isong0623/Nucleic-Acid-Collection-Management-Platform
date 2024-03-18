package com.dreaming.hscj.utils.algorithm;

import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class DecryptUtils {

    public static String decrypt(String data, String key, String algorithm) throws Exception{
        String switchAlgorithm = algorithm.toUpperCase().trim();
        if(switchAlgorithm.startsWith("AES") || switchAlgorithm.startsWith("DES")){
            return decryptAES(data,key,algorithm);
        }
        if(switchAlgorithm.startsWith("BASE64")){
            return decryptBase64(data);
        }
        if(switchAlgorithm.startsWith("URL")){
            return decryptUrl(data);
        }
        throw new NoSuchAlgorithmException();
    }

    public static String decryptUrl(String data) throws Exception {
        return URLDecoder.decode("data", "utf-8");
    }

    public static String decryptBase64(String data){
        return new String(Base64.getDecoder().decode(data), Charset.forName("utf-8"));
    }

    public static String decryptAES(String base64Data, String secretKey, String algorithm) throws Exception {
        byte[] data = Base64.getDecoder().decode(base64Data);
        Cipher cipher = Cipher.getInstance(algorithm,"BC"); //android 没有BC参数
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(Charset.forName("utf-8")), algorithm.split("/")[0]);
        //设置为解密模式
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
        //执行解密操作
        byte[] result = cipher.doFinal(data);
        return new String(result, Charset.forName("utf-8"));
    }
}
