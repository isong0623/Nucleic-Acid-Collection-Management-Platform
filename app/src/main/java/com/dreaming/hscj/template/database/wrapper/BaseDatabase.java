package com.dreaming.hscj.template.database.wrapper;

import android.database.Cursor;
import android.util.Log;
import android.util.Pair;

import com.dreaming.hscj.App;
import com.dreaming.hscj.Constants;
import com.dreaming.hscj.core.ThreadPoolProvider;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.template.api.wrapper.ApiParam;
import com.dreaming.hscj.template.database.DatabaseTemplate;
import com.dreaming.hscj.utils.SPUtils;

import net.sqlcipher.SQLException;
import net.sqlcipher.database.SQLiteDatabase;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

public abstract class BaseDatabase {
    public static final String TAG = BaseDatabase.class.getSimpleName();

    static {
        SQLiteDatabase.loadLibs(App.sInstance);
    }

    public static String getDatabaseDir(String unifySocialCreditCodes, long tempCode, String regionName,String townName, String villageName){
        String path = String.format("%s/databases/%s/%s/%s/%s/%s",
                App.sInstance.getFilesDir().getParentFile().getAbsolutePath(),
                unifySocialCreditCodes,
                tempCode==0?"":String.valueOf(tempCode),
                regionName,
                townName,
                villageName
        )
                .replaceAll("//","/")
                .replaceAll("\\\\\\\\","\\")
                .replaceAll("/", File.separator)
                .replaceAll("\\\\",File.separator);

        return path;
    }

    public static String getDatabasePath(String unifySocialCreditCodes, long tempCode, String regionName,String townName, String villageName, String dbName){
        return getDatabaseDir(unifySocialCreditCodes, tempCode, regionName, townName, villageName) + File.separator + dbName;
    }

    abstract protected String getPassword();
    abstract protected String getDatabaseName();
    abstract protected String getTableName();

    protected BaseDatabase(DatabaseConfig config){
        this.config = config;
    }

    DatabaseSetting setting;
    public void attachSetting(DatabaseSetting setting){
        this.setting = setting;
    }

    protected abstract List<String> getPrimaryKey();

    private final DatabaseConfig config;
    public DatabaseConfig getConfig() {
        return config;
    }

    long lTempCode = 0L;
    String regionName;
    String townName;
    String villageName;
    public void setUseTempDbPath(long lTempCode, String regionName, String townName, String villageName){
        this.lTempCode = lTempCode;
        this.regionName = regionName;
        this.townName = townName;
        this.villageName = villageName;
    }

    public void closeDbAndDelete(long lTempCode, String regionName, String townName, String villageName){
        String path = getDatabasePath(
                setting.getUnifySocialCreditCodes(),
                lTempCode,
                regionName,
                townName,
                villageName,
                getDatabaseName()
        );
        Log.e(TAG,"closeAndDelete:"+path);
        SQLiteDatabase result = mDatabaseMapper.get(path);
        if(result == null) return;
        result.close();
        mDatabaseMapper.remove(path);
        new File(path).delete();
    }

    private static Map<String,SQLiteDatabase> mDatabaseMapper = new ConcurrentHashMap<>();
    protected synchronized SQLiteDatabase getDatabase(){
        ESONObject e = Constants.DBConfig.getSelectedDatabase();
        String regionName  = setting.getRegionName();
        String townName    = e.getJSONValue("townName","");
        String villageName = e.getJSONValue("villageName","");
        String path = getDatabasePath(
                setting.getUnifySocialCreditCodes(),
                lTempCode,
                lTempCode == 0L ? regionName  : this.regionName  ,
                lTempCode == 0L ? townName    : this.townName    ,
                lTempCode == 0L ? villageName : this.villageName ,
                getDatabaseName()
        );

        File fPath = new File(path);
        if(!fPath.getParentFile().exists()) fPath.getParentFile().mkdirs();
        if(!fPath.exists()) {
            try { fPath.createNewFile(); } catch (Exception ex) { }
        }

        SQLiteDatabase result = mDatabaseMapper.get(path);
        Log.e(TAG,"getDatabase:"+path+" "+result);
        if(result == null || !result.isOpen()){
            Log.e(TAG,path);
            result = SQLiteDatabase.openOrCreateDatabase(path, getPassword(), null);
            result.execSQL(getCreateTableStatement());
            update(result);
            mDatabaseMapper.put(path,result);
        }

        return result;
    }

    public void beginTransaction(){
        SQLiteDatabase db = getDatabase();
        Log.e(TAG,"setTransactionSuccessful "+db);
        db.beginTransaction();
    }
    public void setTransactionSuccessful(){
        SQLiteDatabase db = getDatabase();
        Log.e(TAG,"setTransactionSuccessful "+db);
        db.setTransactionSuccessful();
    }
    public void endTransaction(){
        SQLiteDatabase db = getDatabase();
        Log.e(TAG,"setTransactionSuccessful "+db);
        db.endTransaction();
    }

    private String parseToPrimaryKey(){
        StringBuilder sb = new StringBuilder();

        List<String> lst = getPrimaryKey();
        for(int i=0,ni=lst.size();i<ni;++i){
            if(i!=0) sb.append(",");
            sb.append("`").append(lst.get(i)).append("`");
        }

        return sb.toString();
    }
    protected String getCreateTableStatement(){
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE IF NOT EXISTS `").append(getTableName()).append("` (\n\t");
        List<ApiParam> lstParams = getConfig().getFields();
        for(int i=0;i<lstParams.size();++i){
            ApiParam param = lstParams.get(i);
            if(i!=0) sb.append(",\n\t");
            sb.append(parseItemToCreateStatement(param));
        }
        sb.append(",\n");
        sb.append("PRIMARY KEY (").append(parseToPrimaryKey()).append(")\n");
        sb.append(");");

        return sb.toString();
    }

    private String parseItemToCreateStatement(ApiParam param){
        StringBuilder sb = new StringBuilder();

        sb.append("`").append(param.getName()).append("`").append(" ");

        String type = param.getType();
        boolean bIsString = false;
        boolean bIsDate   = false;
        boolean bIsBoolean= false;
        boolean bIsDouble = false;
        switch (type.trim().toUpperCase().split(":")[0]){
            case "DATE":
                sb.append("TIMESTAMP").append(" ");
                bIsDate = true;
                break;
            case "STRING":
                sb.append("TEXT").append(" ");
                bIsString = true;
                break;
            case "DOUBLE":
            case "FLOAT":
                sb.append("REAL").append(" ");
                bIsDouble = true;
                break;
            case "BOOL":
            case "BOOLEAN":
                bIsBoolean = true;
            case "INT":
            case "INTEGER":
            case "LONG":
                sb.append("INTEGER").append(" ");
                break;
        }

        String value = param.getDefaultValue();
        if(ApiParam.DEFAULT_NO_EMPTY.equals(value)){
            sb.append("NOT NULL");
        }
        else if(value == null){

        }
        else if(ApiParam.DEFAULT_NULL_STR.equals(value)){
            if(bIsString){
                sb.append("DEFAULT 'null'");
            }
        }
        else if(!value.trim().isEmpty()){
            if(bIsDate && ("CURRENT_TIMESTAMP".equalsIgnoreCase(value) || "NOW".equalsIgnoreCase(value))){
                sb.append("DEFAULT CURRENT_TIMESTAMP");
            }
            else if(bIsString){
                sb.append("DEFAULT '").append(value.replaceAll("'","''")).append("'");
            }
            else{
                sb.append("DEFAULT ").append(value);
            }
        }

        return sb.toString();
    }

    private void update(SQLiteDatabase db){
        long version = SPUtils.with(setting.getSPUnify()).getLong(getDatabaseName(),0L);

        List<DatabaseConfig.Update> updates = getConfig().getUpdate();
        if(updates.isEmpty()) return;

        Set<String> sDel = new HashSet<>();
        Set<String> sAdd = new HashSet<>();
        List<ApiParam> lstFields = getConfig().getFields();
        for(ApiParam p : lstFields){
            sAdd.add(p.getName());
        }
        db.beginTransaction();
        for(int i=0,ni=updates.size();i<ni;++i){
            DatabaseConfig.Update update = updates.get(i);
            String tag = "updates["+i+"]";
            long v = update.version;

            //del
            for(String key: update.del){
                if(sDel.contains(key)) throw new InvalidParameterException(tag + "[\""+key+"\"]重复删除！");
                if(!sAdd.contains(key)) throw new InvalidParameterException(tag + "[\""+key+"\"]不存在！");

                getConfig().getGetter().remove(key);
                getConfig().getSetter().remove(key);
                sAdd.remove(key);
                sDel.add(key);
            }

            //add
            for(ApiParam p: update.add){
                String key = p.getName();
                if(sDel.contains(key)) throw new InvalidParameterException(tag + "[\""+key+"\"]已删除无法添加！");
                if(sAdd.contains(key)) throw new InvalidParameterException(tag + "[\""+key+"\"]已存在无法添加！");
                lstFields.add(p);

                if(v <= version) continue;

                List<String> tmp = getConfig().getSetter().get(key);
                if(tmp.isEmpty()) tmp = new ArrayList<>();
                tmp.addAll(update.getSetter().get(key));
                getConfig().getSetter().put(key,tmp);

                List<String> tmp1 = getConfig().getGetter().get(key);
                List<String> tmp2 = update.getGetter().get(key);
                if(tmp1 == null) tmp1 = new ArrayList<>();
                if(tmp2 != null) {
                    tmp1.addAll(tmp2);
                    getConfig().getGetter().put(key,tmp1);
                }

                db.execSQL("ALTER TABLE `" + getTableName() + "` ADD COLUMN "+ parseItemToCreateStatement(p) + ";");
            }

            //alter
            for(Pair<String,ApiParam> alter:update.alter){
                String key = alter.first;
                if(sDel.contains(key)) throw new InvalidParameterException(tag + "[\""+key+"\"]已删除无法更改！");
                if(!sAdd.contains(key)) throw new InvalidParameterException(tag + "[\""+key+"\"]字段不存在无法更改！");
                if(v <= version) continue;
                for(int j = 0,nj=lstFields.size();j<nj;++j){
                    ApiParam p = lstFields.get(j);
                    if(p.getName().equals(key)){
                        lstFields.remove(j);
                        lstFields.add(j,alter.second);
                        break;
                    }
                }
            }

            //move
            for(Pair<String,Integer> move: update.move){
                String key = move.first;
                int index = move.second;
                if(sDel.contains(key)) throw new InvalidParameterException(tag + "[\""+key+"\"]已删除无法移动！");
                if(!sAdd.contains(key)) throw new InvalidParameterException(tag + "[\""+key+"\"]字段不存在无法移动！");
                if(index<0 || index>=lstFields.size()) throw new InvalidParameterException(tag + "[\""+key+"\"]字段不存在无法移动！");
                for(int j = 0,nj=lstFields.size();j<nj;++j) {
                    ApiParam p = lstFields.get(j);
                    if(p.getName().equals(key)){
                        lstFields.add(index,p);
                        lstFields.remove(j);
                        break;
                    }
                }
            }
        }

        //Map<ApiType,Map<ApiField,DbField>>
        Map<Integer,Map<String,String>> rebuildGet = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : getConfig().getGetter().entrySet()) {
            String dbKey = entry.getKey();
            for(String getter: entry.getValue()){
                if(getter == null) continue;
                if(!getter.startsWith("Api")) continue;
                Integer type = null;
                try { type = Integer.valueOf(getter.substring(getter.indexOf("[")+1,getter.indexOf("]"))); } catch (Exception e) { continue; }
                String apiKey = getter.substring(getter.indexOf("\"")+1,getter.lastIndexOf("\""));

                Map<String,String> apiMapper = rebuildGet.get(type);
                if(apiMapper == null) apiMapper = new HashMap<>();
                apiMapper.put(apiKey,dbKey);
                rebuildGet.put(type,apiMapper);
            }
        }
        getConfig().setGetMapper(rebuildGet);

        //Map<ApiType,Map<DbField,ApiField>>
        Map<Integer,Map<String,String>> rebuildSet = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : getConfig().getSetter().entrySet()) {
            String dbKey = entry.getKey();
            for(String setter: entry.getValue()){
                if(setter == null) continue;
                if(!setter.startsWith("Api")) continue;
                Integer type = null;
                try { type = Integer.valueOf(setter.substring(setter.indexOf("[")+1,setter.indexOf("]"))); } catch (Exception e) { continue; }
                String apiKey = setter.substring(setter.indexOf("\"")+1,setter.lastIndexOf("\""));

                Map<String,String> dbMapper = rebuildSet.get(type);
                if(dbMapper == null) dbMapper = new HashMap<>();
                dbMapper.put(dbKey,apiKey);
                rebuildSet.put(type,dbMapper);
            }
        }
        getConfig().setSetMapper(rebuildSet);

        SPUtils.with(setting.getSPUnify()).commitLong(getDatabaseName(),setting.getNetApiVersion());
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    private String parseToWhere(List<String> lstKeys){
        if(lstKeys.isEmpty()) return "1==1";

        StringBuilder sb = new StringBuilder();
        for(int i=0,ni=lstKeys.size();i<ni;++i){
            if(i!=0)sb.append(" AND ");
            sb.append("`").append(lstKeys.get(i)).append("`").append("=?");
        }
        return sb.toString();
    }

    private String parseToQuery(List<String> lstKeys){
        if(lstKeys.isEmpty()) return "*";

        StringBuilder sb = new StringBuilder();
        for(int i=0,ni=lstKeys.size();i<ni;++i){
            if(i!=0)sb.append(",");
            sb.append("`").append(lstKeys.get(i)).append("`");
        }
        return sb.toString();
    }

    protected String getQueryStatement(List<String> lstQueryKeys, List<String> lstWhereKeys){
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ").append(parseToQuery(lstQueryKeys)).append(" FROM `").append(getTableName()).append("` WHERE ").append(parseToWhere(lstWhereKeys)).append(";");
        return sb.toString();
    }

    private String parseToOrder(List<String> lstKeys){
        if(lstKeys.isEmpty()) return "1";

        StringBuilder sb = new StringBuilder();
        for(int i=0,ni=lstKeys.size();i<ni;++i){
            if(i!=0)sb.append(",");
            sb.append("`").append(lstKeys.get(i)).append("`");
        }
        return sb.toString();
    }

    protected String getQueryStatement(List<String> lstQueryKeys, List<String> lstWhereKeys,int page,int pageSize){
        StringBuilder sb = new StringBuilder()
                            .append("SELECT ").append(parseToQuery(lstQueryKeys)).append(" FROM `").append(getTableName()).append("` ")
                            .append("WHERE ").append(parseToWhere(lstWhereKeys)).append(" ")
                            .append("ORDER BY ").append(parseToOrder(getPrimaryKey())).append(" ")
                            .append("LIMIT ").append(pageSize).append(" OFFSET ").append(page*pageSize)
                            .append(";");
        return sb.toString();
    }

    private String parseToUpdate(List<String> lstKeys){
        if(lstKeys.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        for(int i=0,ni=lstKeys.size();i<ni;++i){
            if(i!=0)sb.append(",");
            sb.append("`").append(lstKeys.get(i)).append("`").append("=?");
        }
        return sb.toString();
    }

    protected String getUpdateStatement(List<String> lstUpdateKeys, List<String> lstWhereKeys){
        StringBuilder sb = new StringBuilder();

        sb.append("UPDATE `").append(getTableName()).append("` SET ").append(parseToUpdate(lstUpdateKeys)).append(" WHERE ").append(parseToWhere(lstWhereKeys)).append(";");

        return sb.toString();
    }

    private String parseToInsert(List<String> lstKeys){
        if(lstKeys.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        for(int i=0,ni=lstKeys.size();i<ni;++i){
            if(i!=0)sb.append(",");
            sb.append("`").append(lstKeys.get(i)).append("`");
        }
        return sb.toString();
    }

    private String parseToInsertValues(int length){
        if(length<1) return "";

        StringBuilder sb = new StringBuilder();
        for(int i=0;i<length;++i){
            if(i!=0) sb.append(",");
            sb.append("?");
        }
        return sb.toString();
    }

    protected String getInsertStatement(List<String> lstInsertKeys){
        StringBuilder sb = new StringBuilder();

        sb.append("INSERT INTO `").append(getTableName()).append("` (").append(parseToInsert(lstInsertKeys)).append(") VALUES (").append(parseToInsertValues(lstInsertKeys.size())).append(");");

        return sb.toString();
    }

    protected String getDeleteStatement(List<String> lstWhereKeys){
        StringBuilder sb = new StringBuilder();

        sb.append("DELETE FROM `").append(getTableName()).append("` WHERE ").append(parseToWhere(lstWhereKeys)).append(";");

        return sb.toString();
    }

    protected String getCountStatement(List<String> lstWhereKeys){
        StringBuilder sb = new StringBuilder();

        sb.append("SELECT COUNT(1) FROM `").append(getTableName()).append("` WHERE ").append(parseToWhere(lstWhereKeys)).append(";");

        return sb.toString();
    }

    private String parseToLike(List<String> lstSearchKeys, List<Object> lstSearchValue){
        StringBuilder sb = new StringBuilder();

        if(lstSearchKeys.isEmpty()){
            return "1==1";
        }

        for(int i=0,ni=lstSearchKeys.size();i<ni;++i){
            String key = lstSearchKeys.get(i);
            Object value = lstSearchValue.get(i);
            if(value == null) continue;

            if(sb.length()!=0) sb.append(",");
            sb.append("`").append(key).append("` ").append("LIKE '%").append(value.toString()).append("%'");
        }

        return sb.toString();
    }

    private String getLikeStatement(List<String> lstSearchKeys, List<Object> lstSearchValue){
        return new StringBuilder()
                .append("SELECT * FROM `").append(getTableName()).append("` ")
                .append("WHERE ").append(parseToLike(lstSearchKeys, lstSearchValue))
                .toString();
    }

    private String getLikeStatement(List<String> lstSearchKeys, List<Object> lstSearchValue,int page,int pageSize){
        return new StringBuilder()
                .append("SELECT * FROM `").append(getTableName()).append("` ")
                .append("WHERE ").append(parseToLike(lstSearchKeys, lstSearchValue)).append(" ")
                .append("ORDER BY ").append(parseToOrder(getPrimaryKey())).append(" ")
                .append("LIMIT ").append(pageSize).append(" OFFSET ").append(page*pageSize)
                .append(";")
                .toString();
    }

    private String getCountSearchStatement(List<String> lstSearchKeys, List<Object> lstSearchValue){
        return new StringBuilder()
                .append("SELECT COUNT(1) FROM `").append(getTableName()).append("` ")
                .append("WHERE ").append(parseToLike(lstSearchKeys, lstSearchValue))
                .toString();
    }

    public int countSearch(List<String> lstSearchKeys, List<Object> lstSearchValue){
        synchronized (BaseDatabase.class){
            Cursor cursor = getDatabase().query(getCountSearchStatement(lstSearchKeys, lstSearchValue));
            try {
                if(cursor.moveToNext()){
                    return cursor.getInt(0);
                }
            } finally {
                try { cursor.close(); } catch (Exception e) { }
            }
            return -1;
        }
    }

    public ESONArray search(List<String> lstSearchKeys, List<Object> lstSearchValue){
        synchronized (BaseDatabase.class){
            Cursor cursor = getDatabase().query(getLikeStatement(lstSearchKeys, lstSearchValue));
            return parseCursorToArray(cursor);
        }
    }

    public ESONArray search(List<String> lstSearchKeys, List<Object> lstSearchValue,int page,int pageSize){
        synchronized (BaseDatabase.class){
            Cursor cursor = getDatabase().query(getLikeStatement(lstSearchKeys, lstSearchValue,page,pageSize));
            return parseCursorToArray(cursor);
        }
    }

    public ESONArray query(List<String> lstQueryKeys, List<Object> lstQueryValue){
        synchronized (BaseDatabase.class){
            Cursor cursor = getDatabase().query(getQueryStatement(new ArrayList<>(),lstQueryKeys),lstQueryValue.toArray());
            return parseCursorToArray(cursor);
        }
    }

    public ESONArray query(List<String> lstQueryKeys, List<Object> lstQueryValue,int page,int pageSize){
        synchronized (BaseDatabase.class){
            Cursor cursor = getDatabase().query(getQueryStatement(new ArrayList<>(),lstQueryKeys,page,pageSize),lstQueryValue.toArray());
            return parseCursorToArray(cursor);
        }
    }

    protected int rawCount(List<String> lstCountKeys, List<Object> lstCountValues){
        synchronized (BaseDatabase.class){
            Cursor cursor = getDatabase().query(getCountStatement(lstCountKeys),lstCountValues.toArray());
            try {
                if(cursor.moveToNext()){
                    return cursor.getInt(0);
                }
            } finally {
                try { cursor.close(); } catch (Exception e) { }
            }
            return -1;
        }
    }

    protected boolean rawInsert(List<String> lstInsertKeys, List<Object> lstInsertValue){
        synchronized (BaseDatabase.class){
            Pair<List<String>,List<Object>> pKV = parseToPrimaryKV(lstInsertKeys, lstInsertValue);
            if(pKV==null) return false;
            try { getDatabase().execSQL(getInsertStatement(lstInsertKeys),lstInsertValue.toArray()); } catch (SQLException e) { e.printStackTrace(); }
            return rawCount(pKV.first,pKV.second)>0;
        }
    }

    protected boolean rawUpdate(List<String> lstUpdateKeys, List<Object> lstUpdateValue){
        synchronized (BaseDatabase.class){
            Pair<List<String>,List<Object>> pKV = parseToPrimaryKV(lstUpdateKeys, lstUpdateValue);
            if(pKV==null) return false;
            List<Object> objects = new ArrayList<>();
            objects.addAll(lstUpdateValue);
            objects.addAll(pKV.second);
            try { getDatabase().execSQL(getUpdateStatement(lstUpdateKeys,pKV.first),objects.toArray()); } catch (SQLException e) { e.printStackTrace(); }
            return rawCount(lstUpdateKeys,lstUpdateValue)>0;
        }
    }

    protected boolean insertOrUpdate(List<String> lstInsertKeys, List<Object> lstInsertValue){
        synchronized (BaseDatabase.class){
            if(lstInsertKeys.size() != lstInsertValue.size()) return false;

            Pair<List<String>,List<Object>> pKV = parseToPrimaryKV(lstInsertKeys, lstInsertValue);
            if(pKV == null) return false;

            if(rawCount(pKV.first, pKV.second)>0){
                return rawUpdate(lstInsertKeys,lstInsertValue);
            }
            else{
                return rawInsert(lstInsertKeys, lstInsertValue);
            }
        }
    }

    protected int delete(List<String> lstDeleteKeys, List<Object> lstDeleteValue){
        synchronized (BaseDatabase.class){
            if(lstDeleteKeys.size()!=lstDeleteValue.size()) return -1;

            int count1 = rawCount(new ArrayList<>(),new ArrayList<>());
            try { getDatabase().execSQL(getDeleteStatement(lstDeleteKeys),lstDeleteValue.toArray()); } catch (SQLException e) { }
            int count2 = rawCount(new ArrayList<>(),new ArrayList<>());

            return count1 - count2;
        }
    }

    public int clear(){
        return delete(new ArrayList<>(),new ArrayList<>());
    }

    public void count(ICountListener listener){
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            try {
                int count = rawCount(new ArrayList<>(),new ArrayList<>());
                if(count==-1){
                    post(()->listener.onFailure("页数查询失败！"));
                    return;
                }
                post(()->listener.onSuccess(count));
            } catch (Exception e) {
                e.printStackTrace();
                post(()->listener.onFailure(e.getMessage()));
            }
        });
    }

    public void count(ESONObject data, ICountListener listener){
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            Pair<List<String>,List<Object>> pKV = null;
            try {
                pKV = parseJsonToKV(data);
            } catch (Exception e) {
                post(()->listener.onFailure(e.getMessage()));
                return;
            }
            try {
                int count = rawCount(pKV.first,pKV.second);
                if(count==-1){
                    post(()->listener.onFailure("页数查询失败！"));
                    return;
                }
                post(()->listener.onSuccess(count));
            } catch (Exception e) {
                e.printStackTrace();
                post(()->listener.onFailure(e.getMessage()));
            }
        });
    }

    public void countSearch(ESONObject data, ICountListener listener){
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            Pair<List<String>,List<Object>> pKV = null;
            try {
                pKV = parseJsonToKV(data);
            } catch (Exception e) {
                post(()->listener.onFailure(e.getMessage()));
                return;
            }
            try {
                int count = countSearch(pKV.first,pKV.second);
                if(count==-1){
                    post(()->listener.onFailure("页数查询失败！"));
                    return;
                }
                post(()->listener.onSuccess(count));
            } catch (Exception e) {
                e.printStackTrace();
                post(()->listener.onFailure(e.getMessage()));
            }
        });
    }

    public void search(ESONObject data, int page, int pageSize, IGetterListener listener){
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            Pair<List<String>,List<Object>> pKV = null;
            try {
                pKV = parseJsonToKV(data);
            } catch (Exception e) {
                post(()->listener.onFailure(e.getMessage()));
                return;
            }
            try {
                ESONArray result = search(pKV.first,pKV.second,page,pageSize);
                post(()->listener.onSuccess(result));
            } catch (Exception e) {
                e.printStackTrace();
                post(()->listener.onFailure(e.getMessage()));
            }
        });
    }

    public void query(ESONObject data, int page, int pageSize, IGetterListener listener){
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            Pair<List<String>,List<Object>> pKV = null;
            try {
                pKV = parseJsonToKV(data);
            } catch (Exception e) {
                post(()->listener.onFailure(e.getMessage()));
                return;
            }

            try {
                ESONArray result = query(pKV.first,pKV.second,page,pageSize);
                post(()->listener.onSuccess(result));
            } catch (Exception e) {
                e.printStackTrace();
                post(()->listener.onFailure(e.getMessage()));
            }
        });
    }

    protected void post(Runnable r){
        App.Post(r);
    }

    protected void postDelayed(Runnable r, long delayMillis){
        App.PostDelayed(r, delayMillis);
    }

    //region 格式转换
    //数据库数据转API数据
    protected ESONArray parseCursorToArrayNoChange(Cursor cursor){
        int    count    = cursor.getColumnCount();
        String names [] = cursor.getColumnNames();
        ESONArray result = new ESONArray();
        while(cursor.moveToNext()) {
            ESONObject object = new ESONObject();
            for (int i = 0; i < count; ++i) {
                String name = names[i];
                object.putValue(name,cursor.getString(i));
            }
            result.putValue(object);
        }
        try { cursor.close(); } catch (Exception e) { }
        return result;
    }
    protected ESONArray parseCursorToArray(Cursor cursor){
        int    count    = cursor.getColumnCount();
        String names [] = cursor.getColumnNames();

        ESONArray result = new ESONArray();
        Map<String,String> mTypeMapper = new HashMap<>();
        for(ApiParam param : getConfig().getFields()){
            mTypeMapper.put(param.getName(),param.getType());
        }
        Map<String,ApiParam> mFieldsMapper = getConfig().getFieldMapper();
        while(cursor.moveToNext()){
            ESONObject object = new ESONObject();
            for(int i=0;i<count;++i){
                String name = names[i];
                ApiParam p = mFieldsMapper.get(name);
                if(p == null) continue;//NOT POSSIBLE
                String type = p.getType();
                if(type == null) continue;//NOT POSSIBLE
                type = type.toUpperCase();
                String defaultValue = p.getDefaultValue();

                int cursorResult = cursor.getType(i);
                boolean isNull   = cursorResult == Cursor.FIELD_TYPE_NULL;

                switch (type){
                    case "INT":
                        Integer iDef = null;
                        if(isNull){ }
                        else{
                            iDef = cursor.getInt(i);
                        }

                        if(iDef == null && !"null".equals(defaultValue)){
                            try { iDef = Integer.valueOf(defaultValue); } catch (Exception e) { }
                        }

                        object.putValue(names[i],iDef);
                        break;
                    case "FLOAT":
                    case "DOUBLE":
                        Double dDef = null;
                        if(isNull){ }
                        else{
                            dDef = cursor.getDouble(i);
                        }

                        if(dDef == null && !"null".equals(defaultValue)){
                            try { dDef = Double.valueOf(defaultValue); } catch (Exception e) { }
                        }

                        object.putValue(names[i],dDef);
                        break;
                    case "BOOL":
                    case "BOOLEAN":
                        Boolean bDef = null;
                        if(isNull){ }
                        else{
                            bDef = cursor.getInt(i) != 0;
                        }

                        if(bDef == null && !"null".equals(defaultValue)){
                            try { bDef = Boolean.parseBoolean(defaultValue); } catch (Exception e) { }
                            if("0".equals(defaultValue) || "false".equalsIgnoreCase(defaultValue)){
                                bDef = false;
                            }
                            else if(bDef == null){
                                bDef = true;
                            }
                        }

                        object.putValue(names[i],bDef);
                        break;
                    case "DATE"://2022-03-23 07:51:38
                    case "STRING":
                    default:
                        String sDef = null;
                        if(isNull){}
                        else{
                            sDef = cursor.getString(i);
                        }

                        if(type.startsWith("DATE:") && sDef == null){
                            if("NOW".equalsIgnoreCase(defaultValue) || "CURRENT_TIMESTAMP".equalsIgnoreCase(defaultValue)){
                                String fmt  = type.substring(5);
                                SimpleDateFormat format = new SimpleDateFormat(fmt);
                                try { sDef = format.format(new Date(System.currentTimeMillis())); } catch (Exception e) { }
                            }
                        }

                        if(!type.startsWith("DATE:") &&sDef == null && !"null".equals(defaultValue)){
                            sDef = defaultValue;
                        }

                        object.putValue(names[i],sDef);
                        break;
                }

            }
            result.putValue(object);
        }
        try { cursor.close(); } catch (Exception e) { }
        return result;
    }

    //将JSON转换成数据库存储键值对
    protected Pair<List<String>,List<Object>> parseJsonToKV(ESONObject data) throws Exception{
        List<String> keys   = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        if(data != null){
            Map<String,ApiParam> mFieldsMapper = getConfig().getFieldMapper();
            for (Iterator<String> it = data.keys(); it.hasNext(); ) {
                String key   = it.next();
                ApiParam p = mFieldsMapper.get(key);
                if(p == null) continue;
                String type  = p.getType();
                String defaultValue = p.getDefaultValue();

                Object value = null;
                try { value = data.get(key); } catch (Exception e) { }

                switch (type.trim().toUpperCase().split(":")[0]){
                    case ApiParam.TYPE_DATE:
                        String date = null;
                        String fmt  = type.substring(5);
                        SimpleDateFormat format = new SimpleDateFormat(fmt);

                        if(value == null){}
                        else{
                            try { date = format.format(format.parse((String)value)); } catch (Exception e) { value = null; }
                        }

                        if(date == null){
                            if(defaultValue == null){
                                date = null;
                            }
                            else if("NOW".equalsIgnoreCase(defaultValue) || "CURRENT_TIMESTAMP".equalsIgnoreCase(defaultValue)){
                                try { date = format.format(new Date(System.currentTimeMillis())); } catch (Exception e) { value = null; }
                            }
                        }

                        if(date == null && "NOEMPTY".equals(defaultValue)){
                            throw new InvalidParameterException("字段["+p.getDescription()+"]不能为空！");
                        }

                        keys.add(key);
                        values.add(date);
                        break;
                    case ApiParam.TYPE_STRING:
                        String string = null;

                        if(value!= null){
                            String sSetter = p.setter(value.toString());
                            if(sSetter != null){
                                string = sSetter;
                            }
                        }

                        if(string==null){
                            if(value == null){}
                            else{
                                string = value.toString();
                            }

                            if(string == null && ApiParam.DEFAULT_NULL_STR.equals(defaultValue)){
                                string = defaultValue;
                            }

                            if(string == null && ApiParam.DEFAULT_NO_EMPTY.equals(defaultValue)){
                                throw new InvalidParameterException("字段["+p.getDescription()+"]不能为空！");
                            }

                            string = defaultValue;
                        }

                        keys.add(key);
                        values.add(string);
                        break;
                    case ApiParam.TYPE_DOUBLE:
                        Double dDef = null;

                        if(value!=null){
                            String dSetter = p.setter(value.toString());
                            if(dSetter != null && value!=null){
                                try { dDef = Double.parseDouble(dSetter); } catch (Exception e) { }
                            }
                        }

                        if(dDef == null){
                            if(value == null){}
                            else if(value.getClass().equals(double.class) || value instanceof Double){
                                dDef = (Double) value;
                            }
                            else{
                                try { dDef = Double.parseDouble(value.toString()); } catch (Exception e) { }
                            }

                            if(dDef == null){
                                try { dDef = Double.parseDouble(defaultValue); } catch (Exception e) { }
                            }

                            if(dDef == null && ApiParam.DEFAULT_NO_EMPTY.equals(defaultValue)){
                                throw new InvalidParameterException("字段["+p.getDescription()+"]不能为空！");
                            }
                        }

                        keys.add(key);
                        values.add(dDef);
                        break;
                    case ApiParam.TYPE_BOOLEAN:
                        Boolean b = null;

                        String bSetter = p.setter(value.toString());
                        if(bSetter != null && value!=null){
                            try { b = Boolean.parseBoolean(bSetter); } catch (Exception e) { }
                        }

                        if(b==null){
                            if(value == null){ }
                            else if(value.getClass().equals(boolean.class) || value instanceof Boolean){
                                b = (boolean) value;
                            }
                            else if(value.toString().equals("FALSE") || value.toString().equals("0")){
                                b = false;
                            }

                            if(b!=null && !"null".equals(defaultValue)){
                                try { b = Boolean.parseBoolean(value.toString()); } catch (Exception e) { }
                            }

                            if(b == null && ApiParam.DEFAULT_NO_EMPTY.equals(defaultValue)){
                                throw new InvalidParameterException("字段["+p.getDescription()+"]不能为空！");
                            }

                        }

                        keys.add(key);
                        values.add(b);
                        break;
                    case ApiParam.TYPE_INTEGER:
                        Integer i = null;

                        if(value!=null){
                            String iSetter = p.setter(value.toString());
                            if(iSetter != null && value!=null){
                                try { i = Integer.parseInt(iSetter); } catch (Exception e) { }
                            }
                        }

                        if(i==null){
                            if(value == null){ }
                            else if(value.getClass().equals(int.class) || value instanceof Integer){
                                i = (Integer) value;
                            }
                            else {
                                try { i = Integer.parseInt(value.toString()); } catch (Exception e) { }
                            }
                            if(i == null && !"null".equals(defaultValue)){
                                try { i = Integer.parseInt(defaultValue); } catch (Exception e) { }
                            }

                            if(i == null && ApiParam.DEFAULT_NO_EMPTY.equals(defaultValue)){
                                throw new InvalidParameterException("字段["+p.getDescription()+"]不能为空！");
                            }
                        }

                        keys.add(key);
                        values.add(i);
                        break;
                    case ApiParam.TYPE_LONG:
                        Long l = null;

                        if(value!=null){
                            String lSetter = p.setter(value.toString());
                            if(lSetter != null && value!=null){
                                try { l = Long.parseLong(lSetter); } catch (Exception e) { }
                            }
                        }

                        if(l == null){
                            if(value == null){ }
                            else if(value.getClass().equals(long.class) || value instanceof Long){
                                l = (Long) value;
                            }
                            else {
                                l = Long.parseLong(value.toString());
                            }

                            if(l == null && !"null".equals(defaultValue)){
                                try { l = Long.parseLong(defaultValue); } catch (Exception e) { }
                            }

                            if(l == null && ApiParam.DEFAULT_NO_EMPTY.equals(defaultValue)){
                                throw new InvalidParameterException("字段["+p.getDescription()+"]不能为空！");
                            }
                        }

                        keys.add(key);
                        values.add(l);
                        break;
                }
            }
        }

        return new Pair(keys,values);
    }

    protected Pair<List<String>,List<Object>> parseToPrimaryKV(List<String> lstKeys, List<Object> lstValue){
        List<String> lstPrimaryKeys     = getPrimaryKey();
        List<Object> lstPrimaryValues   = new ArrayList<>();
        Map<String,Object> mInsertMapper = new HashMap<>();
        for(int i=0,ni=lstKeys.size();i<ni;++i){
            mInsertMapper.put(lstKeys.get(i),lstValue.get(i));
        }

        for(int i=0,ni=lstPrimaryKeys.size();i<ni;++i){
            String key   = lstPrimaryKeys.get(i);
            Object value = mInsertMapper.get(key);
            if(value==null) return null;
            lstPrimaryValues.add(value);
        }

        return new Pair(lstPrimaryKeys,lstPrimaryValues);
    }
    //endregion
}