package com.dreaming.hscj.template.database.wrapper;

import android.content.DialogInterface;
import android.util.Pair;
import android.widget.EditText;
import android.widget.TextView;

import com.dreaming.hscj.Constants;
import com.dreaming.hscj.R;
import com.dreaming.hscj.base.BaseActivity;
import com.dreaming.hscj.base.ToastDialog;
import com.dreaming.hscj.template.api.wrapper.ApiParam;
import com.dreaming.hscj.utils.DensityUtils;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import priv.songxusheng.easydialog.EasyDialog;
import priv.songxusheng.easydialog.EasyDialogHolder;
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

        ESONArray updates = data.getJSONValue("updates",new ESONArray());
        List<Update> lstUpdate = new ArrayList<>();
        for(int i=0,ni=updates.length();i<ni;++i){
            Update update = Update.parse(tag+"[\"updates\"]["+i+"]",updates.getArrayValue(i,new ESONObject()));
            lstUpdate.add(update);
        }
        config.update = lstUpdate;
        Collections.sort(lstUpdate, new Comparator<Update>() {
            @Override
            public int compare(Update o1, Update o2) {
                return Long.compare(o1.version, o2.version);
            }
        });

        Set<String> sDel = new HashSet<>();
        Set<String> sAdd = new HashSet<>();
        List<ApiParam> lstFields = config.getFields();
        for(ApiParam p : lstFields){
            sAdd.add(p.getName());
        }

        for(int i=0,ni=config.update.size();i<ni;++i){
            DatabaseConfig.Update update = config.update.get(i);

            //del
            for(String key: update.del){
                if(sDel.contains(key))  throw new InvalidParameterException(tag + "[\"updates\"]["+i+"][\"del\"][\""+key+"\"]重复删除！");
                if(!sAdd.contains(key)) throw new InvalidParameterException(tag + "[\"updates\"]["+i+"][\"del\"][\""+key+"\"]不存在！");
                sAdd.remove(key);
                sDel.add(key);
            }

            //add
            for(ApiParam p: update.add){
                String key = p.getName();
                if(sDel.contains(key)) throw new InvalidParameterException(tag + "[\"updates\"]["+i+"][\"add\"][\""+key+"\"]已删除无法添加！");
                if(sAdd.contains(key)) throw new InvalidParameterException(tag + "[\"updates\"]["+i+"][\"add\"][\""+key+"\"]已存在无法添加！");
                lstFields.add(p);
            }

            //alter
            for(Pair<String,ApiParam> alter:update.alter){
                String key = alter.first;
                if(sDel.contains(key))  throw new InvalidParameterException(tag + "[\"updates\"]["+i+"][\"alter\"][\""+key+"\"]已删除无法更改！");
                if(!sAdd.contains(key)) throw new InvalidParameterException(tag + "[\"updates\"]["+i+"][\"alter\"][\""+key+"\"]字段不存在无法更改！");
            }

            //move
            for(Pair<String,Integer> move: update.move){
                String key = move.first;
                int index = move.second;
                if(sDel.contains(key))                 throw new InvalidParameterException(tag + "[\"updates\"]["+i+"][\"move\"][\""+key+"\"]已删除无法移动！");
                if(!sAdd.contains(key))                throw new InvalidParameterException(tag + "[\"updates\"]["+i+"][\"move\"][\""+key+"\"]字段不存在无法移动！");
                if(index<0 || index>=lstFields.size()) throw new InvalidParameterException(tag + "[\"updates\"]["+i+"][\"move\"][\""+key+"\"]非法的移动位置！");
            }
        }

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

    public interface IDBCreateListener{
        void onSuccess(String townName,String villageName);
        void onFailure();
    }

    public static void showCreateDatabaseDialog(BaseActivity activity, IDBCreateListener listener){
        final AtomicBoolean b = new AtomicBoolean(false);
        new EasyDialog(R.layout.dialog_create_database,activity)
                .setOnBindDialogListener(new EasyDialog.OnBindDialogListener() {
                    @Override
                    public void onBindDialog(EasyDialogHolder easyDialogHolder) {
                        TextView tvCreate = easyDialogHolder.getViewAsTextView(R.id.tv_create);
                        final EditText etTown   = easyDialogHolder.getViewAsEditText(R.id.ev_town_name);
                        final EditText etVillage= easyDialogHolder.getViewAsEditText(R.id.ev_village_name);

                        tvCreate.setOnClickListener(v -> {
                            String sTownName = etTown.getText().toString().trim();
                            if(sTownName.isEmpty()) {
                                ToastDialog.showCenter(activity,"县镇名称不能为空");
                                etTown.requestFocus();
                                return;
                            }

                            String sVillage = etVillage.getText().toString().trim();
                            if(sVillage.isEmpty()){
                                ToastDialog.showCenter(activity,"社区名称不能为空");
                                return;
                            }

                            switch (Constants.DBConfig.addDatabase(sTownName,sVillage)){
                                case 0:
                                    b.set(true);
                                    easyDialogHolder.dismissDialog();
                                    if(listener!=null){
                                        listener.onSuccess(sTownName,sVillage);
                                    }
                                    break;
                                case 1:
                                    ToastDialog.showCenter(activity,"数据库已存在");
                                    break;
                                case 2:
                                    ToastDialog.showCenter(activity,"数据保存失败！");
                                    break;
                            }
                        });
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if(listener!=null && !b.get()){
                            listener.onFailure();
                        }
                    }
                })
                .setForeground(activity.getResources().getDrawable(R.drawable.shape_common_dialog))
                .setDialogHeight((int) DensityUtils.dp2px(200f))
                .showDialog();
    }
}
