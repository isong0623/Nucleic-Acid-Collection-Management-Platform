package com.dreaming.hscj.template.api.wrapper;

import com.dreaming.hscj.utils.algorithm.DecryptUtils;
import com.dreaming.hscj.utils.algorithm.EncryptUtils;
import com.dreaming.hscj.utils.algorithm.SignUtils;

import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;

import priv.songxusheng.easyjson.ESONArray;

public class ApiAlgorithm {
    public static ApiAlgorithm parse(String tag, ESONArray data) throws Exception {
//        int type, int index1, int index2
//        String tag = String.format("Api[%d][%s][%d][%d]",type,"\"algorithms\"",index1,index2);
        if(data.length() < 3) throw new InvalidParameterException(tag+"数组成员个数应为3个或5个！\n");

        ApiAlgorithm item = new ApiAlgorithm();

        item.tag = tag;

        String _type = data.getArrayValue(0,"").toUpperCase().trim();
        item.setType(_type);

        String secret = data.getArrayValue(1,"");
        item.setSecret(secret);

        String algorithm = data.getArrayValue(2,"");
        item.setAlgorithm(algorithm);

        if(data.length() == 5){
            String test     = data.getArrayValue(3,"");
            String result   = item.convert(test);
            String expected = data.getArrayValue(4,"");
            if(!expected.equals(result)){
                throw new Exception(tag+"algorithm测试未通过，样例["+test+"]转换结果为["+result+"]与期望值["+expected+"]不匹配！");
            }
        }

        return item;
    }

    private ApiAlgorithm(){}
    private String tag;

    //算法类型 eg:ENCRYPT
    private String type;
    public String getType(){
        return type;
    }
    public ApiAlgorithm setType(String type){
        this.type = type;
        return this;
    }

    //秘钥
    private String secret;
    public String getSecret(){
        return secret;
    }
    public ApiAlgorithm setSecret(String secret){
        this.secret = secret;
        return this;
    }

    //组合的加密算法 eg:["BASE64","URL"]
    private String algorithm;
    public String getAlgorithm(){
        return algorithm;
    }
    public ApiAlgorithm setAlgorithm(String algorithm){
        this.algorithm = algorithm;
        return this;
    }

    public String convert(String data) throws Exception{
        try {
            switch (getType()){
                case "ENCRYPT":
                    return EncryptUtils.encrypt(data,secret,algorithm);
                case "DECRYPT":
                    return DecryptUtils.decrypt(data,secret,algorithm);
                case "SIGN":
                    return SignUtils   .sign   (data,secret,algorithm);
            }
        } catch (Exception e) {
            e.printStackTrace();
            //windows平台与android平台支持的算法不一致，具体以android为准
            throw new Exception(tag + e.getMessage());
        }
        throw new NoSuchAlgorithmException(tag+"仅支持[\"ENCRYPT\",\"DECRYPT\",\"SIGN\"]三种数据变换算法类型");
    }

}
