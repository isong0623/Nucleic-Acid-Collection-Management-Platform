package com.dreaming.hscj.template.database.wrapper;

import com.dreaming.hscj.App;
import com.dreaming.hscj.Constants;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.template.database.DatabaseTemplate;
import com.dreaming.hscj.utils.CheckUtils;

import java.io.File;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

public class DatabaseSetting {
    public static DatabaseSetting parse(ESONObject data){
        DatabaseSetting setting = new DatabaseSetting();

        setting.groupMemberNum = data.getJSONValue("group member num",10);
        if(setting.groupMemberNum<2){
            throw new InvalidParameterException("DBSetting[\"group member num\"]分组人员数没有意义！");
        }

        setting.regionName = data.getJSONValue("region name","").trim();
        if(setting.regionName.isEmpty()){
            throw new InvalidParameterException("DBSetting[\"region name\"]不能为空！");
        }

        setting.enableEnterPassword = data.getJSONValue("enable enter password",true);

        setting.password = data.getJSONValue("password","");
        if(setting.enableEnterPassword && setting.password.isEmpty()){
            throw new InvalidParameterException("DBSetting[\"password\"]不能为空！");
        }

        setting.regionCode = data.getJSONValue("region code","").trim();
        if(setting.regionCode.isEmpty() || setting.regionCode.replaceAll("[1-9]([0-9]{5})","").length()!=0 ){
            throw new InvalidParameterException("DBSetting[\"region code\"]区域码为六位数！");
        }

        setting.netApiProvider = data.getJSONValue("net api provider","").trim();
        if(setting.netApiProvider.isEmpty()){
            throw new InvalidParameterException("DBSetting[\"net api provider\"]网络提供方不能为空！");
        }

        setting.unifySocialCreditCodes = data.getJSONValue("unify social credit codes","").trim();
        if(setting.unifySocialCreditCodes.isEmpty()){
            throw new InvalidParameterException("DBSetting[\"unify social credit codes\"]网络提供方社会信用代码不能为空！");
        }
        if(!CheckUtils.isValidSocialCreditCode(setting.unifySocialCreditCodes)){
            throw new InvalidParameterException("DBSetting[\"unify social credit codes\"]网络提供方社会信用代码校验失败！");
        }

        setting.netApiVersion = data.getJSONValue("net api version",0L);

        setting.netApiIntroduce = new ArrayList<>();
        ESONArray array = data.getJSONValue("net api introduce",new ESONArray());
        for(int i=0,ni=array.length();i<ni;++i){
            setting.netApiIntroduce.add(array.getArrayValue(i,""));
        }

        return setting;
    }

    private DatabaseSetting(){}

    int groupMemberNum;
    public int getGroupMemberNum() {
        return groupMemberNum;
    }

    String regionName;
    public String getRegionName() {
        return regionName;
    }

    String regionCode;
    public String getRegionCode() {
        return regionCode;
    }

    String password;
    public String getPassword(){
        return password;
    }

    boolean enableEnterPassword;
    public boolean isEnableEnterPassword(){
        return enableEnterPassword;
    }

    String netApiProvider;
    public String getNetApiProvider() {
        return netApiProvider;
    }

    String unifySocialCreditCodes;
    public String getUnifySocialCreditCodes(){
        return unifySocialCreditCodes;
    }

    public String getSPUnify(){
        return regionCode + "_" + unifySocialCreditCodes;
    }

    long netApiVersion;
    public long getNetApiVersion(){
        return netApiVersion;
    }

    List<String> netApiIntroduce;
    public List<String> getNetApiIntroduce() {
        return netApiIntroduce;
    }

}
