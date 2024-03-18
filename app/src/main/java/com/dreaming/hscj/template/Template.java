package com.dreaming.hscj.template;

import android.util.Log;

import com.dreaming.hscj.App;
import com.dreaming.hscj.Constants;
import com.dreaming.hscj.base.ToastDialog;
import com.dreaming.hscj.template.api.ApiTemplate;
import com.dreaming.hscj.template.api.impl.Api;
import com.dreaming.hscj.template.api.impl.ApiConfig;
import com.dreaming.hscj.template.api.wrapper.ApiParam;
import com.dreaming.hscj.template.database.DatabaseTemplate;
import com.dreaming.hscj.template.database.impl.DaySamplingLogDatabase;
import com.dreaming.hscj.template.database.impl.NoneGroupingDatabase;
import com.dreaming.hscj.template.database.impl.NucleicAcidGroupingDatabase;
import com.dreaming.hscj.template.database.impl.UserInputGuideDatabase;
import com.dreaming.hscj.template.database.impl.UserOverallDatabase;
import com.dreaming.hscj.template.database.wrapper.DatabaseSetting;
import com.dreaming.hscj.utils.FileUtils;
import com.dreaming.security.Defender;

import org.apache.poi.ss.formula.functions.T;

import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.List;
import java.util.zip.CRC32;

import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

public class Template {
    private static final String TAG = Template.class.getSimpleName();

    private static boolean bIsInited = false;
    private static Template defaultTemplate;
    public static Template getDefault(){
        return defaultTemplate;
    }
    public static synchronized void init(String path) throws Exception{
        if(bIsInited) return;
        bIsInited = true;
        defaultTemplate = read(path);
        Template template = readByUnify(defaultTemplate.getDatabaseSetting().getRegionCode(),defaultTemplate.getDatabaseSetting().getUnifySocialCreditCodes(),defaultTemplate.getDatabaseSetting().getNetApiVersion());
        if(template!=null) defaultTemplate = template;
        defaultTemplate.setCurrentTemplate();
    }

    public static Template readByUnify(String regionCode, String unifySocialCreditCodes, long minVersion){
        if(regionCode == null) return null;
        if(unifySocialCreditCodes==null) return null;

        regionCode = regionCode.trim();
        unifySocialCreditCodes = unifySocialCreditCodes.trim().toLowerCase();

        String dir = String.format(
                "%s%stemplate%s",
                App.sInstance.getFilesDir().getParentFile().getAbsolutePath(),
                File.separator,
                File.separator
        );
        File fDir = new File(dir);
        if(!fDir.exists()) return null;
        if(!fDir.isDirectory()) return null;
        File fChilds[] = fDir.listFiles();
        if(fChilds == null || fChilds.length<1) return null;

        long maxVersion = 0L;
        String prefix = String.format("%s_%s_",regionCode,unifySocialCreditCodes.toUpperCase());
        String suffix = ".template";
        File fTarget = null;
        for(File f:fChilds){
            String name = f.getName();
            if(!name.endsWith(suffix)) continue;
            if(!name.startsWith(prefix)) continue;
            try {
                long version = Long.parseLong(name.replace(prefix,"").replace(suffix,""));
                if(version>maxVersion){
                    maxVersion = version;
                    fTarget = f;
                }
            } catch (Exception e) { }
        }

        if(fTarget == null) return null;

        if(maxVersion<=minVersion) return null;

        try {
            return read(fTarget.getAbsolutePath());
        } catch (Exception e) {
            return null;
        }
    }

    public static Template read(String path) throws Exception{
        String srcData = FileUtils.readAll(path);
        if(srcData == null || srcData.trim().isEmpty()) throw new InvalidParameterException("不是有效的模板文件！");
        String ecyData = new Defender().decrypt(srcData);
        if(ecyData==null || ecyData.trim().isEmpty()) throw new InvalidParameterException("无法解析的模板文件！");
        Log.e(TAG,"read length:"+ecyData.length());
        ESONObject eData = new ESONObject(ecyData);

        Template template = new Template();

        CRC32 crc32 = new CRC32();
        crc32.update(srcData.getBytes("utf-8"));
        template.lTempCode = crc32.getValue();
        template.path = path;

        Log.e(TAG,"Starting read!");
        template.apiTemplate      = ApiTemplate     .read(eData.getJSONValue("ApiTemplate"     ,new ESONObject()));
        Log.e(TAG,"read ApiTemplate success!");
        template.databaseTemplate = DatabaseTemplate.read(eData.getJSONValue("DatabaseTemplate",new ESONObject()),template.apiTemplate);
        Log.e(TAG,"read DatabaseTemplate success!");
        Log.e(TAG,"read finished!");
        return template;
    }

    private static Template currentTemplate;
    public static Template getCurrentTemplate(){
        return currentTemplate;
    }

    private ApiTemplate apiTemplate;

    private DatabaseTemplate databaseTemplate;

    private long lTempCode;
    public long getTempCode(){
        return lTempCode;
    }
    private String path;
    public String getPath(){
        return path;
    }

    public Api apiOf(int type){
        return apiTemplate.getApi(type);
    }

    public ApiConfig getApiConfig(){
        return apiTemplate.getApiConfig();
    }

    public ApiParam getUniqueField(){
        return getUserInputGuideDatabase().getConfig().getFields().get(0);
    }

    public DatabaseTemplate getDatabaseTemplate(){
        return databaseTemplate;
    }

    public DaySamplingLogDatabase getDaySamplingLogDatabase(){
        return databaseTemplate.getInstanceOfDaySamplingLogDatabase();
    }

    public NoneGroupingDatabase getNoneGroupingDatabase(){
        return databaseTemplate.getInstanceOfNoneGroupingDatabase();
    }

    public NucleicAcidGroupingDatabase getNucleicAcidGroupingDatabase(){
        return databaseTemplate.getInstanceOfNucleicAcidGroupingDatabase();
    }

    public UserInputGuideDatabase getUserInputGuideDatabase(){
        return databaseTemplate.getInstanceOfUserInputGuideDatabase();
    }

    public UserOverallDatabase getUserOverallDatabase(){
        return databaseTemplate.getInstanceOfUserOverallDatabase();
    }

    private String phone = "";
    public String getPhoneFieldName(){

        if(phone == null || phone.isEmpty()){
            phone = getApiConfig().getPhone().getPhone().get(0);
        }

        return phone;
    }

    public String getIdCardNoFieldName(){
        return getUserOverallDatabase().getIdFieldName();
    }

    public String getIdCardNameFieldName(){
        return getUserOverallDatabase().getNameFiledName();
    }

    public String getGroupIdFieldName(){
        return getNucleicAcidGroupingDatabase().getGroupIdFieldName();
    }

    public DatabaseSetting getDatabaseSetting(){
        return databaseTemplate.getDatabaseSetting();
    }

    public Template setCurrentTemplate() throws IOException {
        currentTemplate = this;

        String dstPath = String.format("%s%stemplate%s%s_%s_%d.template",
                App.sInstance.getFilesDir().getParentFile().getAbsolutePath(),
                File.separator,
                File.separator,
                databaseTemplate.getDatabaseSetting().getRegionCode(),
                databaseTemplate.getDatabaseSetting().getUnifySocialCreditCodes().toLowerCase(),
                databaseTemplate.getDatabaseSetting().getNetApiVersion()
            );

        ESONObject e = new ESONObject();
        if(!path.endsWith("template.dump")&&!path.equals(dstPath)){
            FileUtils.copy(new File(path),new File(dstPath));
            e.putValue("path",dstPath);
        }

        e
        .putValue("crc32",lTempCode)
        .putValue("region code",databaseTemplate.getDatabaseSetting().getRegionCode())
        .putValue("region name",databaseTemplate.getDatabaseSetting().getRegionName())
        .putValue("provider",databaseTemplate.getDatabaseSetting().getNetApiProvider())
        .putValue("version",databaseTemplate.getDatabaseSetting().getNetApiVersion())
        .putValue("unity",databaseTemplate.getDatabaseSetting().getUnifySocialCreditCodes());

        Constants.TemplateConfig.addTemplate(e);
        Constants.TemplateConfig.setCurrentTemplate(e);
        return this;
    }

    public Template setUseTempDbPath(boolean b, String townName, String villageName){
        databaseTemplate.setUseTempDbPath(b?lTempCode:0L, getDatabaseSetting().getRegionName(), townName, villageName);
        return this;
    }

}
