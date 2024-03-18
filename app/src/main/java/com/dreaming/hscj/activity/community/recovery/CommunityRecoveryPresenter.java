package com.dreaming.hscj.activity.community.recovery;

import android.util.Log;
import android.util.Pair;

import com.dreaming.hscj.App;
import com.dreaming.hscj.Constants;
import com.dreaming.hscj.core.ThreadPoolProvider;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.template.database.impl.DaySamplingLogDatabase;
import com.dreaming.hscj.template.database.impl.NoneGroupingDatabase;
import com.dreaming.hscj.template.database.impl.NucleicAcidGroupingDatabase;
import com.dreaming.hscj.template.database.impl.UserOverallDatabase;
import com.dreaming.hscj.template.database.wrapper.BaseDatabase;
import com.dreaming.hscj.template.database.wrapper.DatabaseConfig;
import com.dreaming.hscj.template.database.wrapper.ISetterListener;
import com.dreaming.hscj.utils.FileUtils;
import com.dreaming.hscj.utils.JsonUtils;
import com.dreaming.security.Defender;

import net.lingala.zip4j.ZipFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

public class CommunityRecoveryPresenter extends ICommunityRecoveryContract.Presenter{
    private static final String TAG = CommunityRecoveryPresenter.class.getSimpleName();

    public String sIdCardNo = "";
    public String sName = "";
    public String sGroupId = "";

    //region 设置
    private String path1 = App.sInstance.getExternalCacheDir().getAbsolutePath();
    private String path2 = path1.lastIndexOf("Android") >-1 ? path1.substring(0,path1.lastIndexOf("Android")) : "/storage/emulated/0";

    public final String sQQRootPath = path2+"/tencent/QQfile_recv";
    public final String sWXRootPath = path2+"/tencent/MicroMsg/Download";
    public final String sRootPath   = path2;

    private int target   = 0;
    private int mode     = 0;
    private int priority = 0;
    private int path     = 0;


    @Override
    void setTarget(int target) {
        this.target = target;
    }

    @Override
    void setMode(int mode) {
        this.mode = mode;
    }

    @Override
    void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    void setPath(int path) {
        this.path = path;
    }

    @Override
    int getTarget() {
        return target;
    }

    @Override
    int getMode() {
        return mode;
    }

    @Override
    int getPriority() {
        return priority;
    }

    @Override
    int getPath() {
        return path;
    }

    private String defaultVillageName = "";
    private String defaultTownName = "";

    @Override
    String getDefaultVillageName() {
        return defaultVillageName;
    }
    @Override
    String getDefaultTownName() {
        return defaultTownName;
    }

    int iSelectedDatabaseId = -1;
    @Override
    void setSelectedDatabaseId(int id){
        iSelectedDatabaseId = id;
    }
    @Override
    int getSelectedDatabaseId(){
        return iSelectedDatabaseId;
    }

    private String selectedVillageName = "";
    private String selectedTownName = "";
    @Override
    String getSelectedVillageName() {
        return selectedVillageName;
    }
    @Override
    String getSelectedTownName() {
        return selectedTownName;
    }
    @Override
    void setSelectedVillageName(String name) {
        selectedVillageName = name;
    }
    @Override
    void setSelectedTownName(String name) {
        selectedTownName = name;
    }

    private String diyVillageName = "";
    private String diyTownName = "";
    @Override
    String getDiyVillageName() {
        return diyVillageName;
    }
    @Override
    String getDiyTownName() {
        return diyTownName;
    }
    @Override
    void setDiyVillageName(String name) {
        diyVillageName = name;
    }
    @Override
    void setDiyTownName(String name) {
        diyTownName = name;
    }

    //0初始化 1选择 2解压 3验证 4读取 5冲突及失败处理 6成功
    public final int PROGRESS_INIT = 0;
    public final int PROGRESS_SELECT = 1;
    public final int PROGRESS_EXTRACT = 2;
    public final int PROGRESS_VERIFY = 3;
    public final int PROGRESS_READ = 4;
    public final int PROGRESS_PROCESSING = 5;
    public final int PROGRESS_SUCCESS = 6;
    private int progress = 0;
    @Override
    int getProgress() {
        return progress;
    }



    String getTownName(){
        switch (target){
            case 0:
                return defaultTownName;
            case 1:
                return selectedTownName;
            case 2:
                return diyTownName;
        }
        return "";
    }

    String getVillageName(){
        switch (target){
            case 0:
                return defaultVillageName;
            case 1:
                return selectedVillageName;
            case 2:
                return diyVillageName;
        }
        return "";
    }
    //endregion

    private Template template;

    UserOverallDatabase         getDb2Local(){
        template.setUseTempDbPath(false,"","");
        return template.getUserOverallDatabase();
    }
    UserOverallDatabase         getDb2Recovery(){
        template.setUseTempDbPath(true,getTownName(),getVillageName());
        return template.getUserOverallDatabase();
    }
    NucleicAcidGroupingDatabase getDb3Local   (){
        template.setUseTempDbPath(false,"","");
        return template.getNucleicAcidGroupingDatabase();
    }
    NucleicAcidGroupingDatabase getDb3Recovery(){
        template.setUseTempDbPath(true,getTownName(),getVillageName());
        return template.getNucleicAcidGroupingDatabase();
    }
    DaySamplingLogDatabase      getDb5Local   (){
        template.setUseTempDbPath(false,"","");
        return template.getDaySamplingLogDatabase();
    }
    DaySamplingLogDatabase      getDb5Recovery(){
        template.setUseTempDbPath(true,getTownName(),getVillageName());
        return template.getDaySamplingLogDatabase();
    }

    File fExtractDir;
    File fDbTempDir;
    @Override
    void start(String path) {
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            synchronized (CommunityRecoveryPresenter.class){

                Constants.Config.setRecoveryDbConfig(new ESONObject().putValue("mode",mode).putValue("priority",priority).putValue("path",getPath()));

                template = null;
                progress = 1;

                File fPath = new File(path);
                fExtractDir = new File(App.sInstance.getCacheDir(),"/recovery/"+fPath.getName());

                //解压
                try {
                    File fPkgInfo =  new File(fExtractDir,"package.info");
                    ZipFile zipFile = new ZipFile(fPath);
                    zipFile.extractFile("package.info", fExtractDir.getAbsolutePath());
                    Log.e(TAG,"extract->package.info:"+fPkgInfo.getAbsolutePath());
                    String info = FileUtils.readAll(fPkgInfo.getAbsolutePath()).trim();
                    Log.e(TAG,"extract->info:"+info);
                    String password = new Defender().decrypt(info);
                    Log.e(TAG,"extract->pwd:"+password);
                    zipFile = new ZipFile(fPath,password.toCharArray());
                    zipFile.extractAll(fExtractDir.getAbsolutePath());
                    App.Post(()->mView.onExtractSuccess());
                    progress = 2;
                } catch (Exception e) {
                    progress = 0;
                    App.Post(()->mView.onExtractFailure(e.getMessage()));
                    return;
                }

                //查找*.template
                File []fChild = fExtractDir.listFiles();
                if(fChild!=null){
                    for(File f:fChild){
                        if(f.getName().endsWith(".template")){
                            try {
                                template = Template.read(f.getAbsolutePath());
                            } catch (Exception e) {
                                App.Post(()->mView.onVerifyFailure("模板验证失败！"));
                                return;
                            }
                            break;
                        }
                    }
                }
                boolean bIsUseDefaultTemplate = template == null ||
                        template.getDatabaseSetting().getUnifySocialCreditCodes().equals(Template.getDefault().getDatabaseSetting().getUnifySocialCreditCodes()) &&
                        template.getDatabaseSetting().getRegionCode().equals(Template.getDefault().getDatabaseSetting().getRegionCode());

                if(bIsUseDefaultTemplate){
                    if(template !=null){
                        if(Template.getDefault().getDatabaseSetting().getNetApiVersion()> template.getDatabaseSetting().getNetApiVersion()){
                            template = Template.getDefault();
                        }
                    }
                    else{
                        template = Template.getDefault();
                    }
                }
                else{
                    Template temp = Template.readByUnify(template.getDatabaseSetting().getRegionCode(),template.getDatabaseSetting().getUnifySocialCreditCodes(),template.getDatabaseSetting().getNetApiVersion());
                    if(temp != null) template = temp;
                }

                File fDbInfo = new File(fExtractDir,"database.info");
                ESONObject eDbInfo = new ESONObject(FileUtils.readAll(fDbInfo.getAbsolutePath()));
                defaultTownName    = eDbInfo.getJSONValue("townName"   ,"");
                defaultVillageName = eDbInfo.getJSONValue("villageName","");
                progress = 3;
                App.Post(()->mView.onVerifySuccess());
            }
        });
    }

    @Override
    List<ESONObject> getSelectDatabaseList() {
        return JsonUtils.parseToList(Constants.DBConfig.getAllDatabase((template ==null?Template.getDefault(): template).getDatabaseSetting().getSPUnify()));
    }

    @Override
    int createDatabase(String townName, String villageName) {
        return Constants.DBConfig.addDatabase(template.getDatabaseSetting().getSPUnify(),townName,villageName);
    }

    @Override
    void read() {
        Log.e(TAG,"townName:"+getTownName());
        Log.e(TAG,"villageName:"+getVillageName());
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            fDbTempDir = new File(
                    BaseDatabase.getDatabaseDir(
                            template.getDatabaseSetting().getUnifySocialCreditCodes(),
                            template.getTempCode(),
                            template.getDatabaseSetting().getRegionName(),
                            getTownName(),
                            getVillageName()
                    )
            );
            Log.e(TAG,"read->db temp dir:"+fDbTempDir.getAbsolutePath());

            lstReadMemberLocal.clear();
            mReadMemberLocalMapper.clear();
            lstReadMemberRecovery.clear();
            mReadMemberRecoveryMapper.clear();
            lstReadMemberDuplicated.clear();
            mReadMemberDuplicatedMapper.clear();

            lstReadGroupLocal.clear();
            mReadGroupLocalMapper.clear();
            lstReadGroupRecovery.clear();
            mReadGroupRecoveryMapper.clear();
            lstReadGroupDuplicated.clear();
            mReadGroupDuplicatedMapper.clear();

            lstReadLogLocal.clear();
            mReadLogLocalMapper.clear();
            lstReadLogRecovery.clear();
            mReadLogRecoveryMapper.clear();
            lstReadLogDuplicated.clear();
            mReadLogDuplicatedMapper.clear();


            Log.e(TAG,"read->mode:"+mode);
            Log.e(TAG,"read->priority"+priority);
            Log.e(TAG,"read->target"+target);
            try {
                App.Post(()->mView.onProcess("正在拷贝数据库..."));

                FileUtils.copy(fExtractDir,fDbTempDir);

                sIdCardNo = getDb3Local().getCardIdFieldName();
                sName     = getDb3Local().getNameFiledName();
                sGroupId  = getDb3Local().getGroupIdFieldName();

                App.Post(()->mView.onProcess("正在连接数据库..."));

                countAll();

                App.Post(()->mView.onProcess(String.format("正在读取，总共%d条待读取记录。",iCountAll)));

                readLocalMember();
                readRecoveryMember();
                readLocalGroup();
                readRecoveryGroup();
                readLocalLog();
                readRecoveryLog();

                progress = 4;

                FileUtils.delete(fExtractDir);
                App.Post(()->mView.onReadSuccess());
            } catch (Exception e) {
                e.printStackTrace();
                App.Post(()->mView.onReadError(e.getMessage()));
            }
        });
    }

    int iCountAll = 0;
    void countAll(){
        countLocal();
        countRecovery();
        iCountAll = iCountLocal + iCountRecovery;
    }

    int iCountLocal = 0;
    void countLocal(){
        int count2 = getDb2Local().countSync();
        count2 = Math.max(0,count2);
        Log.e(TAG,"count local 2->"+count2);
        int count3 = getDb3Local().countSync();
        count3 = Math.max(0,count3);
        Log.e(TAG,"count local 3->"+count3);
        int count4 = getDb5Local().countSync();
        count4 = Math.max(0,count4);
        Log.e(TAG,"count local 4->"+count4);
        iCountLocal = count2 + count3 + count4;
    }

    int iCountRecovery = 0;
    void countRecovery(){
        int count2 = getDb2Recovery().countSync();
        count2 = Math.max(0,count2);
        Log.e(TAG,"count recovery 4->"+count2);
        int count3 = getDb3Recovery().countSync();
        count3 = Math.max(0,count3);
        Log.e(TAG,"count recovery 4->"+count3);
        int count4 = getDb5Recovery().countSync();
        count4 = Math.max(0,count4);
        Log.e(TAG,"count recovery 4->"+count4);
        iCountRecovery = count2 + count3 + count4;
    }

    final private List<ESONObject> lstReadMemberLocal = new ArrayList<>();
    final private Map<String,ESONObject> mReadMemberLocalMapper = new HashMap<>();
    void readLocalMember(){
        App.Post(()->mView.onProcess("正在读取本地成员数据..."));
        Log.e(TAG,"call readLocalMember");

        UserOverallDatabase db2 = getDb2Local();
        ESONArray result = db2.query(new ArrayList<>(),new ArrayList<>());

        Log.e(TAG,"readLocalMember->length:"+result.length());
        for(int i=0,ni=result==null?0:result.length();i<ni;++i){
            final int progress = i*100/iCountAll;

            App.Post(()->mView.onReading(progress));

            ESONObject item = result.getArrayValue(i,new ESONObject());
            String idCardNo = item.getJSONValue(db2.getIdFieldName(),"").trim();
            if(idCardNo.isEmpty()) continue;
            mReadMemberLocalMapper.put(idCardNo,item);
            lstReadMemberLocal.add(item);

            App.Post(()->mView.onReadingMemberLocalCount(lstReadMemberLocal.size()));
        }
    }

    final private List<ESONObject> lstReadMemberRecovery = new ArrayList<>();
    final private Map<String,ESONObject> mReadMemberRecoveryMapper = new HashMap<>();
    void readRecoveryMember(){
        App.Post(()->mView.onProcess("正在读取备份成员数据..."));
        Log.e(TAG,"call readRecoveryMember");

        UserOverallDatabase db2 = getDb2Recovery();
        ESONArray result = db2.query(new ArrayList<>(),new ArrayList<>());

        int processed = lstReadMemberLocal.size();
        Log.e(TAG,"readRecoveryMember->length:"+result.length());
        for(int i=0,ni=result==null?0:result.length();i<ni;++i){
            final int progress = (processed+i)*100/iCountAll;
            App.Post(()->mView.onReading(progress));

            ESONObject item = result.getArrayValue(i,new ESONObject());
            String idCardNo = item.getJSONValue(db2.getIdFieldName(),"").trim();
            if(idCardNo.isEmpty()) continue;
            mReadMemberRecoveryMapper.put(idCardNo,item);
            lstReadMemberRecovery.add(item);

            App.Post(()->mView.onReadingMemberRecoveryCount(lstReadMemberRecovery.size()));

            ESONObject eLocal = mReadMemberLocalMapper.get(idCardNo);
            if(eLocal == null) continue;
            lstReadMemberDuplicated.add(new Pair<>(eLocal,item));
            mReadMemberDuplicatedMapper.put(idCardNo,new Pair(eLocal,item));

            App.Post(()->mView.onReadingMemberFailureCount(lstReadMemberDuplicated.size()));
        }
    }

    @Override
    List<ESONObject> getReadMemberLocal() {
        return lstReadMemberLocal;
    }

    @Override
    List<ESONObject> getReadMemberRecovery() {
        return lstReadMemberRecovery;
    }

    final private List<Pair<ESONObject,ESONObject>> lstReadMemberDuplicated = new ArrayList<>();
    final private Map<String,Pair<ESONObject,ESONObject>> mReadMemberDuplicatedMapper = new HashMap<>();
    @Override
    List<Pair<ESONObject, ESONObject>> getReadMemberDuplicated() {
        return lstReadMemberDuplicated;
    }

    final private List<ESONObject> lstReadGroupLocal = new ArrayList<>();
    final private Map<String,ESONObject> mReadGroupLocalMapper = new HashMap<>();
    void readLocalGroup(){
        App.Post(()->mView.onProcess("正在读取本地分组数据..."));
        Log.e(TAG,"call readLocalGroup");

        NucleicAcidGroupingDatabase db3 = getDb3Local();
        ESONArray result = db3.query(new ArrayList<>(),new ArrayList<>());

        int processed = lstReadMemberLocal.size()+lstReadMemberRecovery.size();
        Log.e(TAG,"readLocalGroup->length:"+result.length());
        for(int i=0,ni=result==null?0:result.length();i<ni;++i){
            final int progress = (processed+i)*100/iCountAll;
            App.Post(()->mView.onReading(progress));

            ESONObject item = result.getArrayValue(i,new ESONObject());
            String idCardNo = item.getJSONValue(db3.getCardIdFieldName(),"").trim();
            if(idCardNo.isEmpty()) continue;
            mReadGroupLocalMapper.put(idCardNo,item);
            lstReadGroupLocal.add(item);

            App.Post(()->mView.onReadingGroupLocalCount(lstReadGroupLocal.size()));
        }
    }
    final private List<ESONObject> lstReadGroupRecovery = new ArrayList<>();
    final private Map<String,ESONObject> mReadGroupRecoveryMapper = new HashMap<>();
    void readRecoveryGroup(){
        App.Post(()->mView.onProcess("正在读取备份分组数据..."));
        Log.e(TAG,"call readRecoveryGroup");

        NucleicAcidGroupingDatabase db3 = getDb3Recovery();
        ESONArray result = db3.query(new ArrayList<>(),new ArrayList<>());

        int processed = lstReadMemberLocal.size()+lstReadMemberRecovery.size()+lstReadGroupLocal.size();
        Log.e(TAG,"readRecoveryGroup->length:"+result.length());
        for(int i=0,ni=result==null?0:result.length();i<ni;++i){
            final int progress = (processed+i)*100/iCountAll;
            App.Post(()->mView.onReading(progress));

            ESONObject item = result.getArrayValue(i,new ESONObject());
            String idCardNo = item.getJSONValue(db3.getCardIdFieldName(),"").trim();
            if(idCardNo.isEmpty()) continue;
            mReadGroupRecoveryMapper.put(idCardNo,item);
            lstReadGroupRecovery.add(item);

            App.Post(()->mView.onReadingGroupRecoveryCount(lstReadGroupRecovery.size()));

            ESONObject eLocal = mReadGroupLocalMapper.get(idCardNo);
            if(eLocal == null) continue;
            lstReadGroupDuplicated.add(new Pair<>(eLocal,item));
            mReadGroupDuplicatedMapper.put(idCardNo,new Pair(eLocal,item));

            App.Post(()->mView.onReadingGroupFailureCount(lstReadGroupDuplicated.size()));
        }
    }

    final private List<ESONObject> lstReadLogLocal = new ArrayList<>();
    final private Map<String,ESONObject> mReadLogLocalMapper = new HashMap<>();
    void readLocalLog(){
        App.Post(()->mView.onProcess("正在读取本地采样记录数据..."));
        Log.e(TAG,"call readLocalLog");

        DaySamplingLogDatabase db5 = getDb5Local();
        ESONArray result = db5.query(new ArrayList<>(),new ArrayList<>());

        int processed = lstReadMemberLocal.size()+lstReadMemberRecovery.size()+lstReadGroupLocal.size()+lstReadGroupRecovery.size();
        Log.e(TAG,"readLocalLog->length:"+result.length());
        for(int i=0,ni=result==null?0:result.length();i<ni;++i){
            final int progress = (processed+i)*100/iCountAll;
            App.Post(()->mView.onReading(progress));

            ESONObject item = result.getArrayValue(i,new ESONObject());
            String idCardNo = item.getJSONValue(db5.getCardIdFieldName(),"").trim();
            if(idCardNo.isEmpty()) continue;
            mReadLogLocalMapper.put(idCardNo,item);
            lstReadLogLocal.add(item);
        }
    }
    final private List<ESONObject> lstReadLogRecovery = new ArrayList<>();
    final private Map<String,ESONObject> mReadLogRecoveryMapper = new HashMap<>();
    void readRecoveryLog(){
        App.Post(()->mView.onProcess("正在读取备份采样记录数据..."));
        Log.e(TAG,"call readRecoveryLog");

        DaySamplingLogDatabase db5 = getDb5Recovery();
        ESONArray result = db5.query(new ArrayList<>(),new ArrayList<>());

        int processed = lstReadMemberLocal.size()+lstReadMemberRecovery.size()+lstReadGroupLocal.size()+lstReadGroupRecovery.size()+lstReadLogLocal.size();
        Log.e(TAG,"readRecoveryLog->length:"+result.length());
        for(int i=0,ni=result==null?0:result.length();i<ni;++i){
            final int progress = (processed+i)*100/iCountAll;
            App.Post(()->mView.onReading(progress));

            ESONObject item = result.getArrayValue(i,new ESONObject());
            String idCardNo = item.getJSONValue(db5.getCardIdFieldName(),"").trim();
            if(idCardNo.isEmpty()) continue;
            mReadLogRecoveryMapper.put(idCardNo,item);
            lstReadLogRecovery.add(item);

            ESONObject eLocal = mReadLogLocalMapper.get(idCardNo);
            if(eLocal == null) continue;
            lstReadLogDuplicated.add(new Pair<>(eLocal,item));
            mReadLogDuplicatedMapper.put(idCardNo,new Pair<>(eLocal,item));
        }
    }
    final private List<Pair<ESONObject,ESONObject>> lstReadLogDuplicated = new ArrayList<>();
    final private Map<String,Pair<ESONObject,ESONObject>> mReadLogDuplicatedMapper = new HashMap<>();

    @Override
    List<ESONObject> getReadGroupLocal() {
        return lstReadGroupLocal;
    }

    @Override
    List<ESONObject> getReadGroupRecovery() {
        return lstReadGroupRecovery;
    }

    final private List<Pair<ESONObject,ESONObject>> lstReadGroupDuplicated = new ArrayList<>();
    final private Map<String,Pair<Integer,Integer>> mReadGroupDuplicatedMapper = new HashMap<>();
    @Override
    List<Pair<ESONObject, ESONObject>> getReadGroupDuplicated() {
        return lstReadGroupDuplicated;
    }

    final private List<ESONObject> lstSaveMemberSuccess = new ArrayList<>();
    @Override
    List<ESONObject> getSaveMemberSuccess() {
        return mode == 0 ? lstSaveMemberSuccess : lstReadMemberRecovery;
    }

    final private List<ESONObject> lstSaveMemberFailure = new ArrayList<>();
    @Override
    List<ESONObject> getSaveMemberFailure() {
        return lstSaveMemberFailure;
    }

    final private List<ESONObject> lstSaveGroupSuccess = new ArrayList<>();
    @Override
    List<ESONObject> getSaveGroupSuccess() {
        return mode == 0 ? lstSaveGroupSuccess : lstReadGroupRecovery;
    }

    final private List<ESONObject> lstSaveGroupFailure = new ArrayList<>();
    @Override
    List<ESONObject> getSaveGroupFailure() {
        return lstSaveGroupFailure;
    }

    volatile int iSaveMemberIndex = 0;
    volatile int iSaveGroupIndex  = 0;
    void autoSave(){
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            iSaveMemberIndex = 0;
            iSaveGroupIndex  = 0;

            lstSaveMemberSuccess.clear();
            lstSaveMemberFailure.clear();
            mSaveMemberRetry.clear();

            lstSaveGroupSuccess.clear();
            lstSaveGroupFailure.clear();
            mSaveGroupRetry.clear();

            mSaveLogRetry.clear();

            if(isOverride()){
                getDb2Local().clear();
                getDb3Local().clear();
                getDb5Local().clear();
            }

            App.Post(()->mView.onProcess("开始保存成员数据..."));

            Constants.DBConfig.addDatabase(getTownName(),getVillageName());

            if(mode == 1){
                template.getDatabaseTemplate().closeDbAndDelete(
                        0L,
                        template.getDatabaseSetting().getRegionName(),
                        getTownName(),
                        getVillageName()
                );
                String path = BaseDatabase.getDatabaseDir(
                        template.getDatabaseSetting().getUnifySocialCreditCodes(),
                        0L,
                        template.getDatabaseSetting().getRegionName(),
                        getTownName(),
                        getVillageName()
                );

                try {
                    App.Post(()->mView.onProcess("正在覆盖文件..."));
                    FileUtils.copy(fDbTempDir,new File(path));
                    App.Post(()->mView.onProcess("正在删除缓存文件..."));
                    FileUtils.delete(fDbTempDir);
                    App.Post(()->mView.onProcess("覆盖成功！"));
                    App.Post(()->mView.onSaving(100));

                    progress = 6;

                    App.Post(()->mView.onSavingMemberSuccessCount(lstReadMemberRecovery.size()));
                    App.Post(()->mView.onSavingGroupSuccessCount (lstReadGroupRecovery .size()));
                    App.Post(()->mView.onSaveSuccess());
                } catch (Exception e) {
                    e.printStackTrace();
                    App.Post(()->mView.onSaveFailure(e.getMessage()));
                }
            }
            else{
                autoSaveMember();
            }

        });
    }

    boolean isOverride(){
        return mode == 1;
    }

    boolean isRecoveryFirst(){
        return mode == 0 || priority == 0;
    }

    boolean isLocalFirst(){
        return mode == 0 && priority == 1;
    }

    boolean isUserDetermine(){
        return mode == 0 && priority == 2;
    }

    void autoSaveMember(){
        if(iSaveMemberIndex == lstReadMemberRecovery.size()) {
            autoSaveGroup();
            return;
        }
        final int index = mReadMemberRecoveryMapper.size() - lstReadMemberRecovery.size() + 1;
        final int progress = index * 100 / iCountRecovery;
        App.Post(()->mView.onProcess(String.format("正在保存#%d成员数据...",index)));
        App.Post(()->mView.onSaving(progress));
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            ESONObject e = lstReadMemberRecovery.get(iSaveMemberIndex);
            List<ESONObject> lstTemp = new ArrayList<>();
            String idCardNo = e.getJSONValue(sIdCardNo,"");

            ISetterListener listener = new ISetterListener() {
                @Override
                public void onSuccess() {
                    ++iSaveMemberIndex;
                    lstSaveMemberSuccess.add(lstTemp.get(0));
                    App.Post(()->mView.onSavingMemberSuccessCount(lstSaveMemberSuccess.size()));
                    autoSaveMember();
                }

                @Override
                public void onFailure(String err) {
                    Integer retry = mSaveMemberRetry.get(idCardNo);
                    if(retry == null) retry = 0;
                    mSaveMemberRetry.put(idCardNo,retry+1);

                    if(retry<3){
                        final int i = retry;
                        App.Post(()->mView.onProcess(String.format("保存#%d成员失败，稍后尝试第%d次！",index,i+1)));
                        App.PostDelayed(()->autoSaveMember(),500L);
                        return;
                    }

                    App.Post(()->mView.onProcess(String.format("保存#%d成员失败，跳过！",index)));

                    lstSaveMemberFailure.add(lstTemp.get(0));
                    ++iSaveMemberIndex;
                    autoSaveMember();
                    App.Post(()->mView.onSavingMemberFailureCount(lstSaveMemberFailure.size()));
                }
            };

            if(isOverride() || isRecoveryFirst()){
                Log.e(TAG,"autoSaveMember->isRecoveryFirst");
                lstTemp.add(e);
                saveMember(e,listener);
                return;
            }
            if(isLocalFirst()){
                Log.e(TAG,"autoSaveMember->isLocalFirst");
                if(mReadMemberDuplicatedMapper.containsKey(idCardNo)){
                    e = mReadMemberLocalMapper.get(idCardNo);
                }
                lstTemp.add(e);
                saveMember(e,listener);
                return;
            }
            if(isUserDetermine()){
                Log.e(TAG,"autoSaveMember->isUserDetermine");
                if(mReadMemberDuplicatedMapper.containsKey(idCardNo)){
                    ++iSaveMemberIndex;
                    autoSaveMember();
                }
                else{
                    lstTemp.add(e);
                    saveMember(e,listener);
                }
            }
        });
    }

    private Map<String,Integer> mSaveMemberRetry = new HashMap<>();
    @Override
    void saveMember(ESONObject data, ISetterListener listener) {
        getDb2Local().addPeople(data,listener);
    }

    void autoSaveGroup(){
        if(iSaveGroupIndex == lstReadGroupRecovery.size()) {
            autoSaveLog();
            return;
        }

        final int index = mReadGroupRecoveryMapper.size() - lstReadGroupRecovery.size() + 1;
        final int progress = (index + mReadMemberRecoveryMapper.size()) * 100 / iCountRecovery;
        App.Post(()->mView.onProcess(String.format("正在保存#%d分组数据...",index)));
        App.Post(()->mView.onSaving(progress));

        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            ESONObject e = lstReadGroupRecovery.get(iSaveGroupIndex);
            List<ESONObject> lstTemp = new ArrayList<>();
            String idCardNo = e.getJSONValue(sIdCardNo,"");

            ISetterListener listener = new ISetterListener() {
                @Override
                public void onSuccess() {
                    ++iSaveGroupIndex;
                    lstSaveGroupSuccess.add(lstTemp.get(0));
                    App.Post(()->mView.onSavingGroupSuccessCount(lstSaveGroupSuccess.size()));
                    autoSaveGroup();
                }

                @Override
                public void onFailure(String err) {
                    Integer retry = mSaveGroupRetry.get(idCardNo);
                    if(retry == null) retry = 0;
                    mSaveGroupRetry.put(idCardNo,retry+1);
                    if(retry<3){
                        final int i = retry;
                        App.Post(()->mView.onProcess(String.format("保存#%d分组失败，稍后尝试第%d次！",index,i+1)));
                        App.PostDelayed(()->autoSaveGroup(),500L);
                        return;
                    }
                    App.Post(()->mView.onProcess(String.format("保存#%d分组失败，跳过！",index)));

                    lstSaveGroupFailure.add(lstTemp.get(0));
                    ++iSaveGroupIndex;
                    autoSaveGroup();
                    App.Post(()->mView.onSavingGroupFailureCount(lstSaveGroupFailure.size()));
                }
            };

            if(isOverride() || isRecoveryFirst()){
                lstTemp.add(e);
                saveGroup(e,listener);
                return;
            }
            if(isLocalFirst()){
                if(mReadGroupDuplicatedMapper.containsKey(idCardNo)){
                    e = mReadGroupLocalMapper.get(idCardNo);
                }
                lstTemp.add(e);
                saveGroup(e,listener);
                return;
            }
            if(isUserDetermine()){
                if(mReadGroupDuplicatedMapper.containsKey(idCardNo)){
                    ++iSaveGroupIndex;
                    autoSaveGroup();
                }
                else{
                    lstTemp.add(e);
                    saveGroup(e,listener);
                }
            }
        });
    }

    private Map<String,Integer> mSaveGroupRetry = new HashMap<>();
    @Override
    void saveGroup(ESONObject data, ISetterListener listener) {
        getDb3Local().addPeopleToGroup(data,listener);
    }

    void autoSaveLog(){
        if(lstReadLogRecovery.isEmpty()) {
            App.Post(()->mView.onSaveSuccess());
            App.Post(()->mView.onProcess("保存完毕"));
            if(mode == 0 && priority == 2){
                progress = 5;
            }
            else{
                progress = 6;
            }
            return;
        }

        final int index = mReadLogRecoveryMapper.size() - lstReadLogRecovery.size() + 1;
        final int progress = (index + mReadMemberRecoveryMapper.size() + mReadGroupRecoveryMapper.size()) * 100 / iCountRecovery;
        App.Post(()->mView.onProcess(String.format("正在保存#%d采集日志数据...",index)));
        App.Post(()->mView.onSaving(progress));

        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            ESONObject e = lstReadLogRecovery.get(0);
            String idCardNo = e.getJSONValue(sIdCardNo,"");

            ISetterListener listener = new ISetterListener() {
                @Override
                public void onSuccess() {
                    lstReadLogRecovery.remove(0);
                    App.Post(()->mView.onSavingMemberSuccessCount(index+1));
                    autoSaveLog();
                }

                @Override
                public void onFailure(String err) {
                    Integer retry = mSaveLogRetry.get(idCardNo);
                    if(retry == null) retry = 0;
                    mSaveGroupRetry.put(idCardNo,retry+1);
                    if(retry<5){
                        final int i = retry;
                        App.Post(()->mView.onProcess(String.format("保存#%d采集日志失败，稍后尝试第%d次！",index,i+1)));
                        App.PostDelayed(()->autoSaveLog(),1000L);
                        return;
                    }
                    lstReadLogRecovery.remove(0);

                    App.Post(()->mView.onProcess(String.format("保存#%d采集日志失败，跳过！",index)));
                    App.Post(()->mView.onSavingMemberFailureCount(index+1));
                }
            };
            saveLog(e,listener);
        });
    }
    private Map<String,Integer> mSaveLogRetry = new HashMap<>();
    void saveLog(ESONObject data, ISetterListener listener){
        getDb5Local().log(data, listener);
    }

    @Override
    void markTransactionSuccess() {

    }

    @Override
    void markTransactionFailure() {

    }

    @Override
    void release() {
        if(template !=null){
            ThreadPoolProvider.getFixedThreadPool().execute(()->{
                template.getDatabaseTemplate().closeDbAndDelete(
                        template.getTempCode(),
                        template.getDatabaseSetting().getRegionName(),
                        getTownName(),
                        getVillageName()
                );
                List<ESONObject> lstDbs = JsonUtils.parseToList(Constants.DBConfig.getAllDatabase());
                for(ESONObject db:lstDbs){
                    String sTownName    = db.getJSONValue("townName"   ,"");
                    String sVillageName = db.getJSONValue("villageName","");
                    if(!getTownName().equals(sTownName) || !getVillageName().equals(sVillageName)) continue;
                    int    id           = db.getJSONValue("id",0);
                    Constants.DBConfig.setSelectedDatabase(id);
                    break;
                }
                template.setUseTempDbPath(false,"","");
            });

        }
    }
}
