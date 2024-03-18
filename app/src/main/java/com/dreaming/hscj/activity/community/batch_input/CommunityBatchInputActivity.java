package com.dreaming.hscj.activity.community.batch_input;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dreaming.hscj.Constants;
import com.dreaming.hscj.R;
import com.dreaming.hscj.base.contract.BaseMVPActivity;
import com.dreaming.hscj.core.EasyAdapter.EasyAdapter;
import com.dreaming.hscj.core.ViewInjector;
import com.dreaming.hscj.dialog.DialogManager;
import com.dreaming.hscj.dialog.loading.LoadingDialog;
import com.dreaming.hscj.template.DataParser;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.template.api.wrapper.ApiParam;
import com.dreaming.hscj.template.database.DatabaseTemplate;
import com.dreaming.hscj.template.database.wrapper.DatabaseConfig;
import com.dreaming.hscj.utils.DensityUtils;
import com.dreaming.hscj.utils.EasyPermission;
import com.dreaming.hscj.utils.ToastUtils;
import com.leon.lfilepickerlibrary.LFilePicker;
import com.leon.lfilepickerlibrary.utils.Constant;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import priv.songxusheng.easydialog.EasyDialog;
import priv.songxusheng.easydialog.EasyDialogHolder;
import priv.songxusheng.easyjson.ESONObject;

public class CommunityBatchInputActivity extends BaseMVPActivity<CommunityBatchInputPresenter> implements ICommunityBatchInputContract.View, CompoundButton.OnCheckedChangeListener {
    @Override
    public int getContentViewResId() {
        return R.layout.activity_community_batch_input;
    }

    @Override
    protected void onBindView(View vContent) {
        ButterKnife.bind(this,vContent);
    }

    @BindView(R.id.part_database_info)
    CardView cvDatabaseInfo;
    @BindView(R.id.cv_rg)
    CardView cvRG;
    @BindView(R.id.cv_init_path)
    CardView cvInitPath;
    @BindView(R.id.rb_cover)
    RadioButton rbCover;
    @BindView(R.id.rb_user_determine)
    RadioButton rbUserDetermine;
    @BindView(R.id.rb_root_path)
    RadioButton rbRootPath;
    @BindView(R.id.rb_wx)
    RadioButton rbWX;
    @BindView(R.id.rb_qq)
    RadioButton rbQQ;
    @BindView(R.id.part_start_line)
    CardView cvPartStartLine;
    @BindView(R.id.part_sheet_index)
    CardView cvSheetIndex;
    @BindView(R.id.part_current_file)
    CardView cvCurrentFile;
    @BindView(R.id.part_process_progress)
    CardView cvProcessProgress;
    @BindView(R.id.part_success_read)
    CardView cvPartSuccessRead;
    @BindView(R.id.part_duplicated_read)
    CardView cvPartDuplicatedRead;
    @BindView(R.id.part_check_field_read)
    CardView cvCheckFieldRead; 
    @BindView(R.id.part_local_duplicated)
    CardView cvLocalDuplicated;
    @BindView(R.id.part_net_success)
    CardView cvNetSuccess;
    @BindView(R.id.part_local_save_failed)
    CardView cvLocalSaveFailed;
    @BindView(R.id.part_local_save_success)
    CardView cvLocalSaveSuccess;
    @BindView(R.id.part_net_empty)
    CardView cvNetEmpty;
    @BindView(R.id.part_net_failed)
    CardView cvNetFailed;
    @BindView(R.id.pb_process)
    ProgressBar pbProcess;

    TextView tvCurrentDatabaseName;
    TextView tvCurrentDatabaseValue;
    TextView tvStartLineName;
    EditText etStartLineValue;
    TextView tvSheetIndexName;
    EditText etSheetIndexValue;
    TextView tvCurrentFileName;
    TextView tvCurrentFileValue;
    TextView tvProgressName;
    TextView tvProgressValue;
    TextView tvSuccessReadName;
    TextView tvSuccessReadValue;
    TextView tvDuplicatedReadName;
    TextView tvDuplicatedReadValue;
    TextView tvCheckFieldReadName;
    TextView tvCheckFieldReadValue;
    TextView tvLocalDuplicatedName;
    TextView tvLocalDuplicatedValue;
    TextView tvNetSuccessName;
    TextView tvNetSuccessValue;
    TextView tvNetEmptyName;
    TextView tvNetEmptyValue;
    TextView tvNetFailedName;
    TextView tvNetFailedValue;
    TextView tvLocalSaveFailedName;
    TextView tvLocalSaveFailedValue;
    TextView tvLocalSaveSuccessName;
    TextView tvLocalSaveSuccessValue;

    @Override
    public void initView() {
        setCenterText("社区人员批量添加");
        setRightText("配置导入字段");
        tvTitleRight.setOnClickListener(v -> {
            startActivity(CommunityBatchInputConfigActivity.class);
        });

        rbCover.setOnCheckedChangeListener(this);
        rbUserDetermine.setOnCheckedChangeListener(this);
        rbRootPath.setOnCheckedChangeListener(this);
        rbWX.setOnCheckedChangeListener(this);
        rbQQ.setOnCheckedChangeListener(this);

        int mode = Constants.Config.getBatchInputMode();
        (mode == 0 ? rbCover : rbUserDetermine).setChecked(true);
        mPresenter.setCovered(mode==0);

        int path = Constants.Config.getBatchInputPathIndex();
        (path ==0 ? rbRootPath : path==1? rbWX : rbQQ).setChecked(true);


        tvCurrentDatabaseName = cvDatabaseInfo.findViewById(R.id.tv_name);
        tvCurrentDatabaseName.setText("当前数据库：");
        tvCurrentDatabaseName.setWidth(dp2px(150f));
        tvCurrentDatabaseName.invalidate();
        tvCurrentDatabaseValue= cvDatabaseInfo.findViewById(R.id.tv_value);
        ESONObject object = Constants.DBConfig.getSelectedDatabase();
        String sTownName    = object.getJSONValue("townName"   ,"");
        String sVillageName = object.getJSONValue("villageName","");
        tvCurrentDatabaseValue.setText(String.format("%s/%s/%s", Template.getCurrentTemplate().getDatabaseSetting().getRegionName(),sTownName,sVillageName));

        cvPartSuccessRead   .setOnClickListener(v -> showExcelSuccessPreviewDialog());
        cvPartDuplicatedRead.setOnClickListener(v -> showExcelDuplicatedPreviewDialog());
        cvCheckFieldRead    .setOnClickListener(v -> showExcelCheckFailedPreviewDialog());
        cvLocalDuplicated   .setOnClickListener(v -> showLocalDuplicatedPreviewDialog());
        cvNetSuccess        .setOnClickListener(v -> showNetSuccessPreviewDialog());
        cvNetEmpty          .setOnClickListener(v -> showNetEmptyPreviewDialog());
        cvNetFailed         .setOnClickListener(v -> showNetErrorPreviewDialog());
        cvLocalSaveFailed   .setOnClickListener(v -> showLocalSaveFailedPreviewDialog());
        cvLocalSaveSuccess  .setOnClickListener(v -> showLocalSaveSuccessPreviewDialog());

        unlockView();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if(isChecked){
            switch (buttonView.getId()){
                case R.id.rb_cover:
                    mPresenter.setCovered(true);
                    break;
                case R.id.rb_user_determine:
                    mPresenter.setCovered(false);
                    break;
                case R.id.rb_qq:
                    if(!new File(mPresenter.sQQRootPath).exists()){
                        rbRootPath.setChecked(true);
                        Toast.makeText(this, "未找到QQ存储文件夹！", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case R.id.rb_wx:
                    if(!new File(mPresenter.sWXRootPath).exists()){
                        rbRootPath.setChecked(true);
                        Toast.makeText(this, "未找到微信存储文件夹！", Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    }

    void lockView(){
        cvRG             .setEnabled(false);
        cvInitPath       .setEnabled(false);
        etStartLineValue .setEnabled(false);
        etSheetIndexValue.setEnabled(false);
        tvTitleRight     .setEnabled(false);
        rbRootPath.setEnabled(false);
        rbQQ.setEnabled(false);
        rbWX.setEnabled(false);
        rbCover.setEnabled(false);
        rbUserDetermine.setEnabled(false);
        tvChooseFile.setVisibility(View.INVISIBLE);
    }
    
    void unlockView(){
        cvPartSuccessRead   .setEnabled(false);
        cvPartDuplicatedRead.setEnabled(false);
        cvCheckFieldRead    .setEnabled(false);
        cvLocalDuplicated   .setEnabled(false);
        cvNetSuccess        .setEnabled(false);
        cvNetEmpty          .setEnabled(false);
        cvNetFailed         .setEnabled(false);
        cvLocalSaveFailed   .setEnabled(false);
        cvLocalSaveSuccess  .setEnabled(false);
        rbRootPath.setEnabled(true);
        rbQQ.setEnabled(true);
        rbWX.setEnabled(true);
        rbCover.setEnabled(true);
        rbUserDetermine.setEnabled(true);

        tvStartLineName = cvPartStartLine.findViewById(R.id.tv_name);
        tvStartLineName.setText("Excel起始行：");
        tvStartLineName.getLayoutParams().width = dp2px(150f);
        tvStartLineName.invalidate();
        etStartLineValue= cvPartStartLine.findViewById(R.id.ev_value);
        etStartLineValue.setText("1");
        etStartLineValue.setInputType(InputType.TYPE_NUMBER_FLAG_SIGNED);

        tvSheetIndexName = cvSheetIndex.findViewById(R.id.tv_name);
        tvSheetIndexName.setText("工作表序号：");
        tvSheetIndexName.getLayoutParams().width = dp2px(150f);
        tvSheetIndexName.invalidate();
        etSheetIndexValue= cvSheetIndex.findViewById(R.id.ev_value);
        etSheetIndexValue.setText("1");
        etSheetIndexValue.setInputType(InputType.TYPE_NUMBER_FLAG_SIGNED);

        tvCurrentFileName= cvCurrentFile.findViewById(R.id.tv_name);
        tvCurrentFileName.setText("当前选择文件：");
        tvCurrentFileName.getLayoutParams().width = dp2px(120f);
        tvCurrentFileName.invalidate();
        tvCurrentFileValue=cvCurrentFile.findViewById(R.id.tv_value);
        tvCurrentFileValue.setText("未选择");
        tvCurrentFileValue.setTextSize(TypedValue.COMPLEX_UNIT_SP,12f);
        tvCurrentFileValue.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        tvCurrentFileValue.setMaxLines(1);

        tvProgressName= cvProcessProgress.findViewById(R.id.tv_name);
        tvProgressName.setText("处理信息：");
        tvProgressName.getLayoutParams().width = LinearLayout.LayoutParams.WRAP_CONTENT;
        tvProgressValue=cvProcessProgress.findViewById(R.id.tv_value);
        tvProgressValue.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
        tvProgressValue.setText("等待文件选择");
        tvProgressValue.setTextSize(TypedValue.COMPLEX_UNIT_SP,14f);

        tvSuccessReadName= cvPartSuccessRead.findViewById(R.id.tv_name);
        tvSuccessReadName.setText("Excel成功读取：");
        tvSuccessReadName.getLayoutParams().width = dp2px(150f);
        tvSuccessReadName.invalidate();
        tvSuccessReadValue=cvPartSuccessRead.findViewById(R.id.tv_value);
        tvSuccessReadValue.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
        tvSuccessReadValue.setText("等待文件选择");

        tvDuplicatedReadName= cvPartDuplicatedRead.findViewById(R.id.tv_name);
        tvDuplicatedReadName.setText("Excel重复记录：");
        tvDuplicatedReadName.getLayoutParams().width = dp2px(150f);
        tvDuplicatedReadName.invalidate();
        tvDuplicatedReadValue=cvPartDuplicatedRead.findViewById(R.id.tv_value);
        tvDuplicatedReadValue.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
        tvDuplicatedReadValue.setText("等待文件选择");

        tvCheckFieldReadName= cvCheckFieldRead.findViewById(R.id.tv_name);
        tvCheckFieldReadName.setText("Excel校验错误：");
        tvCheckFieldReadName.getLayoutParams().width = dp2px(150f);
        tvCheckFieldReadName.invalidate();
        tvCheckFieldReadValue=cvCheckFieldRead.findViewById(R.id.tv_value);
        tvCheckFieldReadValue.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
        tvCheckFieldReadValue.setText("等待文件选择");

        tvLocalDuplicatedName= cvLocalDuplicated.findViewById(R.id.tv_name);
        tvLocalDuplicatedName.setText("本地重复记录：");
        tvLocalDuplicatedName.getLayoutParams().width = dp2px(150f);
        tvLocalDuplicatedName.invalidate();
        tvLocalDuplicatedValue=cvLocalDuplicated.findViewById(R.id.tv_value);
        tvLocalDuplicatedValue.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
        tvLocalDuplicatedValue.setText("等待文件选择");

        tvNetSuccessName= cvNetSuccess.findViewById(R.id.tv_name);
        tvNetSuccessName.setText("网络数据有效：");
        tvNetSuccessName.getLayoutParams().width = dp2px(150f);
        tvNetSuccessName.invalidate();
        tvNetSuccessValue=cvNetSuccess.findViewById(R.id.tv_value);
        tvNetSuccessValue.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
        tvNetSuccessValue.setText("等待文件选择");

        tvNetEmptyName= cvNetEmpty.findViewById(R.id.tv_name);
        tvNetEmptyName.setText("网络无此记录：");
        tvNetEmptyName.getLayoutParams().width = dp2px(150f);
        tvNetEmptyName.invalidate();
        tvNetEmptyValue=cvNetEmpty.findViewById(R.id.tv_value);
        tvNetEmptyValue.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
        tvNetEmptyValue.setText("等待文件选择");

        tvNetFailedName= cvNetFailed.findViewById(R.id.tv_name);
        tvNetFailedName.setText("网络请求错误：");
        tvNetFailedName.getLayoutParams().width = dp2px(150f);
        tvNetFailedName.invalidate();
        tvNetFailedValue=cvNetFailed.findViewById(R.id.tv_value);
        tvNetFailedValue.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
        tvNetFailedValue.setText("等待文件选择");

        tvLocalSaveFailedName= cvLocalSaveFailed.findViewById(R.id.tv_name);
        tvLocalSaveFailedName.setText("本地存储失败：");
        tvLocalSaveFailedName.getLayoutParams().width = dp2px(150f);
        tvLocalSaveFailedName.invalidate();
        tvLocalSaveFailedValue=cvLocalSaveFailed.findViewById(R.id.tv_value);
        tvLocalSaveFailedValue.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
        tvLocalSaveFailedValue.setText("等待文件选择");

        tvLocalSaveSuccessName= cvLocalSaveSuccess.findViewById(R.id.tv_name);
        tvLocalSaveSuccessName.setText("本地存储成功：");
        tvLocalSaveSuccessName.getLayoutParams().width = dp2px(150f);
        tvLocalSaveSuccessName.invalidate();
        tvLocalSaveSuccessValue=cvLocalSaveSuccess.findViewById(R.id.tv_value);
        tvLocalSaveSuccessValue.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
        tvLocalSaveSuccessValue.setText("等待文件选择");

        cvRG.setEnabled(true);
        cvInitPath.setEnabled(true);
        etStartLineValue.setEnabled(true);
        etSheetIndexValue.setEnabled(true);
        tvTitleRight.setEnabled(true);
        tvChooseFile.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == 1000) {
                List<String> list = data.getStringArrayListExtra(Constant.RESULT_INFO);
                File f = new File(list.isEmpty()?"":list.get(0));
                if(!f.exists()){
                    Toast.makeText(this, "文件不可读请重新选择！", Toast.LENGTH_SHORT).show();
                    return;
                }
                lockView();
                tvCurrentFileValue.setText(f.getAbsolutePath());
                int startRow = 0;
                try {
                    startRow = Integer.valueOf(etStartLineValue.getText().toString().trim()) - 1;
                } catch (Exception e) {
                    Toast.makeText(this, "起始行数不是整数！", Toast.LENGTH_SHORT).show();
                    etStartLineValue.requestFocus();
                    unlockView();
                    return;
                }
                if(startRow<0){
                    Toast.makeText(this, "起始行数不是正整数！", Toast.LENGTH_SHORT).show();
                    etStartLineValue.setText("1");
                    etStartLineValue.clearFocus();
                    etStartLineValue.requestFocus();
                    etStartLineValue.selectAll();
                    unlockView();
                    return;
                }

                int sheetIndex = 0;
                try {
                    sheetIndex = Integer.valueOf(etSheetIndexValue.getText().toString().trim()) - 1;
                } catch (Exception e) {
                    Toast.makeText(this, "工作表序号不是整数！", Toast.LENGTH_SHORT).show();
                    etSheetIndexValue.requestFocus();
                    unlockView();
                    return;
                }
                if(sheetIndex<0){
                    Toast.makeText(this, "工作表序号不是正整数！", Toast.LENGTH_SHORT).show();
                    etSheetIndexValue.setText("1");
                    etSheetIndexValue.clearFocus();
                    etSheetIndexValue.requestFocus();
                    etSheetIndexValue.selectAll();
                    unlockView();
                    return;
                }

                Constants.Config.setBatchInputMode(rbCover.isChecked()?0:1);
                Constants.Config.setBatchInputPathIndex(rbRootPath.isChecked()?0: rbWX.isChecked()?1:2);

                tvProgressValue.setText("开始处理Excel");
                LoadingDialog.showDialog("Read Excel",this);

                try {
                    mPresenter.readExcel(f.getAbsolutePath(),sheetIndex,Math.max(startRow,0));
                } catch (FileNotFoundException e) {
                    Toast.makeText(this, "文件不存在请重新选择！", Toast.LENGTH_SHORT).show();
                    unlockView();
                    LoadingDialog.dismissDialog("Read Excel");
                }catch (IOException e){
                    Toast.makeText(this, "文件读取失败请重新尝试！", Toast.LENGTH_SHORT).show();
                    unlockView();
                    LoadingDialog.dismissDialog("Read Excel");
                } catch (Exception e) {
                    Toast.makeText(this, "Excel版本不支持！", Toast.LENGTH_SHORT).show();
                    unlockView();
                    LoadingDialog.dismissDialog("Read Excel");
                }
            }
        }
    }

    @BindView(R.id.tv_choose_file)
    TextView tvChooseFile;
    @OnClick(R.id.tv_choose_file)
    void onChooseFileClicked(){
        requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, new EasyPermission.PermissionResultListener() {
            @Override
            public void onPermissionGranted() {
                String sInitPath = mPresenter.sRootPath;
                if(rbQQ.isChecked()) sInitPath = mPresenter.sQQRootPath;
                if(rbWX.isChecked()) sInitPath = mPresenter.sWXRootPath;

                new LFilePicker()
                        .withActivity(CommunityBatchInputActivity.this)
                        .withRequestCode(1000)
                        .withTitle("文件选择")
                        .withIconStyle(Constant.ICON_STYLE_BLUE)
                        .withBackIcon(Constant.BACKICON_STYLETWO)
                        .withMutilyMode(false)
                        .withMaxNum(1)
                        .withStartPath(sInitPath)//指定初始显示路径
                        .withNotFoundBooks("取消选择")
                        .withIsGreater(false)//过滤文件大小 小于指定大小的文件
                        .withFileSize(500 * 1024)//指定文件大小为500K
                        .withChooseMode(true)
                        .withFileFilter(new String[]{"xls", "xlsx"})
                        .start();
            }

            @Override
            public void onPermissionDenied() {
                ToastUtils.show("没有授予读取外置存储器权限，无法读取文件！");
            }
        });
    }

    @Override
    public void onReadExcelStarted() {
        tvProgressValue.setText("开始读取Excel数据！");
        tvSuccessReadValue.setText("无");
        tvCheckFieldReadValue.setText("无");
        tvDuplicatedReadValue.setText("无");
    }

    @Override
    public void onReadingExcel(int row, int column, int progress, int count, String value) {
        tvProgressValue.setText(String.format("正在读取%3d行%3d列: %s",row,column,value));
        pbProcess.setProgress(progress);
        tvSuccessReadValue.setText(String.format("%d条，读取完成后查看",count));
    }

    @Override
    public void onReadCheckFailed(int failedCount) {
        tvCheckFieldReadValue.setText(String.format("%d条，读取完成后查看",failedCount));
    }

    @Override
    public void onReadDuplicated(int duplicatedCount) {
        tvDuplicatedReadValue.setText(String.format("%d条，读取完成后查看",duplicatedCount));
    }

    @Override
    public void onReadError(String err) {
        Toast.makeText(CommunityBatchInputActivity.this, "Excel读取异常:"+err, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onReadFinished() {
        cvPartSuccessRead   .setEnabled(true);
        cvPartDuplicatedRead.setEnabled(true);
        cvCheckFieldRead    .setEnabled(true);
        tvSuccessReadValue    .setText(tvSuccessReadValue   .getText().toString().replaceAll("读取完成后查看","点击查看"));
        tvCheckFieldReadValue .setText(tvCheckFieldReadValue.getText().toString().replaceAll("读取完成后查看","点击查看"));
        tvDuplicatedReadValue .setText(tvDuplicatedReadValue.getText().toString().replaceAll("读取完成后查看","点击查看"));
        
        tvProgressValue.setText("Excel读取结束！");

        tvNetSuccessValue      .setText("等待数据同步！");
        tvNetEmptyValue        .setText("等待数据同步！");
        tvNetFailedValue       .setText("等待数据同步！");
        tvLocalDuplicatedValue .setText("等待数据同步！");
        tvLocalSaveFailedValue .setText("等待数据同步！");
        tvLocalSaveSuccessValue.setText("等待数据同步！");

        LoadingDialog.dismissDialog("Read Excel");

        Toast.makeText(this, "Excel读取结束!", Toast.LENGTH_SHORT).show();
        showPreviewExcelDialog();
    }

    @Override
    public void onProcessStart() {
        tvProgressValue.setText("开始同步数据！");
        pbProcess.setProgress(0);
        Toast.makeText(this, "开始同步数据！", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProcessSuccess(int successCount, int progress) {
        pbProcess.setProgress(progress);
        tvNetSuccessValue.setText(String.format("%d条，同步完成后查看",successCount));
    }

    @Override
    public void onProcessNetEmpty(int emptyCount) {
        tvNetEmptyValue.setText(String.format("%d条，同步完成后查看",emptyCount));
    }

    @Override
    public void onProcessDuplicated(int duplicatedCount) {
        tvLocalDuplicatedValue.setText(String.format("%d条，同步完成后查看",duplicatedCount));
    }

    @Override
    public void onProcessNetError(int errCount) {
        tvNetFailedValue.setText(String.format("%d条，同步完成后查看",errCount));
    }

    @Override
    public void onProcessLocalFailed(int failedCount) {
        tvLocalSaveFailedValue.setText(String.format("%d条，同步完成后查看",failedCount));
    }

    @Override
    public void onProcessLocalSuccess(int successCount) {
        tvLocalSaveSuccessValue.setText(String.format("%d条，同步完成后查看",successCount));
    }

    @Override
    public void onProcessFinished() {
        cvLocalDuplicated   .setEnabled(true);
        cvNetSuccess        .setEnabled(true);
        cvNetEmpty          .setEnabled(true);
        cvNetFailed         .setEnabled(true);
        cvLocalSaveFailed   .setEnabled(true);
        cvLocalSaveSuccess  .setEnabled(true);

        tvNetSuccessValue      .setText(tvNetSuccessValue      .getText().toString().replaceAll("同步完成后查看","点击查看"));
        tvNetEmptyValue        .setText(tvNetEmptyValue        .getText().toString().replaceAll("同步完成后查看","点击查看"));
        tvLocalDuplicatedValue .setText(tvLocalDuplicatedValue .getText().toString().replaceAll("同步完成后查看","点击查看"));
        tvNetFailedValue       .setText(tvNetFailedValue       .getText().toString().replaceAll("同步完成后查看","点击查看"));
        tvLocalSaveFailedValue .setText(tvLocalSaveFailedValue .getText().toString().replaceAll("同步完成后查看","点击查看"));
        tvLocalSaveSuccessValue.setText(tvLocalSaveSuccessValue.getText().toString().replaceAll("同步完成后查看","点击查看"));

        tvProgressValue.setText("数据导入完成！");

        if(!rbCover.isChecked()){
            showLocalDuplicatedPreviewDialog();
        }
        else{
            DialogManager.showAlertDialogWithConfirm(this,"数据导入完成！", null);
        }
    }

    @Override
    public void updateProcessMessage(String msg) {
        post(()->tvProgressValue.setText(msg));
    }

    //region Excel 读取
    //展示处理结束浏览及后续选项对话框
    void showPreviewExcelDialog(){
        if(mPresenter.getExcelPreviewData().isEmpty()){
            DialogManager.showAlertDialogWithConfirm(this,"没有读取到任何记录！", null);
            return;
        }
        Point p = DensityUtils.getScreenSize();
        new EasyDialog(R.layout.dialog_view_excel_and_continue,this)
                .setOnBindDialogListener(new EasyDialog.OnBindDialogListener() {
                    @Override
                    public void onBindDialog(EasyDialogHolder holder) {
                        holder.setText(R.id.tv_path,String.format("路径：%s",tvCurrentFileValue.getText().toString()));

                        RecyclerView rv = holder.getView(R.id.rv_view_excel);
                        rv.setHasFixedSize(true);
                        rv.setLayoutManager(new LinearLayoutManager(CommunityBatchInputActivity.this));
                        rv.setAdapter(new EasyAdapter(CommunityBatchInputActivity.this, R.layout.recy_detail_excel, mPresenter.getExcelPreviewData(), (EasyAdapter.IEasyAdapter<Pair<Integer, ESONObject>>) (holder1, data, position) -> {
                            holder1.setText(R.id.tv_index,String.valueOf(data.first+1));
                            holder1.setText(R.id.tv_name,data.second.getJSONValue(mPresenter.sUserName,""));
                            holder1.setText(R.id.tv_id_card_no,data.second.getJSONValue(mPresenter.sIdCardNoName,""));
                            holder1.setOnClickListener(R.id.tv_detail,v->showDataDetailDialog(data.second));
                        }));
                        rv.invalidate();

                        holder.setOnClickListener(R.id.tv_re_pick,v -> {
                            unlockView();
                            holder.dismissDialog();
                        });

                        holder.setOnClickListener(R.id.tv_start,v->{
                            holder.dismissDialog();
                            tvNetSuccessValue      .setText("无");
                            tvNetEmptyValue        .setText("无");
                            tvNetFailedValue       .setText("无");
                            tvLocalDuplicatedValue .setText("无");
                            tvLocalSaveFailedValue .setText("无");
                            tvLocalSaveSuccessValue.setText("无");
                            mPresenter.processExcel();
                        });
                    }
                })
                .setForegroundResource(R.drawable.shape_dialog_corner_with_tl_tr)
                .setDialogParams(p.x,p.y- ((int)DensityUtils.dp2px(50f)), Gravity.BOTTOM)
                .setAllowDismissWhenTouchOutside(false)
                .setAllowDismissWhenBackPressed(false)
                .showDialog();
    }

    //展示Excel读取的对话框
    private void showPreviewDialog(List<Pair<Integer, ESONObject>> lstData, String err){
        if(lstData.isEmpty()){
            DialogManager.showAlertDialogWithConfirm(this, err, null);
            return;
        }
        Point p = DensityUtils.getScreenSize();
        new EasyDialog(R.layout.dialog_view_excel,this)
                .setOnBindDialogListener(new EasyDialog.OnBindDialogListener() {
                    @Override
                    public void onBindDialog(EasyDialogHolder holder) {

                        holder.setOnClickListener(R.id.iv_close,v -> holder.dismissDialog());

                        RecyclerView rv = holder.getView(R.id.rv_view_excel);
                        rv.setHasFixedSize(true);
                        rv.setLayoutManager(new LinearLayoutManager(CommunityBatchInputActivity.this));
                        rv.setAdapter(new EasyAdapter(CommunityBatchInputActivity.this, R.layout.recy_detail_excel, lstData, (EasyAdapter.IEasyAdapter<Pair<Integer, ESONObject>>) (holder1, data, position) -> {
                            holder1.setText(R.id.tv_index,String.valueOf(data.first+1));
                            holder1.setOnClickListener(R.id.tv_index,v -> ViewInjector.copyClipboard(String.valueOf(data.first+1)));

                            holder1.setText(R.id.tv_name,data.second.getJSONValue(mPresenter.sUserName,""));
                            holder1.setOnClickListener(R.id.tv_name,v -> ViewInjector.copyClipboard(data.second.getJSONValue(mPresenter.sUserName,"")));

                            final String idCardNo = data.second.getJSONValue(mPresenter.sIdCardNoName,"");

                            holder1.setText(R.id.tv_id_card_no,idCardNo);
                            holder1.setOnClickListener(R.id.tv_id_card_no,v -> ViewInjector.copyClipboard(idCardNo));

                            holder1.setOnClickListener(R.id.tv_detail,v->showDataDetailDialog(data.second));
                        }));
                    }
                })
                .setForegroundResource(R.drawable.shape_dialog_corner_with_tl_tr)
                .setDialogParams(p.x,p.y- ((int)DensityUtils.dp2px(100f)), Gravity.BOTTOM)
                .showDialog();
    }

    void showExcelSuccessPreviewDialog(){
        showPreviewDialog(mPresenter.getExcelSuccessData(),"没有成功记录！");
    }

    void showExcelDuplicatedPreviewDialog(){
        showPreviewDialog(mPresenter.getExcelDuplicatedData(),"没有重复记录！");
    }

    void showExcelCheckFailedPreviewDialog(){
        showPreviewDialog(mPresenter.getExcelCheckFailedData(),"没有错误记录！");
    }
    //endregion

    //region 本地重复记录
    public interface ISavingListener{
        void onSuccess();
        void onFailure(String err);
    }
    ESONObject eConfig = Constants.Config.getBatchInputSavingUserDetermineConfig();
    private void showProcessDuplicatedDialog(List<Pair<Integer, ESONObject>> lstDatas, String err, int index, String idCardNo, RecyclerView.Adapter adapter, View.OnClickListener onProcessSuccess){
        if(lstDatas.isEmpty()){
            DialogManager.showAlertDialogWithConfirm(this,err, null);
            return;
        }
        Point p = DensityUtils.getScreenSize();
        new EasyDialog(R.layout.dialog_process_duplacated_excel,this)
                .setOnBindDialogListener(new EasyDialog.OnBindDialogListener() {
                    @Override
                    public void onBindDialog(EasyDialogHolder holder) {
                        final Map<Integer,EditText> mEditMapper = new TreeMap<>();

                        holder.setOnClickListener(R.id.iv_close,v -> holder.dismissDialog());

                        final List<Pair<String,String>> lstData = new ArrayList<>();
                        final ESONObject netData = mPresenter.getNetShownData(idCardNo);
                        final ESONObject dbData  = mPresenter.getLocalShownData(idCardNo);
                        final ESONObject defaultData = new ESONObject(dbData.toString());
                        final List<Pair<ApiParam,String>> lstSaving = new ArrayList<>();

                        final List<ApiParam> lstFields =  Template.getCurrentTemplate().getUserOverallDatabase().getConfig().getFields();
                        for(ApiParam p:lstFields){
                            String value = defaultData.getJSONValue(p.getName(),"");
                            lstData.add(new Pair<>(p.getName(),value));
                            lstSaving.add(new Pair<>(p,dbData.getJSONValue(p.getName(),"")));
                        }

                        RecyclerView rv = holder.getView(R.id.rv_view_excel);
                        rv.setHasFixedSize(true);
                        rv.setLayoutManager(new LinearLayoutManager(CommunityBatchInputActivity.this));
                        rv.setAdapter(new EasyAdapter(CommunityBatchInputActivity.this, R.layout.recy_excel_determine,lstData, (EasyAdapter.IEasyAdapter<Pair<String,String>>) (holder1, data, position) -> {
                            ApiParam p = lstFields.get(position);

                            final String sLocalValue = dbData .getJSONValue(p.getName(),"");
                            final String sNetValue   = netData.getJSONValue(p.getName(),"");

                            holder1.setText(R.id.tv_name ,lstFields.get(position).getDescription());
                            holder1.setText(R.id.tv_local,sLocalValue);
                            holder1.setText(R.id.tv_net  ,sNetValue);
                            holder1.setText(R.id.ev_value,data.second);

                            CheckBox rbDb    = holder1.getView(R.id.cb_local);
                            CheckBox rbNet   = holder1.getView(R.id.cb_net);
                            CheckBox rbInput = holder1.getView(R.id.cb_input);

                            final EditText et = holder1.getView(R.id.ev_value);
                            mEditMapper.put(position,et);
                            if(et.getTag()==null){
                                et.setTag(true);
                                et.addTextChangedListener(new TextWatcher() {
                                    @Override
                                    public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                                    @Override
                                    public void onTextChanged(CharSequence s, int start, int before, int count) { }
                                    @Override
                                    public void afterTextChanged(Editable s) {
                                        lstData.add(position,new Pair(data.first,s.toString()));
                                        lstData.remove(position+1);
                                    }
                                });
                            }

                            CompoundButton.OnCheckedChangeListener onCheckedChangeListener = (v,b)->{
                                if(b){
                                    switch (v.getId()){
                                        case R.id.cb_local:
                                            lstSaving.add(position,new Pair<>(p,sLocalValue));
                                            lstSaving.remove(position);
                                            rbNet  .setChecked(false);
                                            rbInput.setChecked(false);
                                            et.setEnabled(false);
                                            eConfig.putValue(p.getName(),0);
                                            break;
                                        case R.id.cb_net:
                                            lstSaving.add(position,new Pair<>(p,sNetValue));
                                            lstSaving.remove(position);
                                            rbDb   .setChecked(false);
                                            rbInput.setChecked(false);
                                            et.setEnabled(false);
                                            eConfig.putValue(p.getName(),1);
                                            break;
                                        case R.id.cb_input:
                                            lstSaving.add(position,new Pair<>(p,et.getText().toString()));
                                            lstSaving.remove(position);
                                            rbDb   .setChecked(false);
                                            rbNet  .setChecked(false);
                                            et.setEnabled(true);
                                            eConfig.putValue(p.getName(),2);
                                            showKeyboard(et);
                                            break;
                                    }
                                }
                            };
                            rbDb   .setOnCheckedChangeListener(onCheckedChangeListener);
                            rbNet  .setOnCheckedChangeListener(onCheckedChangeListener);
                            rbInput.setOnCheckedChangeListener(onCheckedChangeListener);

                            int config = eConfig.getJSONValue(p.getName(),0);
                            (config == 0? rbDb :(config==1?rbNet:rbInput)).setChecked(true);
                        }));

                        holder.setOnClickListener(R.id.tv_start,v->{
                            Constants.Config.setBatchInputSavingUserDetermineConfig(eConfig);
                            showSavingPreviewDialog(lstSaving,idCardNo,new ISavingListener() {
                                @Override
                                public void onSuccess() {
                                    ToastUtils.show("保存成功！");
                                    lstDatas.remove(index);
                                    tvLocalDuplicatedValue.setText(String.format("%d条，点击查看！",lstDatas.size()));
                                    if(lstDatas.isEmpty()){
                                        tvLocalDuplicatedValue.setText("无");
                                        ToastUtils.show("重复记录处理完毕！");
                                        onProcessSuccess.onClick(null);
                                    }
                                    adapter.notifyDataSetChanged();
                                    holder.dismissDialog();
                                }

                                @Override
                                public void onFailure(String err) {
                                    ToastUtils.show("数据保存失败！");
                                }
                            });
                        });
                    }
                })
                .setForegroundResource(R.drawable.shape_common_dialog)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        removeOnKeyBoardLayoutStateChangeListener();
                    }
                })
                .setAllowDismissWhenTouchOutside(false)
                .setDialogParams(p.x,p.y/2, Gravity.BOTTOM)
                .showDialog();
    }

    //预览待存储记录
    void showSavingPreviewDialog(List<Pair<ApiParam,String>> lstData, String idCardNo, ISavingListener listener){
        Point p = DensityUtils.getScreenSize();
        new EasyDialog(R.layout.dialog_saving_excel_preview,this)
                .setOnBindDialogListener(new EasyDialog.OnBindDialogListener() {
                    @Override
                    public void onBindDialog(EasyDialogHolder holder) {
                        TextView tvName = holder.getView(R.id.tv_name);
                        holder.setText(R.id.tv_name ,"字段名");
                        tvName.setTextColor(getResources().getColor(R.color.black));

                        TextView tvValue = holder.getView(R.id.tv_value);
                        tvValue.setTextColor(getResources().getColor(R.color.black));
                        holder.setText(R.id.tv_value,"字段值");

                        holder.setOnClickListener(R.id.iv_close,v -> holder.dismissDialog());

                        RecyclerView rv = holder.getView(R.id.rv_view_excel);
                        rv.setHasFixedSize(true);
                        rv.setLayoutManager(new LinearLayoutManager(CommunityBatchInputActivity.this));
                        rv.setAdapter(new EasyAdapter(CommunityBatchInputActivity.this, R.layout.view_shown,lstData, (EasyAdapter.IEasyAdapter<Pair<ApiParam,String>>) (holder1, data, position) -> {
                            ApiParam p = data.first;
                            holder1.setText(R.id.tv_name ,p.getDescription());
                            holder1.setText(R.id.tv_value,data.second);
                        }));

                        holder.setOnClickListener(R.id.tv_start,v->mPresenter.saveToLocal(lstData, new ISavingListener() {
                            @Override
                            public void onSuccess() {
                                holder.dismissDialog();
                                listener.onSuccess();
                            }

                            @Override
                            public void onFailure(String err) {
                                listener.onFailure(err);
                            }
                        }));
                    }
                })
                .setForegroundResource(R.drawable.shape_dialog_corner_with_tl_tr)
                .setDialogParams(p.x,p.y- ((int)DensityUtils.dp2px(160f)), Gravity.BOTTOM)
                .showDialog();
    }

    //展示重复处理的对话框
    //序号 姓名 身份号 处理
    private void showLocalDuplicatedPreviewDialog(List<Pair<Integer, ESONObject>> lstData, String err){
        if(lstData.isEmpty()){
            DialogManager.showAlertDialogWithConfirm(this, err, null);
            return;
        }
        Point p = DensityUtils.getScreenSize();
        new EasyDialog(R.layout.dialog_view_excel_duplicated,this)
                .setOnBindDialogListener(new EasyDialog.OnBindDialogListener() {
                    @Override
                    public void onBindDialog(EasyDialogHolder holder) {
                        holder.setOnClickListener(R.id.iv_close,v -> holder.dismissDialog());

                        RecyclerView rv = holder.getView(R.id.rv_view_excel);
                        rv.setHasFixedSize(true);
                        rv.setLayoutManager(new LinearLayoutManager(CommunityBatchInputActivity.this));
                        rv.setAdapter(new EasyAdapter(CommunityBatchInputActivity.this, R.layout.recy_process_excel, lstData, (EasyAdapter.IEasyAdapter<Pair<Integer, ESONObject>>) (holder1, data, position) -> {
                            holder1.setText(R.id.tv_title,"查看本地重复记录");

                            holder1.setText(R.id.tv_index,String.valueOf(data.first+1));
                            holder1.setOnClickListener(R.id.tv_index,v -> ViewInjector.copyClipboard(String.valueOf(data.first+1)));

                            holder1.setText(R.id.tv_name,data.second.getJSONValue(mPresenter.sUserName,""));
                            holder1.setOnClickListener(R.id.tv_name,v -> ViewInjector.copyClipboard(data.second.getJSONValue(mPresenter.sUserName,"")));

                            final String idCardNo = data.second.getJSONValue(mPresenter.sIdCardNoName,"");

                            holder1.setText(R.id.tv_id_card_no,idCardNo);
                            holder1.setOnClickListener(R.id.tv_id_card_no,v -> ViewInjector.copyClipboard(idCardNo));

                            holder1.setOnClickListener(
                                    R.id.tv_detail,
                                    v -> showProcessDuplicatedDialog(
                                            lstData,
                                            err,
                                            position,
                                            idCardNo,
                                            rv.getAdapter(),
                                            v2->{
                                                holder.dismissDialog();
                                                DialogManager.showAlertDialogWithConfirm(CommunityBatchInputActivity.this,"数据导入完成！", null);
                                            }
                                    )
                            );
                        },false));
                    }
                })
                .setForegroundResource(R.drawable.shape_dialog_corner_with_tl_tr)
                .setDialogParams(p.x,p.y- ((int)DensityUtils.dp2px(100f)), Gravity.BOTTOM)
                .showDialog();
    }

    void showLocalDuplicatedPreviewDialog(){
        showLocalDuplicatedPreviewDialog(mPresenter.getLocalDuplicatedData(),"没有本地重复记录！");
    }
    //endregion

    //展示已获取的数据
    //序号 姓名 身份号 详情
    private void showDataPreviewDialog(List<Pair<Integer, ESONObject>> lstData, String err){
        if(lstData.isEmpty()){
            DialogManager.showAlertDialogWithConfirm(this, err, null);
            return;
        }
        Point p = DensityUtils.getScreenSize();
        new EasyDialog(R.layout.dialog_preview_data_excel,this)
                .setOnBindDialogListener(new EasyDialog.OnBindDialogListener() {
                    @Override
                    public void onBindDialog(EasyDialogHolder holder) {
                        holder.setOnClickListener(R.id.iv_close,v -> holder.dismissDialog());

                        RecyclerView rv = holder.getView(R.id.rv_view_excel);
                        rv.setHasFixedSize(true);
                        rv.setLayoutManager(new LinearLayoutManager(CommunityBatchInputActivity.this));
                        rv.setAdapter(new EasyAdapter(CommunityBatchInputActivity.this, R.layout.recy_detail_excel, lstData, (EasyAdapter.IEasyAdapter<Pair<Integer, ESONObject>>) (holder1, data, position) -> {
                            holder1.setText(R.id.tv_index,String.valueOf(data.first+1));
                            holder1.setOnClickListener(R.id.tv_index,v -> ViewInjector.copyClipboard(String.valueOf(data.first+1)));

                            holder1.setText(R.id.tv_name,data.second.getJSONValue(mPresenter.sUserName,""));
                            holder1.setOnClickListener(R.id.tv_name,v -> ViewInjector.copyClipboard(data.second.getJSONValue(mPresenter.sUserName,"")));

                            final String idCardNo = data.second.getJSONValue(mPresenter.sIdCardNoName,"");

                            holder1.setText(R.id.tv_id_card_no,idCardNo);
                            holder1.setOnClickListener(R.id.tv_id_card_no,v -> ViewInjector.copyClipboard(idCardNo));

                            holder1.setOnClickListener(R.id.tv_detail,v -> showDataDetailDialog(data.second));
                        }));
                    }
                })
                .setForegroundResource(R.drawable.shape_dialog_corner_with_tl_tr)
                .setDialogParams(p.x,p.y- ((int)DensityUtils.dp2px(50f)), Gravity.BOTTOM)
                .showDialog();
    }

    //展示数据详情，不可修改记录
    //字段名 字段值
    private void showDataDetailDialog(ESONObject eData){
        Point p = DensityUtils.getScreenSize();
        new EasyDialog(R.layout.dialog_detail_data_excel,this)
                .setOnBindDialogListener(new EasyDialog.OnBindDialogListener() {
                    @Override
                    public void onBindDialog(EasyDialogHolder holder) {
                        holder.setOnClickListener(R.id.iv_close,v -> holder.dismissDialog());

                        TextView tvName = holder.getView(R.id.tv_name);
                        tvName.setText("字段名");
                        tvName.setTextColor(getResources().getColor(R.color.black));

                        TextView tvValue = holder.getView(R.id.tv_value);
                        tvValue.setText("字段值");
                        tvValue.setTextColor(getResources().getColor(R.color.black));

                        List<ApiParam> lst = Template.getCurrentTemplate().getUserOverallDatabase().getConfig().getFields();
                        ESONObject eShown = DataParser.parseObjects(eData,lst,false);
                        RecyclerView rv = holder.getView(R.id.rv_view_excel);
                        rv.setHasFixedSize(true);
                        rv.setLayoutManager(new LinearLayoutManager(CommunityBatchInputActivity.this));
                        rv.setAdapter(new EasyAdapter(CommunityBatchInputActivity.this, R.layout.view_shown, lst, (EasyAdapter.IEasyAdapter<ApiParam>) (holder1, data, position) -> {
                            String name = data.getDescription();
                            String value= eShown.getJSONValue(data.getName(),"");

                            holder1.setText(R.id.tv_name,name);
                            holder1.setOnClickListener(R.id.tv_name,v -> ViewInjector.copyClipboard(name));

                            holder1.setText(R.id.tv_value,value);
                            holder1.setOnClickListener(R.id.tv_value,v -> ViewInjector.copyClipboard(value));
                        }));
                    }
                })
                .setForegroundResource(R.drawable.shape_dialog_corner_with_tl_tr)
                .setDialogParams(p.x,p.y- ((int)DensityUtils.dp2px(100f)), Gravity.BOTTOM)
                .showDialog();
    }

    void showNetSuccessPreviewDialog(){
        showDataPreviewDialog(mPresenter.getNetSuccessData(),"没有网络成功记录！");
    }

    void showNetEmptyPreviewDialog(){
        showPreviewDialog(mPresenter.getNetEmptyData(),"没有网络空数据记录！");
    }
    
    void showNetErrorPreviewDialog(){
        showPreviewDialog(mPresenter.getNetErrorData(),"没有网络错误记录！");
    }

    void showLocalSaveFailedPreviewDialog(){
        showPreviewDialog(mPresenter.getLocalSaveFailedData(),"没有本地存储失败记录！");
    }

    void showLocalSaveSuccessPreviewDialog(){
        showDataPreviewDialog(mPresenter.getLocalSaveSuccessData(),"没有本地存储成功记录！");
    }

    @Override
    protected void onResume() {
        CommunityBatchInputConfigActivity.savingConfig();
        super.onResume();
    }

    void showExitAlertToContinue(View.OnClickListener onConfirm, View.OnClickListener onCancel){
        DialogManager.showAlertDialog(this,"提示","当前导入未完成，确认要结束吗？",onCancel,onConfirm);
    }

    @Override
    public void onBackPressed() {
        if(mPresenter.canFinish()) {
            super.onBackPressed();
            return;
        }
        showExitAlertToContinue(
                v->super.onBackPressed(),
                v->{
                    if(!rbCover.isChecked()){
                        showLocalDuplicatedPreviewDialog();
                    }
                }
        );
    }
}
