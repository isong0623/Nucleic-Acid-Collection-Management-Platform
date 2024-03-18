package com.dreaming.hscj.template;

import com.dreaming.hscj.template.api.ApiTemplate;
import com.dreaming.hscj.template.api.impl.Api;
import com.dreaming.hscj.template.api.impl.ApiConfig;
import com.dreaming.hscj.template.database.DatabaseTemplate;
import com.dreaming.hscj.template.database.wrapper.DatabaseSetting;
import com.dreaming.hscj.utils.FileUtils;

import java.nio.charset.StandardCharsets;
import java.security.InvalidParameterException;
import java.util.logging.Logger;
import java.util.zip.CRC32;

import priv.songxusheng.easyjson.ESONObject;

public class Template {
    private static Logger logger = Logger.getLogger(Template.class.getName());


    public static Template read(String path) throws Exception{
        String srcData = FileUtils.readAll(path);
        if(srcData == null || srcData.trim().isEmpty()) throw new InvalidParameterException("不是有效的模板文件！");

        ESONObject eData = new ESONObject(srcData);

        Template template = new Template();

        CRC32 crc32 = new CRC32();
        byte[] bytes = srcData.getBytes("utf-8");
        crc32.update(bytes,0,bytes.length);
        template.lTempCode = crc32.getValue();
        template.path = path;

        logger.info("Starting read!");
        template.apiTemplate      = ApiTemplate     .read(eData.getJSONValue("ApiTemplate"     ,new ESONObject()));
        logger.info("read ApiTemplate success!");
        template.databaseTemplate = DatabaseTemplate.read(eData.getJSONValue("DatabaseTemplate",new ESONObject()));
        logger.info("read DatabaseTemplate success!");
        logger.info("read finished!");
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

    public Api apiOf(int type){
        return apiTemplate.getApi(type);
    }

    public ApiConfig getApiConfig(){
        return apiTemplate.getApiConfig();
    }

    public DatabaseTemplate getDatabaseTemplate(){
        return databaseTemplate;
    }

    public DatabaseSetting getDatabaseSetting(){
        return databaseTemplate.getDatabaseSetting();
    }

}
