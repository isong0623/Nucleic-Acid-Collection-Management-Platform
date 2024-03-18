package com.dreaming.hscj.utils;

import static com.dreaming.hscj.utils.CheckUtils.CardType.HMCard;
import static com.dreaming.hscj.utils.CheckUtils.CardType.IdCard;
import static com.dreaming.hscj.utils.CheckUtils.CardType.Officer;
import static com.dreaming.hscj.utils.CheckUtils.CardType.Passport;
import static com.dreaming.hscj.utils.CheckUtils.CardType.TWCard;

import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.template.api.impl.ApiConfig;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.regex2.Pattern;

public class CheckUtils {
    private static final String BASE_CODE_STRING = "0123456789ABCDEFGHJKLMNPQRTUWXY";
    private static final char[] BASE_CODE_ARRAY = BASE_CODE_STRING.toCharArray();
    private static final List<Character> BASE_CODES = new ArrayList<>();
    private static final String BASE_CODE_REGEX = "[" + BASE_CODE_STRING + "]{18}";
    private static final int[] WEIGHT = {1, 3, 9, 27, 19, 26, 16, 17, 20, 29, 25, 13, 8, 24, 10, 30, 28};
    static {
        for (char c : BASE_CODE_ARRAY) {
            BASE_CODES.add(c);
        }
    }
    /**
     * 是否是有效的统一社会信用代码
     *
     * @param socialCreditCode 统一社会信用代码
     * @return
     */
    public static boolean isValidSocialCreditCode(String socialCreditCode) {
        if(socialCreditCode == null) return false;
        socialCreditCode = socialCreditCode.toUpperCase().trim();
        if (socialCreditCode.isEmpty() || !Pattern.matches(BASE_CODE_REGEX, socialCreditCode)) {
            return false;
        }

        char[] businessCodeArray = socialCreditCode.toCharArray();
        char check = businessCodeArray[17];
        int sum = 0;
        for (int i = 0; i < 17; ++i) {
            char key = businessCodeArray[i];
            sum += (BASE_CODES.indexOf(key) * WEIGHT[i]);
        }
        int value = 31 - sum % 31;
        return check == BASE_CODE_ARRAY[value % 31];
    }

    //                                            1  2  3  4  5  6  7  8  9  10 11 12
    private static int dayOfMonth[] = new int[]{0,31,28,31,30,31,30,31,31,30,31,30,31};
    public static boolean isValidYYYYMMDD(String time){
        if(time==null || time.length()!=8) return false;
        try {
            int year  = Integer.valueOf(time.substring(0,4));
            if(year<1900) return false;
            if(year> Calendar.getInstance().get(Calendar.YEAR)) return false;
            int month = Integer.valueOf(time.substring(4,6));
            if(month<1 || month>12) return false;
            int day   = Integer.valueOf(time.substring(6,8));
            if(day<1 || day>31) return false;
            if(month==2){
                boolean isRun = year%4==0&&year%100!=0 || year%400==0;
                if(day> (isRun ? 29 : 28)) return false;
            }
            else if(day>dayOfMonth[month]) return false;

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public enum CardType{
        Unknown(""),
        IdCard("身份证"),
        Passport("护照"),
        Officer("军官证"),
        HMCard("港澳通行证"),
        TWCard("台湾通行证");

        public final String text;
        private CardType(String text){
            this.text = text;
        }

    }

    public static String isValidCard(ApiConfig.Locate.Card card, StringBuilder sb){
        if(sb == null) return null;

        for(int i=0,ni=card.normal.size();i<ni;++i){
            switch (card.normal.get(i)){
                case IdCard:
                    if(isValidIdCard(sb)) return IdCard.text;
                    break;
                case Passport:
                    if(isPassportCard(sb)) return Passport.text;
                    break;
                case Officer:
                    if(isOfficerCard(sb)) return Officer.text;
                    break;
                case HMCard:
                    if(isHMCard(sb)) return HMCard.text;
                    break;
                case TWCard:
                    if(isTWCard(sb)) return TWCard.text;
                    break;
            }
        }

        for(int i=0,ni=card.other.size();i<ni;++i){
            ApiConfig.Locate.Card.Config config = card.other.get(i);
            if(sb.toString().matches(config.getRegex())) return config.getName();
        }

        return null;
    }

    public static String typeOfCard(String type){
        if(type == null) return null;
        final Map<String,String> mapper = Template.getCurrentTemplate().getApiConfig().getCard().mapper;
        switch (type){
            case "身份证":
                return mapper.get("ID Card");
            case "护照":
                return mapper.get("Passport Card");
            case "军官证":
                return mapper.get("Officer Card");
            case "港澳通行证":
                return mapper.get("HM Card");
            case "台湾通行证":
                return mapper.get("TW Card");
            default:
                return mapper.get(type);
        }
    }

    private static int power[] = { 7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2 };
    private static String verifyCode[] = { "1", "0", "X", "9", "8", "7", "6", "5", "4", "3", "2" };

    /**
     * 是否是合法的身份号
     */
    public static boolean isValidIdCard(StringBuilder sb){
        if(sb == null) return false;
        if(isValidIdCard(sb.toString())){
            sb.replace(0,sb.length(),sb.toString().trim().replaceAll("[^0-9xX]*","").toUpperCase());
            return true;
        }
        return false;
    }
    public static boolean isValidIdCard(String idCardNo){
        if(idCardNo == null) return false;
        idCardNo = idCardNo.replaceAll("[^0-9xX]*","").toUpperCase();
        if(idCardNo.length()!=18) return false;

        int sum = 0;
        for(int i=0;i<18;++i){
            char ch = idCardNo.charAt(i);
            if(i<17) sum += power[i] * (ch - '0');
            if(ch>='0'&&ch<='9') continue;
            if(ch=='X'&&i==17) continue;
            return false;
        }
        if(idCardNo.charAt(17)!=verifyCode[sum%11].charAt(0)) return false;

        return true;
    }

    private static boolean isEmptyStr(String str){
        if(str == null) return true;
        if(str.trim().isEmpty()) return true;
        return false;
    }

    /** 正则表达式：验证护照 数字+字母，共9位 */
    public static final String REGEX_PASSPORT_CARD = "^([a-zA-z]|[0-9]){9}$";

    /** 正则表达式：验证军官证 汉字+8位数字 */
    public static final String REGEX_OFFICER_CARD = "^[\\u4E00-\\u9FA5](字第)([0-9a-zA-Z]{4,8})(号?)$";

    /** 正则表达式：验证港澳居民通行证 H/M + 10位或8位数字 */
    public static final String REGEX_HK_CARD = "^[HMhm]{1}([0-9]{10}|[0-9]{8})$";

    /** 正则表达式：验证台湾居民通行证 新版8位或18位数字,旧版10位数字 + 英文字母 */
    public static final String REGEX_TW_CARD = "^\\d{8}|^[a-zA-Z0-9]{10}|^\\d{18}$";


    public static boolean isPassportCard(StringBuilder sb) {
        if(sb == null) return false;
        if(isPassportCard(sb.toString())){
            sb.replace(0,sb.length(),sb.toString().replaceAll("[^a-zA-Z0-9]","").toUpperCase());
            return true;
        }

        return false;
    }
    /**
     * 校验护照
     *
     * @param passPortNo 护照号
     * @return 校验通过返回true，否则返回false
     */
    public static boolean isPassportCard(String passPortNo) {
        //校验非空
        if (isEmptyStr(passPortNo)) {
            return false;
        }
        passPortNo = passPortNo.trim().replaceAll("[^a-zA-Z0-9]","");
        return passPortNo.matches(REGEX_PASSPORT_CARD);
    }

    public static boolean isOfficerCard(StringBuilder sb) {
        if(sb == null) return false;
        if(isOfficerCard(sb.toString())){
            sb.replace(0,sb.length(),sb.toString().replaceAll("[^\\u4E00-\\u9FA5字第0-9a-zA-Z号]","").toUpperCase());
            return true;
        }
        return false;
    }
    /**
     * 校验军官证
     * 规则： 军/兵/士/文/职/广/（其他中文） + "字第" + 4到8位字母或数字 + "号"
     * 样本： 军字第2001988号, 士字第P011816X号
     * @param officerNo 军官证号
     * @return 校验通过返回true，否则返回false
     */
    public static boolean isOfficerCard(String officerNo) {
        //校验非空
        if (isEmptyStr(officerNo)) {
            return false;
        }
        officerNo = officerNo.replaceAll("[^\\u4E00-\\u9FA5字第0-9a-zA-Z号]","").toUpperCase();
        return officerNo.matches(REGEX_OFFICER_CARD);
    }

    public static boolean isHMCard(StringBuilder sb) {
        if(sb == null) return false;
        if(isHMCard(sb.toString())){
            sb.replace(0,sb.length(),sb.toString().replaceAll("[^HMhm0-9]","").toUpperCase());
            return true;
        }
        return false;
    }
    /**
     * 校验港澳通行证
     *
     * @param HMNo 港澳通行证号
     * @return 校验通过返回true，否则返回false
     */
    public static boolean isHMCard(String HMNo) {
        //校验非空
        if (isEmptyStr(HMNo)) {
            return false;
        }
        HMNo = HMNo.replaceAll("[^HMhm0-9]","").toUpperCase();
        return HMNo.matches(REGEX_HK_CARD);
    }

    public static boolean isTWCard(StringBuilder sb) {
        if(sb == null) return false;
        if(isTWCard(sb.toString())){
            sb.replace(0,sb.length(),sb.toString().replaceAll("[^0-9a-zA-Z]","").toUpperCase());
            return true;
        }

        return false;
    }
    /**
     * 校验台湾通行证
     * 规则 新版8位或18位数字,旧版10位数字 + 英文字母
     * @param TWNo 台湾通行证号
     * @return 校验通过返回true，否则返回false
     */
    public static boolean isTWCard(String TWNo) {
        //校验非空
        if (isEmptyStr(TWNo)) {
            return false;
        }
        return TWNo.matches(REGEX_TW_CARD);
    }
}
