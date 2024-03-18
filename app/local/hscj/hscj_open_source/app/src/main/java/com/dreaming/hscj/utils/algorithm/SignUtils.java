package com.dreaming.hscj.utils.algorithm;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class SignUtils {
    public static String sign(String data, String key, String algorithm) throws Exception{
        String switchAlgorithm = algorithm.toUpperCase().trim();
        if(switchAlgorithm.startsWith("HMAC")){
            return signHMACSHA(data, key, algorithm);
        }
        if(switchAlgorithm.startsWith("MD5")){
            return signMD5(data);
        }
        if(switchAlgorithm.startsWith("SHA")){
            return signSHA(data,algorithm);
        }
        throw new NoSuchAlgorithmException();
    }

    public static String signHMACSHA(String data, String key, String algorithm) {
        String reString = "";

        try {
            byte[] bytes = key.getBytes("UTF-8");
            //根据给定的字节数组构造一个密钥,第二参数指定一个密钥算法的名称
            SecretKey secretKey = new SecretKeySpec(bytes, algorithm);
            //生成一个指定 Mac 算法 的 Mac 对象
            Mac mac = Mac.getInstance(algorithm);
            //用给定密钥初始化 Mac 对象
            mac.init(secretKey);

            byte[] text = data.getBytes("UTF-8");
            //完成 Mac 操作
            byte[] text1 = mac.doFinal(text);

            reString = Base64.getEncoder().encodeToString(text1);

        } catch (Exception e) {}

        return reString;
    }

    public static String signSHA(String string, String algorithm) {
        if (string == null || string.isEmpty()) {
            return "";
        }
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance(algorithm);
            byte[] bytes = md5.digest((string ).getBytes());
            String result = "";
            for (byte b : bytes) {
                String temp = Integer.toHexString(b & 0xff);
                if (temp.length() == 1) {
                    temp = "0" + temp;
                }
                result += temp;
            }
            return result;
        } catch (Exception e) {
        }
        return "";
    }

    public static String signMD5(String data){
        byte[] hash;
        try {
            hash = MessageDigest.getInstance("MD5").digest(data.getBytes("UTF-8"));
        } catch (Exception e) {
            return "";
        }
        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            if ((b & 0xFF) < 0x10) hex.append("0");
            hex.append(Integer.toHexString(b & 0xFF));
        }
        return hex.toString();
    }
}
