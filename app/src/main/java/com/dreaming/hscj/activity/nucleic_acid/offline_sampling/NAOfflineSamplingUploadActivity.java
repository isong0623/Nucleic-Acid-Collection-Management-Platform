package com.dreaming.hscj.activity.nucleic_acid.offline_sampling;

import android.graphics.Color;
import android.graphics.Point;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.dreaming.hscj.App;
import com.dreaming.hscj.R;
import com.dreaming.hscj.base.BaseActivity;
import com.dreaming.hscj.core.BundleBuilder;
import com.dreaming.hscj.core.TTSEngine;
import com.dreaming.hscj.core.ThreadPoolProvider;
import com.dreaming.hscj.dialog.loading.LoadingDialog;
import com.dreaming.hscj.entity.OfflineExcel;
import com.dreaming.hscj.template.DataParser;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.template.api.ApiProvider;
import com.dreaming.hscj.template.api.impl.Api;
import com.dreaming.hscj.template.database.wrapper.DatabaseConfig;
import com.dreaming.hscj.utils.DensityUtils;
import com.dreaming.hscj.utils.ExcelUtils;
import com.dreaming.hscj.utils.JsonUtils;
import com.dreaming.hscj.utils.ToastUtils;
import com.dreaming.hscj.widget.ShownView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import priv.songxusheng.easydialog.EasyDialog;
import priv.songxusheng.easydialog.EasyDialogHolder;
import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

public class NAOfflineSamplingUploadActivity extends BaseActivity {

    public static void doUpload(BaseActivity activity, String path, View.OnClickListener callback){
        activity.startActivity(NAOfflineSamplingUploadActivity.class, BundleBuilder.create("path",path).build());
    }

    @Override
    public int getContentViewResId() {
        return R.layout.activity_nucleic_acid_offline_upload;
    }

    @BindView(R.id.sv_progress)
    ShownView svProgress;
    @BindView(R.id.pb_process)
    ProgressBar pbProgress;
    @BindView(R.id.tv_offline_file_name)
    TextView tvFileName;

    @BindView(R.id.sv_upload_record)
    ShownView svUploadRecord;
    @BindView(R.id.sv_success)
    ShownView svSuccess;
    @BindView(R.id.sv_failed)
    ShownView svFailure;

    @BindView(R.id.tv_upload)
    TextView tvUpload;

    @Override
    protected void onBindView(View vContent) {
        ButterKnife.bind(this,vContent);
    }

    String path;
    OfflineExcel excel;
    @Override
    public void initView() {
        setCenterText("核酸离线记录上传");
        path = getIntent().getStringExtra("path");
        if(path == null) path = "";
        try {
            excel = OfflineExcel.load(path);
        } catch (Exception e) {
            e.printStackTrace();
            ToastUtils.show("离线文件打开失败！");
            return;
        }
        tvFileName.setText(new File(path).getName().replaceAll(".xls","").replaceAll(".xlsx",""));
        svProgress.tvValue.setText("等待开始上传。");
        svUploadRecord.tvValue.setOnClickListener(v -> showUploadRecordDialog());
        svSuccess     .tvValue.setOnClickListener(v -> showSuccessRecordDialog());
        svFailure     .tvValue.setOnClickListener(v -> showFailedRecordDialog());

        svUploadRecord.tvValue.setText("无记录。");
        svSuccess     .tvValue.setText("无记录。");
        svFailure     .tvValue.setText("无记录。");
    }

    //region view

    void setUploadMessage(String msg){
        App.Post(()->svProgress.tvValue.setText(msg));
    }

    void setUploadProgress(int progress){
        App.Post(()->pbProgress.setProgress(progress));
    }

    void setSuccessMessage(int count){
        App.Post(()->svSuccess.tvValue.setText(String.format("共%d条，点击查看。",count)));
    }

    void setFailureMessage(int count){
        App.Post(()->svFailure.tvValue.setText(String.format("共%d条，点击查看。",count)));
    }


    void showLoadDialog(){
        App.Post(()->LoadingDialog.showDialog("UploadOffline",this));
    }

    void hideLoadDialog(){
        App.Post(()->LoadingDialog.dismissDialog("UploadOffline"));
    }

    Map<String, List<ESONObject>> mSelected = new LinkedHashMap<>();

    void showPlanUploadDialog(){
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            if(mSelected.isEmpty()){
                showLoadDialog();
                Map<String,List<ESONObject>> m = excel.getAllSampling();
                for (Map.Entry<String, List<ESONObject>> entry : m.entrySet()) {
                    if(entry.getKey() == null) continue;
                    if(entry.getValue() == null) continue;
                    if(entry.getValue().isEmpty()) continue;

                    List<ESONObject> lst = new ArrayList<>();
                    lst.addAll(entry.getValue());
                    mSelected.put(entry.getKey(),lst);
                }
                hideLoadDialog();
            }
            App.Post(()->{
                Point p = DensityUtils.getScreenSize();

                new EasyDialog(R.layout.dialog_offline_sampling_plan_upload,this)
                        .setOnBindDialogListener(new EasyDialog.OnBindDialogListener() {
                            @Override
                            public void onBindDialog(EasyDialogHolder easyDialogHolder) {
                                easyDialogHolder.setOnClickListener(R.id.iv_close, v -> easyDialogHolder.dismissDialog());
                                ScrollView sv = easyDialogHolder.getView(R.id.sv_main);
                                ((LinearLayout)sv.getChildAt(0)).addView(OfflineSamplingHelper.buildOfflineSelectList(NAOfflineSamplingUploadActivity.this,mSelected),new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
                                easyDialogHolder.setOnClickListener(R.id.tv_upload, v -> {
                                    doUpload();
                                    easyDialogHolder.dismissDialog();
                                });
                            }
                        })
                        .setDialogParams(p.x,p.y - dp2px(80), Gravity.BOTTOM)
                        .setAllowDismissWhenBackPressed(false)
                        .setAllowDismissWhenTouchOutside(false)
                        .showDialog();
            });

        });
    }

    void showUploadRecordDialog(){
        if(mSelected == null || mSelected.isEmpty()){
            ToastUtils.show("没有任何记录！");
            return;
        }
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            Map<String,List<ESONObject>> m = new HashMap<>();
            showLoadDialog();
            for (Map.Entry<String, List<ESONObject>> entry : mSelected.entrySet()) {
                if(entry.getKey() == null) continue;
                if(entry.getValue() == null) continue;
                if(entry.getValue().isEmpty()) continue;

                List<ESONObject> lst = new ArrayList<>();
                for (ESONObject item : entry.getValue()) {
                    boolean b = item.getJSONValue("isSelected",true);
                    if(!b) continue;
                    lst.add(item);
                }
                if(lst.isEmpty()) continue;
                m.put(entry.getKey(),lst);
            }
            hideLoadDialog();
            App.Post(()->{
                Point p = DensityUtils.getScreenSize();
                new EasyDialog(R.layout.dialog_offline_sampling_plan_upload,this)
                        .setOnBindDialogListener(new EasyDialog.OnBindDialogListener() {
                            @Override
                            public void onBindDialog(EasyDialogHolder easyDialogHolder) {
                                easyDialogHolder.setText(R.id.tv_title,"应上传记录");
                                easyDialogHolder.setOnClickListener(R.id.iv_close, v -> easyDialogHolder.dismissDialog());
                                ScrollView sv = easyDialogHolder.getView(R.id.sv_main);
                                ((LinearLayout)sv.getChildAt(0)).addView(OfflineSamplingHelper.buildOfflineViewList(NAOfflineSamplingUploadActivity.this,m),new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
                                easyDialogHolder.setVisible(R.id.tv_upload,false);
                            }
                        })
                        .setDialogParams(p.x,p.y - dp2px(80), Gravity.BOTTOM)
                        .setAllowDismissWhenBackPressed(false)
                        .setAllowDismissWhenTouchOutside(false)
                        .showDialog();
            });
        });
    }

    void showSuccessRecordDialog(){
        if(mSuccess.isEmpty()){
            ToastUtils.show("没有任何成功记录！");
            return;
        }

        Point p = DensityUtils.getScreenSize();
        new EasyDialog(R.layout.dialog_offline_sampling_plan_upload,this)
                .setOnBindDialogListener(new EasyDialog.OnBindDialogListener() {
                    @Override
                    public void onBindDialog(EasyDialogHolder easyDialogHolder) {
                        easyDialogHolder.setText(R.id.tv_title,"上传成功记录");
                        easyDialogHolder.setOnClickListener(R.id.iv_close, v -> easyDialogHolder.dismissDialog());
                        ScrollView sv = easyDialogHolder.getView(R.id.sv_main);
                        ((LinearLayout)sv.getChildAt(0)).addView(OfflineSamplingHelper.buildOfflineViewList(NAOfflineSamplingUploadActivity.this,mSuccess));
                        easyDialogHolder.setVisible(R.id.tv_upload,false);
                    }
                })
                .setDialogParams(p.x,p.y - dp2px(80), Gravity.BOTTOM)
                .setAllowDismissWhenBackPressed(false)
                .setAllowDismissWhenTouchOutside(false)
                .showDialog();
    }

    void showFailedRecordDialog(){
        if(mFailure.isEmpty()){
            ToastUtils.show("没有任何失败记录！");
            return;
        }

        Point p = DensityUtils.getScreenSize();
        new EasyDialog(R.layout.dialog_offline_sampling_plan_upload,this)
                .setOnBindDialogListener(new EasyDialog.OnBindDialogListener() {
                    @Override
                    public void onBindDialog(EasyDialogHolder easyDialogHolder) {
                        easyDialogHolder.setText(R.id.tv_title,"上传失败记录");
                        easyDialogHolder.setOnClickListener(R.id.iv_close, v -> easyDialogHolder.dismissDialog());
                        ScrollView sv = easyDialogHolder.getView(R.id.sv_main);
                        ((LinearLayout)sv.getChildAt(0)).addView(OfflineSamplingHelper.buildOfflineViewList(NAOfflineSamplingUploadActivity.this,mFailure),new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
                        easyDialogHolder.setVisible(R.id.tv_upload,false);
                    }
                })
                .setDialogParams(p.x,p.y - dp2px(80), Gravity.BOTTOM)
                .setAllowDismissWhenBackPressed(false)
                .setAllowDismissWhenTouchOutside(false)
                .showDialog();
    }

    @OnClick(R.id.tv_edit_offline_file)
    void onOfflineFileEditClicked(){
        final File fOld = new File(excel.getConfig().path);
        try {
            excel.close();
        } catch (Exception e) {
            e.printStackTrace();
            ToastUtils.show("文件关闭失败，请参照此文件目录下的backup备份文件夹下的文件进行补正和修复！");
            return;
        }

        NAOfflineSamplingEditActivity.editOfflineFile(this, fOld, new NAOfflineSamplingEditActivity.IOfflineEditCallback() {
            @Override
            public void onClosed(String newPath) {
                if(newPath.isEmpty()){
                    ToastUtils.show("文件被删除！");
                    finish();
                    return;
                }

                try {
                    excel = OfflineExcel.load(newPath);
                } catch (IOException e) {
                    e.printStackTrace();
                    ToastUtils.show("重新载入文件失败！");
                }
            }
        });
    }

    @OnClick(R.id.tv_upload)
    void onUploadClicked(){
        Point p = DensityUtils.getScreenSize();
        new EasyDialog(R.layout.dialog_offline_sampling_test_ping,this)
                .setOnBindDialogListener(new EasyDialog.OnBindDialogListener() {
                    ScrollView svMain;

                    TextView tvResults[] = new TextView[3];
                    TextView tvTimes  [] = new TextView[3];
                    TextView tvStates [] = new TextView[3];

                    TextView tvUpload;

                    void testPing(){
                        ThreadPoolProvider.getFixedThreadPool().execute(()->{
                            long lRequest [] = new long[]{0L,0L,0L};
                            int  iResult  [] = new int[]{0,0,0};
                            for(int i=0;i<3;++i){
                                final int id = i;
                                long startTime = System.currentTimeMillis();

                                ApiProvider.requestSamplingResultSync("100000000", new ApiProvider.ISamplingResultListener() {
                                    @Override
                                    public void onSuccess(ESONArray data) {
                                        iResult[id] = 1;
                                    }

                                    @Override
                                    public void onFailure(String err) {
                                        iResult[id] = 0;
                                    }
                                });

                                long endTime = System.currentTimeMillis();
                                lRequest[i] = endTime - startTime;
                                final int pos = i;
                                App.Post(()->setResult(pos,iResult[pos] == 1,lRequest[pos]));
                                try { Thread.sleep(1000); } catch (Exception e) { }
                            }

                            hideLoadDialog();
                        });

                    }

                    void setResult(int index, boolean bIsSucc, long time){
                        tvResults[index].setText(bIsSucc?"成功":"失败");
                        tvResults[index].setTextColor(Color.parseColor(bIsSucc?"#76ff03":"#d50000"));

                        tvTimes[index].setText(time+"ms");
                        int color = Color.BLACK;
                        if(!bIsSucc){//断开
                            color = Color.RED;
                            tvTimes[index].setText("?ms");
                            tvStates[index].setText("断开");
                            tvUpload.setVisibility(View.GONE);
                        }
                        else if(time<70){//顺畅
                            tvStates[index].setText("顺畅");
                            color = Color.parseColor("#00e676");
                        }
                        else if(time<200){//正常
                            tvStates[index].setText("正常");
                            color = Color.parseColor("#76ff03");
                        }
                        else if(time<400){//繁忙
                            tvStates[index].setText("繁忙");
                            color = Color.parseColor("#ff3d00");
                        }
                        else if(time<600){//拥堵
                            tvStates[index].setText("拥堵");
                            color = Color.parseColor("#e53935");
                        }
                        else{//
                            tvStates[index].setText("超时");
                            color = Color.RED;
                            tvUpload.setVisibility(View.GONE);
                        }

                        tvStates[index].setBackgroundColor(color);
                        if(tvUpload.getVisibility() == View.GONE) return;
                        if(tvUpload.getTag() == null){
                            tvUpload.setBackgroundColor(color);
                            tvUpload.setTag(time);
                        }
                        else{
                            long tag = (long) tvUpload.getTag();
                            if(time > tag){
                                tvUpload.setTag(time);
                                tvUpload.setBackgroundColor(color);
                            }
                        }
                    }

                    @Override
                    public void onBindDialog(EasyDialogHolder easyDialogHolder) {
                        easyDialogHolder.setOnClickListener(R.id.iv_close,v->easyDialogHolder.dismissDialog());
                        showLoadDialog();
                        svMain = easyDialogHolder.getView(R.id.sv_main);

                        tvResults[0] = svMain.findViewById(R.id.tv_req_r_1);
                        tvResults[1] = svMain.findViewById(R.id.tv_req_r_2);
                        tvResults[2] = svMain.findViewById(R.id.tv_req_r_3);

                        tvTimes  [0] = svMain.findViewById(R.id.tv_time_1);
                        tvTimes  [1] = svMain.findViewById(R.id.tv_time_2);
                        tvTimes  [2] = svMain.findViewById(R.id.tv_time_3);

                        tvStates [0] = svMain.findViewById(R.id.tv_state_1);
                        tvStates [1] = svMain.findViewById(R.id.tv_state_2);
                        tvStates [2] = svMain.findViewById(R.id.tv_state_3);

                        tvUpload = easyDialogHolder.getView(R.id.tv_upload);
                        tvUpload.setOnClickListener(v -> {
                            showPlanUploadDialog();
                            easyDialogHolder.dismissDialog();
                        });
                        testPing();
                    }
                })
                .setDialogParams(p.x,p.y - dp2px(80), Gravity.BOTTOM)
                .setAllowDismissWhenBackPressed(false)
                .setAllowDismissWhenTouchOutside(false)
                .showDialog();
    }

    //endregion

    //region presenter
    Map<String,List<ESONObject>> mUpload  = new LinkedHashMap<>();
    Map<String,List<ESONObject>> mSuccess = new HashMap<>();
    Map<String,List<ESONObject>> mFailure = new HashMap<>();
    int iAllCount = 0, iProcIndex = 0, iSuccessCount = 0, iFaulureCount = 0;

    void doUpload(){
        if(!mUpload.isEmpty()) return;
        tvUpload.setVisibility(View.GONE);
        showLoadDialog();
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            for (Map.Entry<String, List<ESONObject>> entry : mSelected.entrySet()) {
                if(entry.getKey() == null) continue;
                if(entry.getValue() == null) continue;
                if(entry.getValue().isEmpty()) continue;
                List<ESONObject> lst = new ArrayList<>();
                for(ESONObject item : entry.getValue()){
                    boolean selected = item.getJSONValue("isSelected",true);
                    if(!selected) continue;
                    lst.add(item);
                }
                if(lst.isEmpty()) continue;
                iAllCount+=lst.size();
                mUpload.put(entry.getKey(),lst);
            }
            iProcIndex = 1;
            iSuccessCount = 0;
            iFaulureCount = 0;
            App.Post(()->svUploadRecord.tvValue.setText(String.format("共%d条，点击查看。",iAllCount)));
            uploadInternal();
        });
    }
    public final String sUserName     = Template.getCurrentTemplate().getUserOverallDatabase().getNameFiledName();
    public final String sIdCardNoName = Template.getCurrentTemplate().getUserOverallDatabase().getIdFieldName();
    public final String sPhone        = Template.getCurrentTemplate().getPhoneFieldName();

    Map<String,Integer> mRetryCount = new HashMap<>();
    void uploadInternal(){

        while(!mUpload.isEmpty()){
            Map.Entry<String,List<ESONObject>> entry = mUpload.entrySet().iterator().next();
            List<ESONObject> lst = entry.getValue();
            if(lst == null || lst.isEmpty()){
                mUpload.remove(entry.getKey());
                continue;
            }

            setUploadMessage(String.format("正在处理第%d/%d条记录",iProcIndex,iAllCount));
            setUploadProgress((int) ((((float)iProcIndex)/iAllCount)*100));

            String id   = lst.get(0).getJSONValue("id"   ,"");
            String name = lst.get(0).getJSONValue("name" ,"");
            String phone= lst.get(0).getJSONValue("phone","");


            ESONObject eData = new ESONObject()
                                    .putValue(sIdCardNoName, id)
                                    .putValue(sUserName    , name)
                                    .putValue(sPhone       , phone);

            ESONArray arr = Template.getCurrentTemplate().getUserOverallDatabase().query(new ArrayList<String>(){{add(sIdCardNoName);}},new ArrayList<Object>(){{add(id);}});
            if(arr.length() == 1){
                eData = arr.getArrayValue(0,new ESONObject());
            }
            else{
                List<ESONObject> lstContainer = new ArrayList<>();
                ApiProvider.requestPeopleInfoByIdCardSync(id, new ApiProvider.IPeopleInfoListener() {
                    @Override
                    public void onSuccess(ESONObject data) {
                        if(data == null || data.length() ==0) return;
                        lstContainer.add(DataParser.parseDatabaseToShown(DatabaseConfig.TYPE_USER_OVERALL,DataParser.parseApiToDatabase(DatabaseConfig.TYPE_USER_OVERALL, Api.TYPE_GET_PEOPLE_INFO,data)));
                    }
                    @Override
                    public void onFailure(String err) { }
                });
                if(!lstContainer.isEmpty()){
                    eData = lstContainer.get(0);
                }
            }

            ApiProvider.requestNucleicAcidSamplingSync(entry.getKey(), eData, new ApiProvider.INCSamplingListener() {
                @Override
                public void onSuccess() {
                    TTSEngine.speakChinese(name);

                    List<ESONObject> lstSucc = mSuccess.get(entry.getKey());
                    if(lstSucc == null) lstSucc = new ArrayList<>();
                    lstSucc.add(lst.get(0));
                    mSuccess.put(entry.getKey(),lstSucc);
                    ++iProcIndex;
                    ++iSuccessCount;
                    setSuccessMessage(iSuccessCount);

                    lst.remove(0);
                    uploadInternal();
                }

                @Override
                public void onFailure(String err) {
                    App.Post(()->ToastUtils.show(err));
                    Integer iCount = mRetryCount.get(id);
                    if(iCount == null) iCount = 0;
                    ++iCount;
                    if(iCount>=3){
                        List<ESONObject> lstFail = mFailure.get(entry.getKey());
                        if(lstFail == null) lstFail = new ArrayList<>();
                        lstFail.add(lst.get(0));
                        lst.get(0).putValue("reason",err);
                        mFailure.put(entry.getKey(),lstFail);
                        ++iProcIndex;
                        ++iFaulureCount;
                        setFailureMessage(iFaulureCount);

                        lst.remove(0);
                        uploadInternal();
                        return;
                    }
                    try { Thread.sleep(500); } catch (Exception e) { }
                    mRetryCount.put(id,iCount);
                    uploadInternal();
                }
            });
            break;
        }
        if(mUpload.isEmpty()) {
            setUploadMessage("全部离线记录上传完毕！");
            setUploadProgress(100);
            doExport();
            return;
        }
    }

    void doExport(){
        try {
            File f = new File(String.format("%s/upload/%d.xls",new File(path).getParentFile().getAbsolutePath(),System.currentTimeMillis()));
            ExcelUtils.Writer writer = new ExcelUtils.Writer();
            writer.getWorkbook().createSheet();
            writer.getWorkbook().createSheet();
            int iSuccessCount = 0;
            for (Map.Entry<String, List<ESONObject>> entry : mSuccess.entrySet()) {
                if(entry.getKey() == null) continue;
                if(entry.getValue() == null || entry.getValue().isEmpty()) continue;

                for(ESONObject item:entry.getValue()){
                    String name = item.getJSONValue("name" ,"");
                    String id   = item.getJSONValue("id"   ,"");
                    String phone= item.getJSONValue("phone","");
                    writer.write(0,iSuccessCount,0,name );
                    writer.write(0,iSuccessCount,1,id   );
                    writer.write(0,iSuccessCount,2,phone);
                    ++iSuccessCount;
                }
            }

            int iFailureCount = 0;
            for (Map.Entry<String, List<ESONObject>> entry : mFailure.entrySet()) {
                if(entry.getKey() == null) continue;
                if(entry.getValue() == null || entry.getValue().isEmpty()) continue;

                for(ESONObject item:entry.getValue()){
                    String name   = item.getJSONValue("name"  ,"");
                    String id     = item.getJSONValue("id"    ,"");
                    String phone  = item.getJSONValue("phone" ,"");
                    String reason = item.getJSONValue("reason","");

                    writer.write(1,iFailureCount,0,name  );
                    writer.write(1,iFailureCount,1,id    );
                    writer.write(1,iFailureCount,2,phone );
                    writer.write(1,iFailureCount,3,reason);
                    ++iFailureCount;
                }
            }

            writer.getWorkbook().setSheetName(0,"上传成功记录");
            writer.getWorkbook().setSheetName(1,"上传失败记录");

            if(!f.getParentFile().exists()) f.getParentFile().mkdirs();
            if(f.exists()) f.delete();
            try { f.createNewFile(); } catch (Exception e) { }
            writer.save(f.getAbsolutePath());
        } catch (Exception e) {
            App.Post(()->ToastUtils.show("保存上传结果文件失败！"));
        }

        hideLoadDialog();
    }

    //endregion
}
