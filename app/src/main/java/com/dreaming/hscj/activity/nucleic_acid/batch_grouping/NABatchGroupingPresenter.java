package com.dreaming.hscj.activity.nucleic_acid.batch_grouping;

import android.util.Log;
import android.util.Pair;

import com.dreaming.hscj.App;
import com.dreaming.hscj.core.ThreadPoolProvider;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.template.api.wrapper.ApiParam;
import com.dreaming.hscj.template.database.impl.NucleicAcidGroupingDatabase;
import com.dreaming.hscj.template.database.impl.UserOverallDatabase;
import com.dreaming.hscj.template.database.wrapper.ISetterListener;
import com.dreaming.hscj.utils.CheckUtils;
import com.dreaming.hscj.utils.ExcelUtils;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

public class NABatchGroupingPresenter extends INABatchGroupingContract.Presenter {

    private String path1 = App.sInstance.getExternalCacheDir().getAbsolutePath();
    private String path2 = path1.lastIndexOf("Android") >-1 ? path1.substring(0,path1.lastIndexOf("Android")) : "/storage/emulated/0";

    public final String sQQRootPath = path2+"/tencent/QQfile_recv";
    public final String sWXRootPath = path2+"/tencent/MicroMsg/Download";
    public final String sRootPath   = path2;

    private int path     = 0;
    @Override
    void setPath(int path) {
        this.path = path;
    }

    @Override
    int getPath() {
        return path;
    }

    //0初始化 1选择 2读取 3冲突及失败处理 4成功
    public final int PROGRESS_INIT = 0;
    public final int PROGRESS_SELECT = 1;
    public final int PROGRESS_READ = 2;
    public final int PROGRESS_PROCESSING = 3;
    public final int PROGRESS_SUCCESS = 4;
    private int progress = 0;

    public String sIdCardNoName = Template.getCurrentTemplate().getIdCardNoFieldName();
    public String sNameFieldName= Template.getCurrentTemplate().getIdCardNameFieldName();
    public String sGroupName    = Template.getCurrentTemplate().getGroupIdFieldName();

    UserOverallDatabase         db2 = Template.getCurrentTemplate().getUserOverallDatabase();
    NucleicAcidGroupingDatabase db3 = Template.getCurrentTemplate().getNucleicAcidGroupingDatabase();

    @Override
    int getProgress() {
        return progress;
    }

    private Map<String,ESONObject> mExcelData         = new HashMap<>();
    private Map<String,Integer>    mExcelSuccess      = new HashMap<>();
    private Map<String,Integer>    mExcelGroupCounter = new HashMap<>();
    private List<Pair<Integer,ESONObject>> lstExcelSuccess     = new ArrayList<>();
    private List<Pair<Integer,ESONObject>> lstExcelInvalid     = new ArrayList<>();
    private List<Pair<Integer,ESONObject>> lstExcelLimitErr    = new ArrayList<>();
    private List<Pair<Integer,ESONObject>> lstExcelMemberEmpty = new ArrayList<>();
    private List<Pair<Integer,ESONObject>> lstExcelDuplicated  = new ArrayList<>();


    private void sort(List<Pair<Integer,ESONObject>> lst){
        Collections.sort(lst, new Comparator<Pair<Integer,ESONObject>>(){
            @Override
            public int compare(Pair<Integer,ESONObject> o1, Pair<Integer,ESONObject> o2) {
                return o1.first.compareTo(o2.first);
            }
        });
    }

    @Override
    void read(String path, int sheet, int startLine) throws Exception {
        final ExcelUtils.Reader reader = new ExcelUtils.Reader(new FileInputStream(new File(path)));

        ThreadPoolProvider.getFixedThreadPool().execute(()->{

            mExcelData        .clear();
            mExcelSuccess     .clear();
            mExcelGroupCounter.clear();

            lstExcelSuccess    .clear();
            lstExcelInvalid    .clear();
            lstExcelLimitErr   .clear();
            lstExcelMemberEmpty.clear();
            lstExcelDuplicated .clear();

            List<ApiParam> lstExcelFields = NABatchGroupingConfigActivity.getExcelConfig();

            final int iRowCount = reader.getRowCount(sheet);
            final int iFieldCount = lstExcelFields.size();

            progress = 1;
            try {
                for(int i=startLine,ni=iRowCount;i<ni;++i){
                    final ESONObject rowData = new ESONObject();
                    for(int j=0,nj=iFieldCount;j<nj;++j){
                        reader.readAtSync(sheet,i,j,new ExcelUtils.IExcelReadListener() {
                            @Override
                            public void onRead(int row, int column, int rowCount, int columnCount, String value) {
                                rowData.putValue(lstExcelFields.get(column).getName(),value);
                                App.Post(()->mView.onReading(row, column, (int) ((row*iFieldCount + column)*100L/(iRowCount*iFieldCount)), mExcelSuccess.size(),value));
                            }

                            @Override
                            public void onError(Exception e) {
                                App.Post(()->mView.onReadError(e.getMessage()));
                            }
                        });
                    }

                    String idCardNo = rowData.getJSONValue(sIdCardNoName,"").trim();
                    StringBuilder sb = new StringBuilder(idCardNo);
                    String type = CheckUtils.isValidCard(Template.getCurrentTemplate().getApiConfig().getCard(), sb);
                    if(type == null){
                        lstExcelInvalid.add(new Pair<>(i,rowData));
                        App.Post(()->mView.onReadingGroupErrorCount(lstExcelInvalid.size()));
                        continue;
                    }
                    idCardNo = sb.toString();
                    String groupId = rowData.getJSONValue(sGroupName,"").trim();

                    if(mExcelSuccess.containsKey(idCardNo)){
                        lstExcelDuplicated.add(new Pair<>(i,rowData));
                        App.Post(()->mView.onReadingGroupDuplicatedCount(lstExcelDuplicated.size()));
                        continue;
                    }

                    List<String> k = new ArrayList<>(); k.add(sIdCardNoName);
                    List<Object> v = new ArrayList<>(); v.add(idCardNo);
                    ESONArray result = db2.query(k,v);
                    if(result.length() != 1){
                        lstExcelMemberEmpty.add(new Pair<>(i,rowData));
                        App.Post(()->mView.onReadingGroupMemberEmpty(lstExcelMemberEmpty.size()));
                        continue;
                    }

                    ESONObject eLocal = result.getArrayValue(0,new ESONObject());
                    for (Iterator<String> it = eLocal.keys(); it.hasNext(); ) {
                        String key = it.next();
                        if(sGroupName.equals(key)) continue;
                        if(sIdCardNoName.equals(key)) continue;
                        Object value = null;
                        try { value = eLocal.get(key); } catch (Exception e) { }
                        if(value == null) continue;
                        rowData.putValue(key,value);
                    }


                    Integer countExcel = mExcelGroupCounter.get(groupId);
                    countExcel = countExcel == null ? 1 : (countExcel+1);
                    mExcelGroupCounter.put(groupId,countExcel);

                    if(countExcel>Template.getCurrentTemplate().getDatabaseSetting().getGroupMemberNum()){
                        lstExcelLimitErr.add(new Pair<>(i,rowData));
                        App.Post(()->mView.onReadingGroupLimitErrorCount(lstExcelLimitErr.size()));
                        continue;
                    }

                    mExcelData.put(idCardNo,rowData);
                    mExcelSuccess.put(idCardNo,i);
                    lstExcelSuccess.add(new Pair<>(i,rowData));
                    App.Post(()->mView.onReadingGroupExcelCount(lstExcelSuccess.size()));
                }
                sort(lstExcelSuccess);
                sort(lstExcelDuplicated);
                sort(lstExcelInvalid);
                sort(lstExcelLimitErr);
                sort(lstExcelMemberEmpty);
                App.Post(()->mView.onReadSuccess());
                progress = 2;
            } catch (Exception e) {
                App.Post(()->{
                    try { mView.onReadError(e.getMessage()); } catch (Exception ex) { }
                });
            }
        });
    }

    @Override
    List<Pair<Integer, ESONObject>> getReadGroupExcelSuccess() {
        return lstExcelSuccess;
    }

    @Override
    List<Pair<Integer, ESONObject>> getReadGroupExcelFailure() {
        return lstExcelInvalid;
    }

    @Override
    List<Pair<Integer, ESONObject>> getReadGroupExcelDuplicated() {
        return lstExcelDuplicated;
    }

    @Override
    List<Pair<Integer, ESONObject>> getReadGroupMemberEmpty() {
        return lstExcelMemberEmpty;
    }

    @Override
    List<Pair<Integer, ESONObject>> getReadGroupLimitError() {
        return lstExcelLimitErr;
    }

    private List<Pair<Integer, ESONObject>> lstSaveSuccess = new ArrayList<>();
    private List<Pair<Integer, ESONObject>> lstSaveFailure = new ArrayList<>();

    @Override
    List<Pair<Integer, ESONObject>> getSaveGroupSuccess() {
        return lstSaveSuccess;
    }

    @Override
    List<Pair<Integer, ESONObject>> getSaveGroupFailure() {
        return lstSaveFailure;
    }

    private AtomicBoolean bInSaving = new AtomicBoolean(false);
    private AtomicBoolean bStarted  = new AtomicBoolean(false);
    private LinkedList<Pair<Integer,ESONObject>> lstSavingQueue = new LinkedList<>();
    private AtomicBoolean bSuccess = new AtomicBoolean(false);
    @Override
    void autoSave() {
        progress = PROGRESS_PROCESSING;

        if(bStarted.get()) return;

        bStarted.set(true);
        bSuccess.set(false);

        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            App.Post(()->mView.onProcess("开始保存Excel分组数据！"));

            mRetry.clear();
            mCallbacks.clear();

            lstSaveFailure.clear();
            lstSaveSuccess.clear();
            lstSavingQueue.clear();

            bIsMarkTransactionSuccess.set(false);
            bIsMarkTransactionFailure.set(false);

            lstSavingQueue.addAll(lstExcelSuccess);

            Log.e("BatchGrouping","start auto save");

            db3.beginTransaction();

            Log.e("BatchGrouping","beginTransaction");

            db3.clear();

            Log.e("BatchGrouping","begin save loop");

            while (true){
                if(bIsMarkTransactionSuccess.get() || bIsMarkTransactionFailure.get()){
                    break;
                }
                if(bInSaving.get()){
                    try { Thread.sleep(100); } catch (Exception e) { }
                    continue;
                }
                if(lstSavingQueue.isEmpty()){
                    if(lstSaveFailure.isEmpty()){
                        progress = PROGRESS_SUCCESS;
                        bIsMarkTransactionSuccess.set(true);
                        App.Post(()->mView.onSaveSuccess());
                        break;
                    }
                    if(!bSuccess.get()){
                        bSuccess.set(true);
                        App.Post(()->mView.onSaveSuccess());
                    }
                    try { Thread.sleep(100); } catch (Exception e) { }
                    continue;
                }
                bInSaving.set(true);

                Pair<Integer,ESONObject> toSave = lstSavingQueue.removeFirst();

                App.Post(()->mView.onProcess(String.format("开始保存#%3d！",toSave.first)));
                App.Post(()->mView.onProgress((lstExcelSuccess.size()+lstSaveFailure.size())*100/lstExcelSuccess.size()));

                Log.e("BatchGrouping","start saving #"+toSave.first);

                saveGroupSync(toSave.first, toSave.second, new ISetterListener() {
                    @Override
                    public void onSuccess() {
                        Log.e("BatchGrouping","saving #"+toSave.first+" success");
                        lstSaveSuccess.add(toSave);
                        App.Post(()->mView.onSavingGroupSuccessCount(lstSaveSuccess.size()));
                    }

                    @Override
                    public void onFailure(String err) {
                        Log.e("BatchGrouping","saving #"+toSave.first+" error");
                        boolean bContains = false;
                        for(Pair<Integer,ESONObject> p:lstSaveFailure){
                            if(p.first.equals(toSave.first)){
                                bContains = true;
                                break;
                            }
                        }
                        if(!bContains) lstSaveFailure.add(toSave);
                        App.Post(()->mView.onSavingGroupFailureCount(lstSaveFailure.size()));
                    }
                });

            }

            if(bIsMarkTransactionSuccess.get()){
                db3.setTransactionSuccessful();
            }

            Log.e("BatchGrouping","end auto save");

            db3.endTransaction();

            Log.e("BatchGrouping","endTransaction");

            bStarted.set(false);
        });
    }

    @Override
    boolean isEndAutoSave() {
        return bSuccess.get() || progress == PROGRESS_SUCCESS;
    }

    private Map<Integer,Integer> mRetry = new HashMap<>();
    private Map<Integer,ISetterListener> mCallbacks = new HashMap<>();
    @Override
    void saveGroup(int index, ESONObject data, ISetterListener listener) {
        mCallbacks.put(index,listener);
        mRetry.put(index,0);
        lstSavingQueue.add(new Pair<>(index,data));
    }

    void saveGroupSync(int index, ESONObject data, ISetterListener listener) {
        try {
            db3.addPeopleToGroupSync(data, new ISetterListener() {
                @Override
                public void onSuccess() {
                    if(listener!=null) listener.onSuccess();
                    ISetterListener callback = mCallbacks.get(index);
                    if(callback!=null) App.Post(()->callback.onSuccess());
                    mCallbacks.remove(index);
                    bInSaving.set(false);
                }

                @Override
                public void onFailure(String err) {
                    Integer retryCount = mRetry.get(index);
                    if(retryCount == null) retryCount = 0;
                    if(retryCount>2){
                        if(listener!=null) listener.onFailure(err);
                        bInSaving.set(false);
                        ISetterListener callback = mCallbacks.get(index);
                        if(callback!=null) App.Post(()->callback.onFailure(err));
                        mCallbacks.remove(index);
                        return;
                    }
                    mRetry.put(index,retryCount+1);
                    App.PostDelayed(()->{
                        saveGroup(index, data, listener);
                    },500);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Integer retryCount = mRetry.get(index);
            if(retryCount == null) retryCount = 0;
            if(retryCount>2){
                if(listener!=null) listener.onFailure(e.getMessage());
                bInSaving.set(false);
                ISetterListener callback = mCallbacks.get(index);
                if(callback!=null) App.Post(()->callback.onFailure(e.getMessage()));
                mCallbacks.remove(index);
                return;
            }
            mRetry.put(index,retryCount+1);
            App.PostDelayed(()->{
                saveGroup(index, data, listener);
            },500);
        }
    }

    private AtomicBoolean bIsMarkTransactionSuccess = new AtomicBoolean(false);
    @Override
    void markTransactionSuccess() {
        bIsMarkTransactionSuccess.set(true);
    }

    private AtomicBoolean bIsMarkTransactionFailure = new AtomicBoolean(false);
    @Override
    void markTransactionFailure() {
        bIsMarkTransactionFailure.set(true);
    }
}
