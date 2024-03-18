package com.dreaming.hscj.template.database;

import android.util.Log;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.dreaming.hscj.template.api.ApiTemplate;
import com.dreaming.hscj.template.database.impl.DaySamplingLogDatabase;
import com.dreaming.hscj.template.database.impl.NoneGroupingDatabase;
import com.dreaming.hscj.template.database.impl.NucleicAcidGroupingDatabase;
import com.dreaming.hscj.template.database.impl.UserInputGuideDatabase;
import com.dreaming.hscj.template.database.impl.UserOverallDatabase;
import com.dreaming.hscj.template.database.wrapper.BaseDatabase;
import com.dreaming.hscj.template.database.wrapper.DatabaseConfig;
import com.dreaming.hscj.template.database.wrapper.DatabaseSetting;
import com.dreaming.hscj.utils.FileUtils;

import java.io.File;

import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

public class DatabaseTemplate {
    private static final String TAG = DatabaseTemplate.class.getSimpleName();

    private NucleicAcidGroupingDatabase instanceOfNucleicAcidGroupingDatabase;
    public NucleicAcidGroupingDatabase getInstanceOfNucleicAcidGroupingDatabase() {
        return instanceOfNucleicAcidGroupingDatabase;
    }

    private UserInputGuideDatabase instanceOfUserInputGuideDatabase;
    public UserInputGuideDatabase getInstanceOfUserInputGuideDatabase() {
        return instanceOfUserInputGuideDatabase;
    }

    private UserOverallDatabase instanceOfUserOverallDatabase;
    public UserOverallDatabase getInstanceOfUserOverallDatabase() {
        return instanceOfUserOverallDatabase;
    }

    private DaySamplingLogDatabase instanceOfDaySamplingLogDatabase;
    public DaySamplingLogDatabase getInstanceOfDaySamplingLogDatabase() {
        return instanceOfDaySamplingLogDatabase;
    }

    private NoneGroupingDatabase instanceOfNoneGroupingDatabase;
    public NoneGroupingDatabase getInstanceOfNoneGroupingDatabase() {
        return instanceOfNoneGroupingDatabase;
    }

    public void setUseTempDbPath(long lTempCode, String regionName, String townName, String villageName){
        instanceOfUserOverallDatabase         .setUseTempDbPath(lTempCode, regionName, townName, villageName);
        instanceOfNucleicAcidGroupingDatabase .setUseTempDbPath(lTempCode, regionName, townName, villageName);
        instanceOfDaySamplingLogDatabase      .setUseTempDbPath(lTempCode, regionName, townName, villageName);
        instanceOfNoneGroupingDatabase        .setUseTempDbPath(lTempCode, regionName, townName, villageName);
    }

    public void closeDbAndDelete(long lTempCode, String regionName, String townName, String villageName){
        instanceOfUserOverallDatabase         .closeDbAndDelete(lTempCode, regionName, townName, villageName);
        instanceOfNucleicAcidGroupingDatabase .closeDbAndDelete(lTempCode, regionName, townName, villageName);
        instanceOfDaySamplingLogDatabase      .closeDbAndDelete(lTempCode, regionName, townName, villageName);
        instanceOfNoneGroupingDatabase        .closeDbAndDelete(lTempCode, regionName, townName, villageName);
    }

    private DatabaseSetting databaseSetting;
    public DatabaseSetting getDatabaseSetting() {
        return databaseSetting;
    }

    public static DatabaseTemplate read(ESONObject data, ApiTemplate apiTemplate) throws Exception{
        DatabaseTemplate databaseTemplate = new DatabaseTemplate();

        ESONArray dbArray = data.getJSONValue("DB",new ESONArray());
        if(dbArray.length() != 3) throw new IllegalArgumentException("DB数组长度应当为3个！");

        databaseTemplate.databaseSetting = DatabaseSetting.parse(data.getJSONValue("DBSetting",new ESONObject()));
        Log.e(TAG,"read DBSetting success!");

        DatabaseConfig config = DatabaseConfig.parse("DB[0]",dbArray.getArrayValue(0,new ESONObject()));
        databaseTemplate.instanceOfUserInputGuideDatabase      = UserInputGuideDatabase     .init(config);
        databaseTemplate.instanceOfUserOverallDatabase         = UserOverallDatabase        .init(dbArray.getArrayValue(1,new ESONObject()),databaseTemplate.instanceOfUserInputGuideDatabase);
        databaseTemplate.instanceOfNucleicAcidGroupingDatabase = NucleicAcidGroupingDatabase.init(dbArray.getArrayValue(2,new ESONObject()),databaseTemplate.instanceOfUserInputGuideDatabase,databaseTemplate.databaseSetting);
        databaseTemplate.instanceOfDaySamplingLogDatabase      = DaySamplingLogDatabase.parse(databaseTemplate.instanceOfUserInputGuideDatabase);
        databaseTemplate.instanceOfNoneGroupingDatabase        = NoneGroupingDatabase  .parse(databaseTemplate.instanceOfUserInputGuideDatabase,databaseTemplate.instanceOfUserOverallDatabase,databaseTemplate.instanceOfNucleicAcidGroupingDatabase);

        databaseTemplate.instanceOfUserOverallDatabase        .attachNoneGroupingDatabase(databaseTemplate.instanceOfNoneGroupingDatabase);
        databaseTemplate.instanceOfUserOverallDatabase        .attachNucleicAcidGroupingDatabase(databaseTemplate.instanceOfNucleicAcidGroupingDatabase);
        databaseTemplate.instanceOfNucleicAcidGroupingDatabase.attachNoneGroupingDatabase(databaseTemplate.instanceOfNoneGroupingDatabase);

        databaseTemplate.instanceOfUserOverallDatabase         .attachSetting(databaseTemplate.databaseSetting);
        databaseTemplate.instanceOfNucleicAcidGroupingDatabase .attachSetting(databaseTemplate.databaseSetting);
        databaseTemplate.instanceOfDaySamplingLogDatabase      .attachSetting(databaseTemplate.databaseSetting);
        databaseTemplate.instanceOfNoneGroupingDatabase        .attachSetting(databaseTemplate.databaseSetting);

        databaseTemplate.instanceOfUserOverallDatabase.attachApiConfig(apiTemplate.getApiConfig());

        Log.e(TAG,"read DB success!");

        return databaseTemplate;
    }

    private DatabaseTemplate(){}

}
