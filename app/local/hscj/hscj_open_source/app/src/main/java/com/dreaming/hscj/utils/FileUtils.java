package com.dreaming.hscj.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.charset.Charset;

public class FileUtils {

    public static String readAll(String path){
        StringBuilder sb = new StringBuilder();
        try {
            FileReader fr = new FileReader(path);
            BufferedReader bf = new BufferedReader(fr);
            String str;
            // 按行读取字符串
            while ((str = bf.readLine()) != null) {
                sb.append(str);
//                sb.append(new String(str.getBytes(), Charset.forName("utf-8")));
            }
            bf.close();
            fr.close();
        } catch (Exception e) { }

        return sb.toString();
    }

}
