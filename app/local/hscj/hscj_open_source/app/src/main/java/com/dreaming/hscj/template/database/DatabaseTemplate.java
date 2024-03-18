package com.dreaming.hscj.template.database;

import com.dreaming.hscj.template.database.wrapper.DatabaseConfig;
import com.dreaming.hscj.template.database.wrapper.DatabaseSetting;

import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

import java.util.logging.Logger;

public class DatabaseTemplate {
    private static Logger logger = Logger.getLogger(DatabaseTemplate.class.getName());


    private DatabaseSetting databaseSetting;
    public DatabaseSetting getDatabaseSetting() {
        return databaseSetting;
    }


    public static DatabaseTemplate read(ESONObject data) throws Exception{
        DatabaseTemplate databaseTemplate = new DatabaseTemplate();

        ESONArray dbArray = data.getJSONValue("DB",new ESONArray());
        if(dbArray.length() != 3) throw new IllegalArgumentException("DB数组长度应当为3个！");

        databaseTemplate.databaseSetting = DatabaseSetting.parse(data.getJSONValue("DBSetting",new ESONObject()));
        logger.info("read DBSetting success!");

        DatabaseConfig config1 = DatabaseConfig.parse("DB[0]",dbArray.getArrayValue(0,new ESONObject()));
        logger.info("read DB1 config success!");
        DatabaseConfig config2 = DatabaseConfig.parse("DB[1]",dbArray.getArrayValue(1,new ESONObject()));
        logger.info("read DB2 config success!");
        DatabaseConfig config3 = DatabaseConfig.parse("DB[2]",dbArray.getArrayValue(2,new ESONObject()));
        logger.info("read DB3 config success!");


        logger.info("read DB success!");

        return databaseTemplate;
    }

    private DatabaseTemplate(){}

}
