package com.dreaming.hscj.utils;

import android.os.Build;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

 /**
  * 拼音操作工具类
  *
  */
 public class PinyinUtil {
     /**
      * 将字符串转换成拼音数组
      *
      * @param src
      * @return
      */
     public static String[] stringToPinyin(String src) {
         return stringToPinyin(src, false, null);
     }

     /**
      * 将字符串转换成拼音数组
      *
      * @param src
      * @return
      */
     public static String[] stringToPinyin(String src, String separator) {
         return stringToPinyin(src, true, separator);
     }

     /**
      * 将字符串转换成拼音数组
      *
      * @param src
      * @param isPolyphone 是否查出多音字的所有拼音
      * @param separator 多音字拼音之间的分隔符
      * @return
      */
     public static String[] stringToPinyin(String src, boolean isPolyphone, String separator) {
         // 判断字符串是否为空
         if ("".equals(src) || null == src) {
             return null;
         }
         char[] srcChar = src.toCharArray();
         int srcCount = srcChar.length;
         String[] srcStr = new String[srcCount];

         for (int i = 0; i < srcCount; i++) {
             srcStr[i] = charToPinyin(srcChar[i], isPolyphone, separator);
         }
         return srcStr;
     }

     /**
      * 将单个字符转换成拼音
      *
      * @param src
      * @return
      */
     public static String charToPinyin(char src, boolean isPolyphone, String separator) {
         // 创建汉语拼音处理类
         HanyuPinyinOutputFormat defaultFormat = new HanyuPinyinOutputFormat();
         // 输出设置，大小写，音标方式
         defaultFormat.setCaseType(HanyuPinyinCaseType.LOWERCASE);
         defaultFormat.setToneType(HanyuPinyinToneType.WITH_TONE_MARK);

         StringBuffer tempPinying = new StringBuffer();

         // 如果是中文
         if (src > 128) {
             try {
                 // 转换得出结果
                 String[] strs = PinyinHelper.toHanyuPinyinStringArray(src, defaultFormat);

                 // 是否查出多音字，默认是查出多音字的第一个字符
                 if (isPolyphone && null != separator) {
                     for (int i = 0; i < strs.length; i++) {
                         tempPinying.append(strs[i]);
                         if (strs.length != (i + 1)) {
                             // 多音字之间用特殊符号间隔起来
                             tempPinying.append(separator);
                         }
                     }
                 } else {
                     tempPinying.append(strs[0]);
                 }

             } catch (BadHanyuPinyinOutputFormatCombination e) {
                 e.printStackTrace();
             }
         } else {
             tempPinying.append(src);
         }

         return tempPinying.toString();

     }

     public static String hanziToPinyin(String hanzi) {
         return hanziToPinyin(hanzi, " ");
     }

     /**
      * 将汉字转换成拼音
      *
      * @param hanzi
      * @param separator
      * @return
      */
     @SuppressWarnings("deprecation")
     public static String hanziToPinyin(String hanzi, String separator) {
         // 创建汉语拼音处理类
         HanyuPinyinOutputFormat defaultFormat = new HanyuPinyinOutputFormat();
         // 输出设置，大小写，音标方式
         defaultFormat.setCaseType(HanyuPinyinCaseType.LOWERCASE);
         defaultFormat.setToneType(HanyuPinyinToneType.WITH_TONE_NUMBER);

         String pinyingStr = "";
         try {
             pinyingStr = PinyinHelper.toHanYuPinyinString(hanzi, defaultFormat, separator, false);
         } catch (BadHanyuPinyinOutputFormatCombination e) {
             e.printStackTrace();
         }
         return pinyingStr;
     }

     /**
      * 将字符串数组转换成字符串
      *
      * @param str
      * @param separator
      *            各个字符串之间的分隔符
      * @return
      */
     public static String stringArrayToString(String[] str, String separator) {
         StringBuffer sb = new StringBuffer();
         for (int i = 0; i < str.length; i++) {
             sb.append(str[i]);
             if (str.length != (i + 1)) {
                 sb.append(separator);
             }
         }
         return sb.toString();
     }

     /**
      * 简单的将各个字符数组之间连接起来
      *
      * @param str
      * @return
      */
     public static String stringArrayToString(String[] str) {
         return stringArrayToString(str, "");
     }

     /**
      * 将字符数组转换成字符串
      *
      * @param ch
      * @param separator 各个字符串之间的分隔符
      * @return
      */
     public static String charArrayToString(char[] ch, String separator) {
         StringBuffer sb = new StringBuffer();
         for (int i = 0; i < ch.length; i++) {
             sb.append(ch[i]);
             if (ch.length != (i + 1)) {
                 sb.append(separator);
             }
         }
         return sb.toString();
     }

     /**
      * 将字符数组转换成字符串
      *
      * @param ch
      * @return
      */
     public static String charArrayToString(char[] ch) {
         return charArrayToString(ch, " ");
     }

     /**
      * 取汉字的首字母
      *
      * @param src
      * @param isCapital
      *            是否是大写
      * @return
      */
     public static char[] getHeadByChar(char src, boolean isCapital) {
         // 如果不是汉字直接返回
         if (src <= 128) {
             return new char[] { src };
         }
         // 获取所有的拼音
         String[] pinyingStr = PinyinHelper.toHanyuPinyinStringArray(src);
         // 过滤中文符号
         if (pinyingStr == null) {
             return new char[] { src };
         }
         // 创建返回对象
         int polyphoneSize = pinyingStr.length;
         char[] headChars = new char[polyphoneSize];
         int i = 0;
         // 截取首字符
         for (String s : pinyingStr) {
             char headChar = s.charAt(0);
             // 首字母是否大写，默认是小写
             if (isCapital) {
                 headChars[i] = Character.toUpperCase(headChar);
             } else {
                 headChars[i] = headChar;
             }
             i++;
         }

         return headChars;
     }

     /**
      * 取汉字的首字母(默认是大写)
      *
      * @param src
      * @return
      */
     public static char[] getHeadByChar(char src) {
         return getHeadByChar(src, true);
     }

     /**
      * 查找字符串首字母
      *
      * @param src
      * @return
      */
     public static String[] getHeadByString(String src) {
         return getHeadByString(src, true);
     }

     /**
      * 查找字符串首字母
      *
      * @param src
      * @param isCapital
      *            是否大写
      * @return
      */
     public static String[] getHeadByString(String src, boolean isCapital) {
         return getHeadByString(src, isCapital, null);
     }

     /**
      * 查找字符串首字母
      *
      * @param src
      * @param isCapital
      *            是否大写
      * @param separator
      *            分隔符
      * @return
      */
     public static String[] getHeadByString(String src, boolean isCapital, String separator) {
         char[] chars = src.toCharArray();
         String[] headString = new String[chars.length];
         int i = 0;
         for (char ch : chars) {

             char[] chs = getHeadByChar(ch, isCapital);
             StringBuffer sb = new StringBuffer();
             if (null != separator) {
                 int j = 1;

                 for (char ch1 : chs) {
                     sb.append(ch1);
                     if (j != chs.length) {
                         sb.append(separator);
                     }
                     j++;
                 }
             } else {
                 sb.append(chs[0]);
             }
             headString[i] = sb.toString();
             i++;
         }
         return headString;
     }

     public static String getPinyin(String hanzi) {
         return stringArrayToString(stringToPinyin(hanzi));
     }

     public static boolean isAlphabet(char c){
        return c>='a'&&c<='z' || c>='A'&&c<='Z';
     }

     public static boolean isDigit(char c){
         return c>='0'&&c<='9';
     }

     public static boolean isLittlePause(char c){
         return c==','||c=='，';
     }

     public static boolean isLargerPause(char c, char p, char n){
         return c=='.' && !isDigit(p) && !isDigit(n) ||c=='。';
     }

     public static boolean isDot(char c, char p, char n){
         return c=='.' && isDigit(p) && isDigit(n);
     }

     /**
      * 判断是否是汉字
      * @param c
      * @return
      */
     public static boolean isChinese(char c) {
         Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
         if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                 || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                 || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                 || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                 || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT) {
             return true;
         }
         if(Build.VERSION.SDK_INT>Build.VERSION_CODES.KITKAT){
            return ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_C//jdk1.7
                     || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_D;//jdk1.7
         }
         return false;
     }

     /**
      * 判断汉字符号
      * @param c
      * @return
      */
     public static boolean isChineseSymbol(char c) {
         Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
         if (ub == Character.UnicodeBlock.GENERAL_PUNCTUATION
                 || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                 || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
                 || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_FORMS) {
             return true;
         }
         if(Build.VERSION.SDK_INT>Build.VERSION_CODES.KITKAT){
             return ub == Character.UnicodeBlock.VERTICAL_FORMS;//jdk1.7
         }
         return false;
     }
 }