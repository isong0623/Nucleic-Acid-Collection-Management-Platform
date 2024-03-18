package com.dreaming.hscj.template.database.impl;

import com.dreaming.hscj.template.database.wrapper.DatabaseConfig;

import priv.songxusheng.easyjson.ESONObject;

//社区成员导入数据库
public class UserInputGuideDatabase {
    private final DatabaseConfig config;
    public DatabaseConfig getConfig() {
        return config;
    }

    public static UserInputGuideDatabase init(DatabaseConfig config){
        return new UserInputGuideDatabase(config);
    }

    private UserInputGuideDatabase(DatabaseConfig config){
        this.config = config;
    }

    public String getIdFieldName(){
        return config.getFields().get(0).getName();
    }

    public String getNameFiledName(){
        return config.getFields().get(1).getName();
    }

    public boolean isValidLoginPassword(String password){
        return getConfig().getPassword().equals(password);
    }
}
