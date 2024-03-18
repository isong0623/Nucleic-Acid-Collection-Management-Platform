package com.dreaming.hscj;

import android.util.Log;

import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.utils.SPUtils;
import com.dreaming.hscj.utils.algorithm.DecryptUtils;
import com.dreaming.hscj.utils.algorithm.EncryptUtils;

import java.util.Iterator;

import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

public class Constants {
    private Constants(){}
    private static final Constants instance = new Constants();
    public static Constants getInstance(){
        return instance;
    }

    public static class Net{
        public static final boolean isDebug = false;
    }

    public static class User{
        private static String keyOfLoginHistory = "___login_history";
        public static ESONArray getLoginHistory(){
            synchronized (keyOfLoginHistory){
                return new ESONObject(SPUtils.withDB().getString(keyOfLoginHistory,"")).getJSONValue("data",new ESONArray());
            }
        }
        public static boolean addLoginHistory(long timestamp){
            synchronized (keyOfLoginHistory){
                ESONArray hist = getLoginHistory();
                hist.putValue(timestamp);
                if(hist.length()>10){
                    ESONArray array = new ESONArray();
                    for(int i=0+hist.length()-10,ni=hist.length();i<ni;++i){
                        array.putValue(hist.getArrayValue(i,0L));
                    }
                    hist = array;
                }
                return SPUtils.withDB().commitString(keyOfLoginHistory,new ESONObject().putValue("data",hist).toString());
            }
        }

        private static String keyOfToken = "token";
        public static String getToken(){
            return SPUtils.withDB().getString(keyOfToken,"");
        }
        public static Boolean setToken(String token){
            return SPUtils.withDB().commitString(keyOfToken,token);
        }

        private static String keyOfExpired = "expired";
        public static Long getExpired(){
            return SPUtils.withDB().getLong(keyOfExpired,0);
        }
        public static Boolean setExpired(Long expired){
            return SPUtils.withDB().commitLong(keyOfExpired,expired);
        }

        private static String keyOfRememberPassword = "RememberPassword";
        public static boolean isRememberPassword(){
            return SPUtils.withDB().getBoolean(keyOfRememberPassword,false);
        }
        public static Boolean setRememberPassword(boolean isRememberPassword){
            return SPUtils.withDB().commitBoolean(keyOfRememberPassword,isRememberPassword);
        }

        private static String keyOfAutoLogin = "AutoLogin";
        public static boolean isAutoLogin(){
            return SPUtils.withDB().getBoolean(keyOfAutoLogin,false);
        }
        public static Boolean setAutoLogin(boolean isAutoLogin){
            return SPUtils.withDB().commitBoolean(keyOfAutoLogin,isAutoLogin);
        }

        private static String secretOfAccount = "H3KZBhhVHoxySfLOMNVvNEztPatefot5";
        private static String keyOfAccount = "account";
        public static String getAccount(){
            try {
                return DecryptUtils.decryptAES(SPUtils.withDB().getString(keyOfAccount,""),secretOfAccount,"AES/ECB/PKCS7Padding");
            } catch (Exception e) {
                return "";
            }
        }
        public static Boolean setAccount(String account) {
            try {
                return SPUtils.withDB().commitString(keyOfAccount, EncryptUtils.encryptAES(account,secretOfAccount,"AES/ECB/PKCS7Padding"));
            } catch (Exception e) {
                return false;
            }
        }

        private static String secretOfPassword = "vtgXvVIvqB1FnezUgsHYiPgmAOYKjzVZ";
        private static String keyOfPassword = "password";
        public static String getPassword(){
            try {
                return DecryptUtils.decryptAES(SPUtils.withDB().getString(keyOfPassword,""),secretOfPassword,"AES/ECB/PKCS7Padding");
            } catch (Exception e) {
                return "";
            }
        }
        public static Boolean setPassword(String password){
            try {
                return SPUtils.withDB().commitString(keyOfPassword, EncryptUtils.encryptAES(password,secretOfPassword,"AES/ECB/PKCS7Padding"));
            } catch (Exception e) {
                return false;
            }
        }

        public static boolean isLogin(){
            String token = getToken();
            long expired = getExpired();
            return token!=null && token.length()>0 && expired>System.currentTimeMillis()+10000L;
        }

        private static String keyOfLastFeedbackTimestamp = "_last_fb_ts";
        public static long getLastFeedbackTimestamp(){
            return SPUtils.Sys.getLong(keyOfLastFeedbackTimestamp,0L);
        }
        public static Boolean setLastFeedbackTimestamp(long timestamp){
            return SPUtils.Sys.commitLong(keyOfLastFeedbackTimestamp,timestamp);
        }

        private static String keyOfDefaultCodeDisplayStyle = "_default_code_display_style";
        public static int getDefaultCodeDisplayStyle(int defaultValue){
            return SPUtils.Sys.getInt(keyOfDefaultCodeDisplayStyle,defaultValue);
        }
        public static Boolean setkeyOfDefaultCodeDisplayStyle(int position){
            return SPUtils.Sys.commitInt(keyOfDefaultCodeDisplayStyle,position);
        }
    }

    public static class Community{
        private static String keyOfCommunityInfo = "community_info";
        public static boolean setInfo(ESONObject info){
            synchronized (Community.class){
                ESONObject last = getInfo();
                for (Iterator<String> it = info.keys(); it.hasNext(); ) {
                    String key = it.next();
                    try { last.putValue(key,info.get(key)); } catch (Exception e) { }
                }
                return SPUtils.withDB().commitString(keyOfCommunityInfo,last.toString());
            }
        }
        
        public static ESONObject getInfo(){
            synchronized (Community.class){
                return new ESONObject(SPUtils.withDB().getString(keyOfCommunityInfo,""));
            }
        }
    }

    public static class DBConfig{
        private static String getGroup(){
            return Template.getCurrentTemplate().getDatabaseSetting().getSPUnify();
        }
        private static String keyOfSelectedDataBase = "selected_database";
        public static ESONObject getSelectedDatabase(){
            return getSelectedDatabase(getGroup());
        }
        public static ESONObject getSelectedDatabase(String group){
            int id = SPUtils.with(group).getInt(keyOfSelectedDataBase,-1);
            if(-1 == id) return new ESONObject();

            ESONArray array = getAllDatabase();
            for(int i=0,ni=array.length();i<ni;++i) {
                ESONObject item = array.getArrayValue(i, new ESONObject());
                int itemId = item.getJSONValue("id",-1);
                if(id == itemId){
                    return item;
                }
            }

            return new ESONObject();
        }

        public static boolean setSelectedDatabase(int id){
            return setSelectedDatabase(getGroup(),id);
        }
        public static boolean setSelectedDatabase(String group, int id){
            return SPUtils.with(group).commitInt(keyOfSelectedDataBase,id);
        }

        private static String keyOfAllDatabase = "all_database";
        public static ESONArray getAllDatabase(){
            return getAllDatabase(getGroup());
        }
        public static ESONArray getAllDatabase(String group){
            synchronized (DBConfig.class){
                ESONObject json = new ESONObject(SPUtils.with(group).getString(keyOfAllDatabase,""));
                return json.getJSONValue("data",new ESONArray());
            }
        }
        //0 ok 1 已存在 2 保存失败
        public static int addDatabase(String townName, String villageName){
            return addDatabase(getGroup(),townName,villageName);
        }
        public static int addDatabase(String group, String townName, String villageName){
            synchronized (DBConfig.class){
                ESONArray array = getAllDatabase(group);
                int id = 0;
                for(int i=0,ni=array.length();i<ni;++i){
                    ESONObject item = array.getArrayValue(i,new ESONObject());
                    String sTownName    = item.getJSONValue("townName"   ,"");
                    String sVillageName = item.getJSONValue("villageName","");
                    if(sTownName.equals(townName) && sVillageName.equals(villageName)){
                        return 1;
                    }
                    id = Math.max(id,item.getJSONValue("id",0));
                }
                array.putValue(new ESONObject().putValue("id",id+1).putValue("townName",townName).putValue("villageName",villageName));

                return SPUtils.with(group).commitString(keyOfAllDatabase,new ESONObject().putValue("data",array).toString()) ? 0 :2;
            }
        }
        public static boolean delDatabase(int id){
            return delDatabase(getGroup(),id);
        }
        public static boolean delDatabase(String group, int id){
            synchronized (DBConfig.class){
                ESONArray array = getAllDatabase(group);
                ESONArray result = new ESONArray();
                for(int i=0,ni=array.length();i<ni;++i) {
                    ESONObject item = array.getArrayValue(i, new ESONObject());
                    int itemId = item.getJSONValue("id",-1);
                    if(itemId == id) continue;
                    result.putValue(item);
                }
                return SPUtils.with(group).commitString(keyOfAllDatabase,new ESONObject().putValue("data",result).toString());
            }
        }

        private static final String keyOfSavingApi = "save_api_";
        public static boolean savingApi(int type, String toSave){
            return savingApi(getGroup(),type,toSave);
        }
        public static boolean savingApi(String group, int type, String toSave){
            return SPUtils.with(group).commitString(keyOfSavingApi+type,toSave);
        }
        public static ESONObject getApiSaving(int type){
            return getApiSaving(getGroup(),type);
        }
        public static ESONObject getApiSaving(String group, int type){
            return new ESONObject(SPUtils.with(group).getString(keyOfSavingApi+type,""));
        }
    }

    public static class Config{
        private static final String keyOfZxingCameraIndex = "__zxing_camera_index";

        public static int getZxingCameraIndex() {
            return SPUtils.Sys.getInt(keyOfZxingCameraIndex,0);
        }
        public static boolean setZxingCameraIndex(int index){
            return SPUtils.Sys.commitInt(keyOfZxingCameraIndex,index);
        }

        private static final String keyOfSavingInputConfig = "__saving_input_config";
        public static boolean setInputConfig(ESONObject data){
            return SPUtils.withDB().commitString(keyOfSavingInputConfig,data.toString());
        }
        public static ESONObject getInputConfig(){
            return new ESONObject(SPUtils.withDB().getString(keyOfSavingInputConfig,""));
        }

        private static final String keyOfSavingBatchInputExcelFieldConfig = "saving_batch_input_config";
        public static boolean setBatchInputExcelFieldConfig(ESONObject data){
            return SPUtils.withDB().commitString(keyOfSavingBatchInputExcelFieldConfig,data.toString());
        }
        public static ESONObject getBatchInputExcelFieldConfig(){
            return new ESONObject(SPUtils.withDB().getString(keyOfSavingBatchInputExcelFieldConfig,""));
        }

        private static final String keyOfBatchInputMode = "saving_batch_input_mode";
        public static boolean setBatchInputMode(int mode){
            return SPUtils.withDB().commitInt(keyOfBatchInputMode,mode);
        }
        public static int getBatchInputMode(){
            return SPUtils.withDB().getInt(keyOfBatchInputMode,0);
        }

        private static final String keyOfBatchInputPath = "saving_batch_input_path";
        public static boolean setBatchInputPathIndex(int config){
            return SPUtils.withDB().commitInt(keyOfBatchInputPath,config);
        }
        public static int getBatchInputPathIndex(){
            return SPUtils.withDB().getInt(keyOfBatchInputPath,0);
        }

        private static final String keyOfSavingUserDetermineConfig = "saving_user_determine";
        public static boolean setBatchInputSavingUserDetermineConfig(ESONObject data){
            return SPUtils.withDB().commitString(keyOfSavingUserDetermineConfig,data.toString());
        }
        public static ESONObject getBatchInputSavingUserDetermineConfig(){
            return new ESONObject(SPUtils.withDB().getString(keyOfSavingUserDetermineConfig,""));
        }

        private static final String keyOfUserQueryMode = "user_query_mode";
        public static boolean setMemberQueryMode(int mode){
            return SPUtils.withDB().commitInt(keyOfUserQueryMode,mode);
        }
        public static int getMemberQueryMode(){
            return SPUtils.withDB().getInt(keyOfUserQueryMode,0);
        }

        private static final String keyOfUserQueryGroup= "user_query_group";
        public static boolean setMemberQueryGroup(int mode){
            return SPUtils.withDB().commitInt(keyOfUserQueryGroup,mode);
        }
        public static int getMemberQueryGroup(){
            return SPUtils.withDB().getInt(keyOfUserQueryGroup,0);
        }

        private static final String keyOfSearchingFieldsConfig = "searching_fields_config";
        public static boolean setSearchingFieldsConfig(ESONObject data){
            return SPUtils.withDB().commitString(keyOfSearchingFieldsConfig,data.toString());
        }
        public static ESONObject getSearchingFieldsConfig(){
            return new ESONObject(SPUtils.withDB().getString(keyOfSearchingFieldsConfig,""));
        }

        private static final String keyOfRecoveryDbConfig = "_key_for_rec_db_config";
        public static ESONObject getRecoveryDbConfig(){
            return new ESONObject(SPUtils.Sys.getString(keyOfRecoveryDbConfig,""));
        }
        public static boolean setRecoveryDbConfig(ESONObject config){
            return SPUtils.Sys.commitString(keyOfRecoveryDbConfig,config.toString());
        }

        private static final String keyOfRecoverySaveMemberConfig = "_key_for_recovery_save_member_config";
        public static ESONObject getRecoverySaveMemberConfig(){
            return new ESONObject(SPUtils.Sys.getString(keyOfRecoverySaveMemberConfig,""));
        }
        public static boolean setRecoverySaveMemberConfig(ESONObject config){
            return SPUtils.Sys.commitString(keyOfRecoverySaveMemberConfig,config.toString());
        }

        private static final String keyOfRecoverySaveGroupConfig = "_key_for_recovery_save_group_config";
        public static ESONObject getRecoverySaveGroupConfig(){
            return new ESONObject(SPUtils.Sys.getString(keyOfRecoverySaveGroupConfig,""));
        }
        public static boolean setRecoverySaveGroupConfig(ESONObject config){
            return SPUtils.Sys.commitString(keyOfRecoverySaveGroupConfig,config.toString());
        }

        private static final String keyOfBatchGroupingConfig = "_key_for_batch_grouping_config";
        public static ESONObject getBatchGroupingConfig(){
            return new ESONObject(SPUtils.Sys.getString(keyOfBatchGroupingConfig,""));
        }
        public static boolean setBatchGroupingConfig(ESONObject config){
            return SPUtils.Sys.commitString(keyOfBatchGroupingConfig,config.toString());
        }

        private static final String keyOfSavingBatchGroupingExcelFieldConfig = "saving_batch_grouping_config";
        public static boolean setBatchGroupingExcelFieldConfig(ESONObject data){
            return SPUtils.withDB().commitString(keyOfSavingBatchGroupingExcelFieldConfig,data.toString());
        }
        public static ESONObject getBatchGroupingExcelFieldConfig(){
            return new ESONObject(SPUtils.withDB().getString(keyOfSavingBatchGroupingExcelFieldConfig,""));
        }
    }

    public static class TemplateConfig{
        private static final  String TAG = TemplateConfig.class.getSimpleName();

        private static final String sKeyOfCurrentTemplate = "current_template";
        public static boolean setCurrentTemplate(ESONObject template){

            int id = template.getJSONValue("id",-1);
            String regionCode   = template.getJSONValue("region code","");
            String unify        = template.getJSONValue("unify","");

            Log.e(TAG,"setCurrentTemplate->"+template.toString());

            if(id == -1){
                ESONArray array = getTemplateList();
                for(int i=0,ni=array.length();i<ni;++i) {
                    ESONObject item = array.getArrayValue(i, new ESONObject());
                    String itemRegionCode  = item.getJSONValue("region code","");
                    String itemUnify       = item.getJSONValue("unify","");

                    if(itemUnify.equalsIgnoreCase(unify) && itemRegionCode.equals(regionCode)){
                        return SPUtils.Sys.commitString(sKeyOfCurrentTemplate,item.toString());
                    }
                }

                int result = addTemplate(template);
                Log.e(TAG,"addTemplate:"+result);
                return result == 0 ? setCurrentTemplate(template) : false;
            }

            return SPUtils.Sys.commitString(sKeyOfCurrentTemplate,template.toString());
        }
        public static ESONObject getCurrentTemplate(){
            return new ESONObject(SPUtils.Sys.getString(sKeyOfCurrentTemplate,""));
        }

        private static final String sKeyOfTemplateList = "template_list";
        //0成功 1配置已存在 2保存失败 3降级保存失败
        public static synchronized int addTemplate(ESONObject templateConfig){
            Long   lSrcCrc32    = templateConfig.getJSONValue("crc32",0L);
            String regionCode   = templateConfig.getJSONValue("region code","");
            long   lVersion     = templateConfig.getJSONValue("version",0L);
            String unify        = templateConfig.getJSONValue("unify","");


            ESONArray eLocal = getTemplateList();
            int id = 0;
            for(int i=0,ni=eLocal.length();i<ni;++i){
                ESONObject item = eLocal.getArrayValue(i,new ESONObject());


                Long   itemSrcCrc32    = item.getJSONValue("crc32",0L);
                int    itemLocalId     = item.getJSONValue("id",0);
                String itemRegionCode  = item.getJSONValue("region code","");
                long   itemVersion     = item.getJSONValue("version",0L);
                String itemUnify       = item.getJSONValue("unify","");


                id = Math.max(id,itemLocalId);

                if(itemSrcCrc32 == lSrcCrc32 && itemRegionCode.equals(regionCode)) return 1;

                if(itemUnify.equalsIgnoreCase(unify) && itemRegionCode.equals(regionCode)){
                    if(itemVersion > lVersion) return 3;

                    ESONArray eNew = new ESONArray();
                    for(int j=0;j<ni;++j){
                        if(j == i){
                            templateConfig.putValue("id",itemLocalId);
                            eNew.putValue(templateConfig);
                        }
                        else{
                            eNew.putValue(eLocal.getArrayValue(j,new ESONObject()));
                        }
                    }
                    return SPUtils.Sys.commitString(sKeyOfTemplateList,new ESONObject().putValue("data",eNew).toString()) ? 0 : 2;
                }
            }

            templateConfig.putValue("id",id+1);

            eLocal.putValue(templateConfig);

            return SPUtils.Sys.commitString(sKeyOfTemplateList,new ESONObject().putValue("data",eLocal).toString()) ? 0 : 2;
        }

        public static ESONArray getTemplateList(){
            return new ESONObject(SPUtils.Sys.getString(sKeyOfTemplateList,"")).getJSONValue("data",new ESONArray());
        }

        public static synchronized boolean delTemplateList(ESONObject template){
            Long   lSrcCrc32    = template.getJSONValue("crc32",0L);
            String regionCode   = template.getJSONValue("region code","");
            int    iLocalId     = template.getJSONValue("id",0);

            ESONArray eLocal = getTemplateList();
            ESONArray eSaving = new ESONArray();

            for(int i=0,ni=eLocal.length();i<ni;++i) {
                ESONObject item = eLocal.getArrayValue(i, new ESONObject());

                String itemCurrentPath = item.getJSONValue("path", "");
                Long itemSrcCrc32 = item.getJSONValue("crc32", 0L);
                int itemLocalId = item.getJSONValue("id", 0);
                String itemRegionCode = item.getJSONValue("region code", "");
                String itemRegionName = item.getJSONValue("region name", "");

                if(iLocalId == itemLocalId || lSrcCrc32==itemSrcCrc32&&regionCode.equals(itemRegionCode)){
                    continue;
                }
                eSaving.putValue(item);
            }

            return SPUtils.Sys.commitString(sKeyOfTemplateList,new ESONObject().putValue("data",eSaving).toString());
        }




        private static String keyOfVerifyPasswordHistory = "___template_password_history";
        public static ESONArray getVerifyHistory(){
            synchronized (keyOfVerifyPasswordHistory){
                return new ESONObject(SPUtils.withDB().getString(keyOfVerifyPasswordHistory,"")).getJSONValue("data",new ESONArray());
            }
        }
        public static boolean addVerifyHistory(long timestamp){
            synchronized (keyOfVerifyPasswordHistory){
                ESONArray hist = getVerifyHistory();
                hist.putValue(timestamp);
                if(hist.length()>10){
                    ESONArray array = new ESONArray();
                    for(int i=0+hist.length()-10,ni=hist.length();i<ni;++i){
                        array.putValue(hist.getArrayValue(i,0L));
                    }
                    hist = array;
                }
                return SPUtils.withDB().commitString(keyOfVerifyPasswordHistory,new ESONObject().putValue("data",hist).toString());
            }
        }

        private static String keyOfTemplateVerify = "__template_verify_tag";
        public static boolean isTemplateVerify(){
            return SPUtils.withDB().getBoolean(keyOfTemplateVerify,false);
        }
        public static boolean setTemplateVerify(boolean isTemplateVerify){
            return SPUtils.withDB().commitBoolean(keyOfTemplateVerify,isTemplateVerify);
        }
    }
}
