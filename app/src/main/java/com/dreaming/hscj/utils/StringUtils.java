package com.dreaming.hscj.utils;

public class StringUtils {
    public static boolean isMatch(String text,String regex){
        return java.util.regex2.Pattern.compile(regex).matcher(text).replaceAll("").length() == 0;
    }
    public static String replaceAll(String text, String regex, String replacement){
        return java.util.regex2.Pattern.compile(regex).matcher(text).replaceAll(replacement);
    }
}
