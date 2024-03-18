package com.dreaming.hscj.activity.community.batch_input;

import android.util.Pair;

import com.dreaming.hscj.App;
import com.dreaming.hscj.core.ThreadPoolProvider;
import com.dreaming.hscj.template.DataParser;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.template.api.ApiProvider;
import com.dreaming.hscj.template.api.impl.Api;
import com.dreaming.hscj.template.api.wrapper.ApiParam;
import com.dreaming.hscj.template.database.wrapper.DatabaseConfig;
import com.dreaming.hscj.template.database.wrapper.IGetterListener;
import com.dreaming.hscj.template.database.wrapper.ISetterListener;
import com.dreaming.hscj.utils.CheckUtils;
import com.dreaming.hscj.utils.ExcelUtils;
import com.dreaming.hscj.utils.ToastUtils;


import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

public class CommunityBatchInputPresenter extends ICommunityBatchInputContract.Presenter{

    private String path1 = App.sInstance.getExternalCacheDir().getAbsolutePath();
    private String path2 = path1.lastIndexOf("Android") >-1 ? path1.substring(0,path1.lastIndexOf("Android")) : "/storage/emulated/0";

    public final String sQQRootPath = path2+"/tencent/QQfile_recv";
    public final String sWXRootPath = path2+"/tencent/MicroMsg/Download";
    public final String sRootPath   = path2;
    public final String sUserName     = Template.getCurrentTemplate().getUserOverallDatabase().getNameFiledName();
    public final String sIdCardNoName = Template.getCurrentTemplate().getUserOverallDatabase().getIdFieldName();

    int iProcessState = 0;//0初始化 1 处理Excel导入  2联网查询 3存入数据库 4完成

    private boolean bIsCover = true;
    @Override
    void setCovered(boolean b) {
        bIsCover = b;
    }

    private Map<String,ESONObject> mExcelData     = new HashMap<>();
    private List<Pair<Integer,ESONObject>> lstExcelPreview = new ArrayList<>();
    private List<Pair<Integer,ESONObject>> lstExcelInvalid = new ArrayList<>();
    private Map<String,Integer> mExcelSuccess = new HashMap<>();
    private Map<String,HashSet<Integer>> sExcelDuplicatedIndex = new HashMap<>();
    private List<Pair<Integer,ESONObject>> lstExcelDuplicatedShown = new ArrayList<>();
    private int iExcelDuplicatedCount = 0;
    private boolean bCanSaveExcelToDb = false;

    @Override
    void readExcel(String path, int sheet, int startLine) throws Exception {
        final ExcelUtils.Reader reader = new ExcelUtils.Reader(new FileInputStream(new File(path)));

        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            bCanSaveExcelToDb = true;
            List<ApiParam> excelParams = CommunityBatchInputConfigActivity.getExcelConfig();
            List<ApiParam> dbParams = Template.getCurrentTemplate().getUserOverallDatabase().getConfig().getFields();
            for(ApiParam p:dbParams){
                if(!ApiParam.DEFAULT_NO_EMPTY.equals(p.getDefaultValue())) continue;
                boolean bFind = false;

                for(ApiParam e:excelParams){
                    if(e == null) continue;
                    if(p.getName().equals(e.getName())){
                        bFind = true;
                        break;
                    }
                }

                if(!bFind){
                    bCanSaveExcelToDb = false;
                    break;
                }
            }
            if(bCanSaveExcelToDb){
                App.Post(()-> ToastUtils.show("Excel中的数据在本地空白且网络空白的情况下将会保存到本地！"));
            }
            else{
                App.Post(()-> ToastUtils.show("Excel中的数据在本地空白且网络空白的情况下不会保存到本地，Excel导入字段没有包含所有【保存必须字段】！"));
            }

            mExcelData.clear();
            lstExcelInvalid.clear();
            mExcelSuccess.clear();
            lstExcelPreview.clear();
            sExcelDuplicatedIndex.clear();
            lstExcelDuplicatedShown.clear();
            iExcelDuplicatedCount = 0;

            List<ApiParam> lstExcelFields = CommunityBatchInputConfigActivity.getExcelConfig();


            final int iRowCount = reader.getRowCount(sheet);
            final int iFieldCount = lstExcelFields.size();

            iProcessState = 1;
            App.Post(()->mView.onReadExcelStarted());
            try {
                for(int i=startLine,ni=iRowCount;i<ni;++i){
                    final ESONObject rowData = new ESONObject();
                    for(int j=0,nj=iFieldCount;j<nj;++j){
                        reader.readAtSync(sheet,i,j,new ExcelUtils.IExcelReadListener() {
                            @Override
                            public void onRead(int row, int column, int rowCount, int columnCount, String value) {
                                rowData.putValue(lstExcelFields.get(column).getName(),value);
                                App.Post(()->mView.onReadingExcel(row, column, (int) ((row*iFieldCount + column)*100L/(iRowCount*iFieldCount)), mExcelSuccess.size(),value));
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
                        App.Post(()->mView.onReadCheckFailed(lstExcelInvalid.size()));
                        continue;
                    }
                    idCardNo = sb.toString();
                    Integer rowIndex = mExcelSuccess.get(idCardNo);
                    if(rowIndex!=null){
                        HashSet s = sExcelDuplicatedIndex.get(idCardNo);
                        if(s==null){
                            s = new HashSet<Integer>();
                            s.add(rowIndex);
                            lstExcelDuplicatedShown.add(new Pair<>(rowIndex,mExcelData.get(idCardNo)));
                        }
                        s.add(i);
                        lstExcelDuplicatedShown.add(new Pair<>(i,rowData));
                        sExcelDuplicatedIndex.put(idCardNo,s);
                        ++iExcelDuplicatedCount;
                        App.Post(()->mView.onReadDuplicated(iExcelDuplicatedCount));
                        continue;
                    }
                    mExcelData.put(idCardNo,rowData);
                    mExcelSuccess.put(idCardNo,i);
                    lstExcelPreview.add(new Pair<>(i,rowData));
                }
                App.Post(()->mView.onReadFinished());
                iProcessState = 2;
            } catch (Exception e) {
                App.Post(()->{
                    try { mView.onReadError(e.getMessage()); } catch (Exception ex) { }
                });
            }
        });
    }

    private void sort(List<Pair<Integer,ESONObject>> lst){
        Collections.sort(lst, new Comparator<Pair<Integer,ESONObject>>(){
            @Override
            public int compare(Pair<Integer,ESONObject> o1, Pair<Integer,ESONObject> o2) {
                return o1.first.compareTo(o2.first);
            }
        });
    }

    @Override
    List<Pair<Integer,ESONObject>> getExcelPreviewData() {
        sort(lstExcelPreview);
        return lstExcelPreview;
    }

    @Override
    List<Pair<Integer, ESONObject>> getExcelSuccessData() {
        sort(lstExcelPreview);
        return lstExcelPreview;
    }

    @Override
    List<Pair<Integer, ESONObject>> getExcelDuplicatedData() {
        sort(lstExcelDuplicatedShown);
        return lstExcelDuplicatedShown;
    }

    @Override
    List<Pair<Integer, ESONObject>> getExcelCheckFailedData() {
        sort(lstExcelInvalid);
        return lstExcelInvalid;
    }

    private Map<String,ESONObject> mLocalData = new HashMap<>();
    private List<Pair<Integer,ESONObject>> lstLocalDuplicated  = new ArrayList<>();
    private Map<Integer,String> mRequestLocalFlag = new TreeMap<>();
    private Map<String,Integer> mLocalRetry = new HashMap<>();
    private void requestDatabaseData() {
        if(mRequestLocalFlag.isEmpty()){
            App.Post(()->mView.onProcessFinished());
            if(lstLocalDuplicated.isEmpty() || bIsCover) iProcessState = 5;
            return;
        }
        Map.Entry<Integer,String> entry = mRequestLocalFlag.entrySet().iterator().next();
        final String idCard = entry.getValue();

        Template.getCurrentTemplate().getUserOverallDatabase().getPeople(new ESONObject().putValue(sIdCardNoName, idCard), new IGetterListener() {
            @Override
            public void onSuccess(ESONArray data) {
                if(data == null || data.length()==0){
                    App.Post(()->mView.updateProcessMessage(String.format("获取本地数据#%d成功，本地无此数据记录！", mExcelSuccess.get(idCard))));
                }
                else{
                    ESONObject e = data.getArrayValue(0,new ESONObject());
                    if(e.length()>0){
                        App.Post(()->mView.updateProcessMessage(String.format("获取本地数据#%d成功，数据重复！", mExcelSuccess.get(idCard))));
                        lstLocalDuplicated.add(new Pair<>(mExcelSuccess.get(idCard),mExcelData.get(idCard)));
                        mLocalData.put(idCard,e);
                        mView.onProcessDuplicated(lstLocalDuplicated.size());
                    }
                }
                mRequestLocalFlag.remove(entry.getKey());
                requestNetworkData(idCard);
            }

            @Override
            public void onFailure(String err) {
                Integer i = mLocalRetry.get(idCard);
                if(i==null){
                    i=1;
                }
                if(i>2){
                    mRequestLocalFlag.remove(entry.getKey());
                    App.Post(()->mView.updateProcessMessage(String.format("获取本地数据#%d失败，跳过获取！", mExcelSuccess.get(idCard))));
                    requestNetworkData(idCard);
                    return;
                }
                mLocalRetry.put(idCard,i+1);
                final int retryCount = i;
                App.Post(()->mView.updateProcessMessage(String.format("获取本地数据#%d失败，稍后重试第%d次！", mExcelSuccess.get(idCard),retryCount)));
                App.PostDelayed(()->requestDatabaseData(),500);
            }
        });
    }

    private Map<String,ESONObject> mNetData  = new HashMap<>();
    private List<Pair<Integer,ESONObject>> lstNetSuccess       = new ArrayList<>();
    private List<Pair<Integer,ESONObject>> lstNetEmpty         = new ArrayList<>();
    private List<Pair<Integer,ESONObject>> lstNetError         = new ArrayList<>();
    private Map<String,Integer> mNetRetry    = new HashMap<>();

    private void requestNetworkData(String idCard) {
        ApiProvider.requestPeopleInfoByIdCard(idCard, new ApiProvider.IPeopleInfoListener() {
            @Override
            public void onSuccess(ESONObject data) {
                if(data.length() == 0){
                    lstNetEmpty.add(new Pair<>(mExcelSuccess.get(idCard),mExcelData.get(idCard)));
                    mView.onProcessNetEmpty(lstNetEmpty.size());
                    App.Post(()->mView.updateProcessMessage(String.format("获取网络数据#%d成功，数据为空！", mExcelSuccess.get(idCard))));

                    saveToLocal(idCard);
                }
                else{
                    mNetData.put(idCard,data);
                    lstNetSuccess.add(new Pair<>(mExcelSuccess.get(idCard),DataParser.parseDatabaseToShown(DatabaseConfig.TYPE_USER_OVERALL,DataParser.parseApiToDatabase(DatabaseConfig.TYPE_USER_OVERALL,Api.TYPE_GET_PEOPLE_INFO,data))));
                    App.Post(()->mView.updateProcessMessage(String.format("获取网络数据#%d成功！", mExcelSuccess.get(idCard))));
                    if(bIsCover || mLocalData.get(idCard)==null){
                        saveToLocal(idCard);
                    }
                    else{
                        requestDatabaseData();
                    }
                }

                App.Post(()->mView.onProcessSuccess(lstNetSuccess.size(), (int) (((mExcelData.size()-mRequestLocalFlag.size())*100L)/mExcelData.size())));
            }

            @Override
            public void onFailure(String err) {
                Integer i = mNetRetry.get(idCard);
                if(i==null){
                    i = 1;
                }
                if(i>2){
                    lstNetError.add(new Pair<>(mExcelSuccess.get(idCard),mExcelData.get(idCard)));
                    mView.onProcessNetError(lstNetError.size());
                    App.Post(()->mView.updateProcessMessage(String.format("获取网络数据#%d失败，跳过请求！", mExcelSuccess.get(idCard))));
                    requestDatabaseData();
                    return;
                }
                mNetRetry.put(idCard,i+1);
                final int retryCount = i;
                App.Post(()->mView.updateProcessMessage(String.format("获取网络数据#%d失败，稍后重试第%d次！", mExcelSuccess.get(idCard),retryCount)));
                App.PostDelayed(()->requestNetworkData(idCard),500);
            }
        });
    }

    @Override
    List<Pair<Integer, ESONObject>> getLocalDuplicatedData() {
        sort(lstLocalDuplicated);
        return lstLocalDuplicated;
    }

    @Override
    List<Pair<Integer, ESONObject>> getLocalSaveFailedData() {
        sort(lstSaveLocalFailed);
        return lstSaveLocalFailed;
    }

    @Override
    List<Pair<Integer, ESONObject>> getLocalSaveSuccessData() {
        sort(lstSaveLocalSuccess);
        return lstSaveLocalSuccess;
    }

    private Map<String,Integer> mSaveLocalRetry = new HashMap<>();
    private List<Pair<Integer,ESONObject>> lstSaveLocalFailed = new ArrayList<>();

    private List<Pair<Integer,ESONObject>> lstSaveLocalSuccess = new ArrayList<>();

    private void saveToLocal(String idCardNo){
        ESONObject eLocal = mLocalData.get(idCardNo);
        if(eLocal==null || bIsCover){
            ESONObject eNet     = mNetData  .get(idCardNo);
            if(eNet == null){
                if(bCanSaveExcelToDb){
                    ESONObject eExcel = mExcelData.get(idCardNo);

                    ESONObject eExcelSave = DataParser.parseShownToDatabase(DatabaseConfig.TYPE_USER_OVERALL,eExcel);
                    Template.getCurrentTemplate().getUserOverallDatabase().addPeople(eExcelSave, new ISetterListener() {
                        @Override
                        public void onSuccess() {
                            lstSaveLocalSuccess.add(new Pair<>(mExcelSuccess.get(idCardNo),eExcelSave));
                            App.Post(()->mView.onProcessLocalSuccess(lstSaveLocalSuccess.size()));
                            App.Post(()->mView.updateProcessMessage(String.format("保存数据#%d成功！", mExcelSuccess.get(idCardNo))));
                            requestDatabaseData();
                        }

                        @Override
                        public void onFailure(String err) {
                            Integer i = mSaveLocalRetry.get(idCardNo);
                            if(i==null){
                                i = 1;
                            }
                            if(i>2){
                                lstSaveLocalFailed.add(new Pair<>(mExcelSuccess.get(idCardNo),mExcelData.get(idCardNo)));
                                App.Post(()->mView.onProcessLocalFailed(mSaveLocalRetry.size()));
                                App.Post(()->mView.updateProcessMessage(String.format("保存数据#%d失败，跳过！", mExcelSuccess.get(idCardNo))));
                                requestDatabaseData();
                                return;
                            }
                            mSaveLocalRetry.put(idCardNo,i+1);
                            final int retryCount = i;
                            App.Post(()->mView.updateProcessMessage(String.format("保存数据#%d失败，稍后重试第%d次！", mExcelSuccess.get(idCardNo),retryCount)));
                            App.PostDelayed(()->saveToLocal(idCardNo),500);
                        }
                    });
                    return;
                }
                requestDatabaseData();
                return;
            }

            ESONObject eNetToDb = DataParser.parseApiToDatabase(DatabaseConfig.TYPE_USER_OVERALL,Api.TYPE_GET_PEOPLE_INFO,eNet);
            Template.getCurrentTemplate().getUserOverallDatabase().addPeople(eNetToDb, new ISetterListener() {
                @Override
                public void onSuccess() {
                    lstSaveLocalSuccess.add(new Pair<>(mExcelSuccess.get(idCardNo),eNetToDb));
                    App.Post(()->mView.onProcessLocalSuccess(lstSaveLocalSuccess.size()));
                    App.Post(()->mView.updateProcessMessage(String.format("保存数据#%d成功！", mExcelSuccess.get(idCardNo))));
                    requestDatabaseData();
                }

                @Override
                public void onFailure(String err) {
                    Integer i = mSaveLocalRetry.get(idCardNo);
                    if(i==null){
                        i = 1;
                    }
                    if(i>2){
                        lstSaveLocalFailed.add(new Pair<>(mExcelSuccess.get(idCardNo),mExcelData.get(idCardNo)));
                        App.Post(()->mView.onProcessLocalFailed(mSaveLocalRetry.size()));
                        App.Post(()->mView.updateProcessMessage(String.format("保存数据#%d失败，跳过！", mExcelSuccess.get(idCardNo))));
                        requestDatabaseData();
                        return;
                    }
                    mSaveLocalRetry.put(idCardNo,i+1);
                    final int retryCount = i;
                    App.Post(()->mView.updateProcessMessage(String.format("保存数据#%d失败，稍后重试第%d次！", mExcelSuccess.get(idCardNo),retryCount)));
                    App.PostDelayed(()->saveToLocal(idCardNo),500);
                }
            });
        }
    }

    @Override
    void processExcel() {
        mView.onProcessStart();
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            mLocalData.clear();
            mLocalRetry.clear();
            lstLocalDuplicated.clear();
            mRequestLocalFlag.clear();

            mNetRetry.clear();
            mNetData.clear();
            lstNetSuccess.clear();
            lstNetEmpty.clear();
            lstNetError.clear();

            mSaveLocalRetry.clear();
            lstSaveLocalFailed.clear();
            for(Map.Entry<String,Integer> entry: mExcelSuccess.entrySet()){
                mRequestLocalFlag.put(entry.getValue(),entry.getKey());
            }
            requestDatabaseData();
        });
    }

    @Override
    List<Pair<Integer, ESONObject>> getNetSuccessData() {
        sort(lstNetSuccess);
        return lstNetSuccess;
    }

    @Override
    List<Pair<Integer, ESONObject>> getNetEmptyData() {
        sort(lstNetEmpty);
        return lstNetEmpty;
    }

    @Override
    List<Pair<Integer, ESONObject>> getNetErrorData() {
        sort(lstNetError);
        return lstNetError;
    }

    @Override
    ESONObject getNetShownData(String idCardNo) {
        return DataParser.parseDatabaseToShown(DatabaseConfig.TYPE_USER_OVERALL,DataParser.parseApiToDatabase(DatabaseConfig.TYPE_USER_OVERALL,Api.TYPE_GET_PEOPLE_INFO,mNetData.get(idCardNo)));
    }

    @Override
    ESONObject getLocalShownData(String idCardNo) {
        return DataParser.parseDatabaseToShown(DatabaseConfig.TYPE_USER_OVERALL,mLocalData.get(idCardNo));
    }

    @Override
    void saveToLocal(List<Pair<ApiParam,String>> data ,CommunityBatchInputActivity.ISavingListener listener) {
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            ESONObject ePrepare = new ESONObject();
            for(Pair<ApiParam,String> item:data){
                ePrepare.putValue(item.first.getName(),item.second);
            }
            ESONObject eSaving = DataParser.parseShownToDatabase(DatabaseConfig.TYPE_USER_OVERALL,ePrepare);
            Template.getCurrentTemplate().getUserOverallDatabase().addPeople(eSaving, new ISetterListener() {
                @Override
                public void onSuccess() {
                    listener.onSuccess();
                    if(lstLocalDuplicated.isEmpty()){
                        iProcessState = 5;
                    }
                }
                @Override
                public void onFailure(String err) {
                   listener.onFailure(err);
                }
            });
        });


    }

    @Override
    boolean canFinish() {
        return iProcessState == 0 || iProcessState==5 ||(iProcessState ==4 && bIsCover);
    }
}
