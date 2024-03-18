package com.dreaming.hscj.template.database.wrapper;



import com.dreaming.hscj.Constants;
import com.dreaming.hscj.template.api.wrapper.ApiParam;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.dreaming.hscj.utils.Pair;
import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

public class DatabaseConfig {
    public static final int TYPE_USER_GUIDE = 0;
    public static final int TYPE_USER_OVERALL = 1;
    public static final int TYPE_NA_GROUPING = 2;
    public static final int TYPE_NA_NONE_GROUP = 3;
    public static final int TYPE_NA_LOG = 4;

    private static void autoCheckSetter(String tag, String input){
        if("INPUT".equals(input)) return;
        if("EXCEL".equals(input)) return;
        if(input==null||input.trim().isEmpty()||input.replaceAll("Api\\[[0-6]\\]\\[\\\"[a-zA-Z_]([a-zA-Z0-9_]*)\\\"\\]","").length()!=0)
            throw new InvalidParameterException(tag+"无法解析的表达式！");
    }

    private static void autoCheckGetter(String tag, String output){
        if("OUTPUT".equals(output)) return;
        if(output==null||output.trim().isEmpty()||output.replaceAll("Api\\[[0-6]\\]\\[\\\"[a-zA-Z_]([a-zA-Z0-9_]*)\\\"\\]","").length()!=0)
            throw new InvalidParameterException(tag+"无法解析的表达式！");
    }

    public static DatabaseConfig parse(String tag, ESONObject data){
        DatabaseConfig config = new DatabaseConfig();

        int type = data.getJSONValue("type",-1);
        if(type<0 || type>3){
            throw new InvalidParameterException(tag+"[\"type\"]类型错误！");
        }
        config.type = type;

        String password = data.getJSONValue("password","").trim();
        if(password.isEmpty()){
            throw new InvalidParameterException(tag+"[\"password\"]不能为空！");
        }
        config.password = password;

        List<ApiParam> lstField = new ArrayList<>();
        ESONArray fields = data.getJSONValue("fields",new ESONArray());
        for(int i=0;i<fields.length();++i){
            lstField.add(ApiParam.parse(tag+"[\"fields\"]["+i+"]",fields.getArrayValue(i,new ESONArray())));
        }
        if(lstField.isEmpty()){
            throw new InvalidParameterException(tag+"[\"fields\"]参数不能为空！");
        }
        config.fields = lstField;

        ESONObject usage = data.getJSONValue("usage",new ESONObject());

        Map<Integer,Map<String,String>> setMapper = new HashMap<>();
        ESONObject setter = usage.getJSONValue("setter",new ESONObject());
        Map<String,List<String>> mSetter = new HashMap<>();
        for (Iterator<String> it = setter.keys(); it.hasNext(); ) {
            String key = it.next();
            String keyTag = tag+"[\"usage\"][\"setter\"][\""+key+"\"]";
            ESONArray value = setter.getJSONValue(key,new ESONArray());

            if(value.length()==0) throw new InvalidParameterException(keyTag+"值不能为空！");

            List<String> lstSetter = new ArrayList<>();
            for(int i=0;i<value.length();++i){
                String input = value.getArrayValue(i,"");
                String inputTag = keyTag+"["+i+"]";
                autoCheckSetter(inputTag,input);
                lstSetter.add(input);
                if(!input.startsWith("Api")) continue;
                try {
                    int apiType = Integer.parseInt(input.substring(input.indexOf("[")+1,input.indexOf("]")));
                    Map<String,String> s = setMapper.get(apiType);
                    if(s==null){
                        s = new HashMap<>();
                    }
                    s.put(key,input.substring(input.indexOf("\"")+1,input.lastIndexOf("\"")));
                    setMapper.put(apiType,s);
                } catch (Exception e) { }
            }
            mSetter.put(key,lstSetter);
        }
        if(mSetter.size()!=fields.length()) throw new InvalidParameterException( tag+"[\"usage\"][\"setter\"]成员个数与其对应的fields个数不一致！");
        config.setter = mSetter;
        config.setMapper = setMapper;

        ESONObject getter = usage.getJSONValue("getter",new ESONObject());
        Map<Integer,Map<String,String>> getMapper = new HashMap<>();
        Map<String,List<String>> mGetter = new HashMap<>();
        for (Iterator<String> it = getter.keys(); it.hasNext(); ) {
            String key = it.next();
            String keyTag = tag+"[\"usage\"][\"getter\"][\""+key+"\"]";
            ESONArray value = getter.getJSONValue(key,new ESONArray());

            if(value.length()==0) throw new InvalidParameterException(keyTag+"值不能为空！");

            List<String> lstGetter = new ArrayList<>();
            for(int i=0;i<value.length();++i){
                String input = value.getArrayValue(i,"");
                String inputTag = keyTag+"["+i+"]";
                autoCheckGetter(inputTag,input);
                lstGetter.add(input);
                if(!input.startsWith("Api")) continue;
                try {
                    int apiType = Integer.parseInt(input.substring(input.indexOf("[")+1,input.indexOf("]")));
                    Map<String,String> s = getMapper.get(apiType);
                    if(s==null){
                        s = new HashMap<>();
                    }
                    s.put(input.substring(input.indexOf("\"")+1,input.lastIndexOf("\"")),key);
                    getMapper.put(apiType,s);
                } catch (Exception e) { }
            }
            mGetter.put(key,lstGetter);
        }
//        if(mGetter.size()!=fields.length()) throw new InvalidParameterException( tag+"[\"usage\"][\"getter\"]成员个数与其对应的fields个数不一致！");
        config.getter = mGetter;
        config.getMapper = getMapper;

        return config;
    }

    private DatabaseConfig(){};

    private int type;
    public DatabaseConfig setType(int type){
        this.type = type;
        return this;
    }
    public int getType(){
        return type;
    }

    private String password;
    public DatabaseConfig setPassword(String password){
        this.password = password;
        return this;
    }
    public String getPassword(){
        return password;
    }

    private List<ApiParam> fields;
    public DatabaseConfig setFields(List<ApiParam> fields){
        this.fields = fields;
        return this;
    }
    public List<ApiParam> getFields(){
        return fields;
    }

    private Map<String,ApiParam> mFieldMapper = new HashMap<>();
    public synchronized Map<String,ApiParam> getFieldMapper(){
        if(!mFieldMapper.isEmpty()) return mFieldMapper;
        if(getFields().isEmpty()) return mFieldMapper;
        for(ApiParam param:getFields()){
            mFieldMapper.put(param.getName(),param);
        }
        return mFieldMapper;
    }

    private Map<String,List<String>> setter;
    public DatabaseConfig setSetter(Map<String,List<String>> setter){
        this.setter = setter;
        return this;
    }
    public Map<String,List<String>> getSetter(){
        return setter;
    }

    //Map<ApiType,Map<DbField,ApiField>>
    private Map<Integer,Map<String,String>> setMapper = new HashMap<>();
    public DatabaseConfig setSetMapper(Map<Integer,Map<String,String>> setMapper){
        this.setMapper = setMapper;
        return this;
    }
    public Map<Integer,Map<String,String>> getSetMapper(){
        return setMapper;
    }

    private Map<String,List<String>> getter = new HashMap<>();
    public DatabaseConfig setGetter(Map<String,List<String>> getter){
        this.getter = getter;
        return this;
    }
    public Map<String,List<String>> getGetter(){
        return getter;
    }

    //Map<ApiType,Map<ApiField,DbField>>
    private Map<Integer,Map<String,String>> getMapper;
    public DatabaseConfig setGetMapper(Map<Integer,Map<String,String>> getMapper){
        this.getMapper = getMapper;
        return this;
    }
    public Map<Integer,Map<String,String>> getGetMapper(){
        return getMapper;
    }

    private List<Update> update;
    public List<Update> getUpdate() { return update; }

    public static class Update{
        long version;

        List<ApiParam> add = new ArrayList<>();
        public List<ApiParam> getAdd() {
            return add;
        }

        List<String>   del = new ArrayList<>();
        public List<String> getDel() {
            return del;
        }

        List<Pair<String,ApiParam>> alter = new ArrayList<>();
        public List<Pair<String, ApiParam>> getAlter() {
            return alter;
        }

        List<Pair<String,Integer >> move  = new ArrayList<>();
        public List<Pair<String, Integer>> getMove() {
            return move;
        }

        //Map<ApiType,Map<DbField,ApiField>>
        private Map<Integer,Map<String,String>> setMapper = new HashMap<>();
        public void setSetMapper(Map<Integer,Map<String,String>> setMapper){
            this.setMapper = setMapper;
        }
        public Map<Integer,Map<String,String>> getSetMapper(){
            return setMapper;
        }
        private Map<String,List<String>> setter = new HashMap<>();
        public Map<String, List<String>> getSetter() {
            return setter;
        }

        //Map<ApiType,Map<ApiField,DbField>>
        private Map<Integer,Map<String,String>> getMapper;
        public void setGetMapper(Map<Integer,Map<String,String>> getMapper){
            this.getMapper = getMapper;
        }
        public Map<Integer,Map<String,String>> getGetMapper(){
            return getMapper;
        }
        private Map<String,List<String>> getter = new HashMap<>();
        public Map<String, List<String>> getGetter() {
            return getter;
        }

        private static Update parse(String tag, ESONObject data) {
            Update update = new Update();

            long version = data.getJSONValue("version",-1L);
            if(version <0L)
                throw new InvalidParameterException(tag+"[\"version\"]非法！");
            update.version = version;

            ESONArray eAdd = data.getJSONValue("add",new ESONArray());
            List<ApiParam> add = new ArrayList<>();
            for(int i=0;i<eAdd.length();++i){
                add.add(ApiParam.parse(tag+"[\"add\"]["+i+"]",eAdd.getArrayValue(i,new ESONArray())));
            }
            update.add = add;

            ESONArray eDel = data.getJSONValue("del",new ESONArray());
            List<String>   del = new ArrayList<>();
            for(int i=0;i<eDel.length();++i){
                del.add(eDel.getArrayValue(i,""));
            }
            update.del = del;

            ESONObject eAlter = data.getJSONValue("alter",new ESONObject());
            List<Pair<String,ApiParam>> alter = new ArrayList<>();
            for (Iterator<String> it = eAlter.keys(); it.hasNext(); ) {
                String key = it.next();
                ESONArray arr = eAlter.getJSONValue(key,new ESONArray());

                ApiParam p = ApiParam.parse(tag+"[\"alter\"]["+key+"]",arr);
                if(!p.getName().equals(key))
                    throw new InvalidParameterException(tag+"[\"alter\"]["+key+"]字段名必须一致！");

                alter.add(new Pair(key,p));
            }

            ESONObject eMove = data.getJSONValue("move",new ESONObject());
            List<Pair<String,Integer >> move  = new ArrayList<>();
            for (Iterator<String> it = eMove.keys(); it.hasNext(); ) {
                String key = it.next();
                Integer index = eMove.getJSONValue(key,-1);
                if(index<0)
                    throw new InvalidParameterException(tag+"[\"alter\"]["+key+"]索引错误！");

                move.add(new Pair<>(key,index));
            }

            ESONObject eUsage= data.getJSONValue("usage",new ESONObject());

            Map<Integer,Map<String,String>> setMapper = new HashMap<>();
            ESONObject setter = eUsage.getJSONValue("setter",new ESONObject());
            Map<String,List<String>> mSetter = new HashMap<>();
            for (Iterator<String> it = setter.keys(); it.hasNext(); ) {
                String key = it.next();
                String keyTag = tag+"[\"usage\"][\"setter\"][\""+key+"\"]";
                ESONArray value = setter.getJSONValue(key,new ESONArray());

                if(value.length()==0) throw new InvalidParameterException(keyTag+"值不能为空！");

                List<String> lstSetter = new ArrayList<>();
                for(int i=0;i<value.length();++i){
                    String input = value.getArrayValue(i,"");
                    String inputTag = keyTag+"["+i+"]";
                    autoCheckSetter(inputTag,input);
                    lstSetter.add(input);
                    if(!input.startsWith("Api")) continue;
                    try {
                        int apiType = Integer.parseInt(input.substring(input.indexOf("[")+1,input.indexOf("]")));
                        Map<String,String> s = setMapper.get(apiType);
                        if(s==null){
                            s = new HashMap<>();
                        }
                        s.put(key,input.substring(input.indexOf("\"")+1,input.lastIndexOf("\"")));
                        setMapper.put(apiType,s);
                    } catch (Exception e) { }
                }
                mSetter.put(key,lstSetter);
            }
            if(mSetter.size()!=add.size()) throw new InvalidParameterException( tag+"[\"usage\"][\"setter\"]成员个数与其对应的add个数不一致！");
            update.setter = mSetter;
            update.setMapper = setMapper;

            ESONObject getter = eUsage.getJSONValue("getter",new ESONObject());
            Map<Integer,Map<String,String>> getMapper = new HashMap<>();
            Map<String,List<String>> mGetter = new HashMap<>();
            for (Iterator<String> it = getter.keys(); it.hasNext(); ) {
                String key = it.next();
                String keyTag = tag+"[\"usage\"][\"getter\"][\""+key+"\"]";
                ESONArray value = getter.getJSONValue(key,new ESONArray());

                if(value.length()==0) throw new InvalidParameterException(keyTag+"值不能为空！");

                List<String> lstGetter = new ArrayList<>();
                for(int i=0;i<value.length();++i){
                    String input = value.getArrayValue(i,"");
                    String inputTag = keyTag+"["+i+"]";
                    autoCheckGetter(inputTag,input);
                    lstGetter.add(input);
                    if(!input.startsWith("Api")) continue;
                    try {
                        int apiType = Integer.parseInt(input.substring(input.indexOf("[")+1,input.indexOf("]")));
                        Map<String,String> s = getMapper.get(apiType);
                        if(s==null){
                            s = new HashMap<>();
                        }
                        s.put(input.substring(input.indexOf("\"")+1,input.lastIndexOf("\"")),key);
                        getMapper.put(apiType,s);
                    } catch (Exception e) { }
                }
                mGetter.put(key,lstGetter);
            }

            update.getter = mGetter;
            update.getMapper = getMapper;

            return update;
        }
    }

}
