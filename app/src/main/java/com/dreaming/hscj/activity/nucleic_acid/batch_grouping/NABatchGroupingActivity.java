package com.dreaming.hscj.activity.nucleic_acid.batch_grouping;

import android.Manifest;
import android.content.Intent;
import android.graphics.Point;
import android.util.Pair;
import android.view.Gravity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dreaming.hscj.Constants;
import com.dreaming.hscj.R;
import com.dreaming.hscj.activity.community.batch_input.CommunityBatchInputActivity;
import com.dreaming.hscj.activity.community.recovery.CommunityRecoveryActivity;
import com.dreaming.hscj.activity.nucleic_acid.grouping.NAGroupInDetailActivity;
import com.dreaming.hscj.base.contract.BaseMVPActivity;
import com.dreaming.hscj.core.EasyAdapter.EasyAdapter;
import com.dreaming.hscj.dialog.DialogManager;
import com.dreaming.hscj.dialog.loading.LoadingDialog;
import com.dreaming.hscj.template.DataParser;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.template.api.wrapper.ApiParam;
import com.dreaming.hscj.template.database.wrapper.DatabaseConfig;
import com.dreaming.hscj.template.database.wrapper.ISetterListener;
import com.dreaming.hscj.utils.DensityUtils;
import com.dreaming.hscj.utils.EasyPermission;
import com.dreaming.hscj.utils.ToastUtils;
import com.dreaming.hscj.widget.InputView;
import com.dreaming.hscj.widget.ShownView;
import com.leon.lfilepickerlibrary.LFilePicker;
import com.leon.lfilepickerlibrary.utils.Constant;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import priv.songxusheng.easydialog.EasyDialog;
import priv.songxusheng.easydialog.EasyDialogHolder;
import priv.songxusheng.easyjson.ESONObject;

public class NABatchGroupingActivity extends BaseMVPActivity<NABatchGroupingPresenter> implements INABatchGroupingContract.View {
    @Override
    public int getContentViewResId() {
        return R.layout.activity_nucleic_acid_batch_grouping;
    }

    @Override
    protected void onBindView(View vContent) {
        ButterKnife.bind(this,vContent);
    }

    //region 字段声明
    //文件选择路径
    @BindView(R.id.rb_root_path)
    RadioButton rbRootPath;
    @BindView(R.id.rb_wx)
    RadioButton rbWX;
    @BindView(R.id.rb_qq)
    RadioButton rbQQ;

    CompoundButton.OnCheckedChangeListener onPathChanged = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (!isChecked) return;
            switch (buttonView.getId()){
                case R.id.rb_root_path:
                    mPresenter.setPath(0);
                    break;
                case R.id.rb_wx:
                    mPresenter.setPath(1);
                    break;
                case R.id.rb_qq:
                    mPresenter.setPath(2);
                    break;
            }
        }
    };

    @BindView(R.id.sv_db_info)
    ShownView svDbInfo;
    @BindView(R.id.iv_start_line)
    InputView ivStartLine;
    @BindView(R.id.iv_sheet_index)
    InputView ivSheetIndex;
    @BindView(R.id.pb_process)
    ProgressBar pbProcess;
    @BindView(R.id.sv_selected_file)
    ShownView svSelectedFile;
    @BindView(R.id.sv_progress)
    ShownView svProgress;
    @BindView(R.id.sv_group_read_excel)
    ShownView svGroupReadExcel;
    @BindView(R.id.sv_group_excel_err)
    ShownView svGroupExcelErr;
    @BindView(R.id.sv_group_limit_err)
    ShownView svGroupLimitErr;
    @BindView(R.id.sv_member_empty)
    ShownView svMemberEmpty;
    @BindView(R.id.sv_group_duplicated)
    ShownView svGroupDuplicated;
    @BindView(R.id.sv_save_group_ok)
    ShownView svGroupSaveOk;
    @BindView(R.id.sv_save_group_err)
    ShownView svGroupSaveErr;

    @BindView(R.id.tv_start)
    TextView tvSelectFile;
    //endregion

    @Override
    public void initView() {
        setCenterText("核酸分组批量导入");

        setRightText("配置导入字段");

        ESONObject object = Constants.DBConfig.getSelectedDatabase();
        String sTownName    = object.getJSONValue("townName"   ,"");
        String sVillageName = object.getJSONValue("villageName","");
        svDbInfo.tvValue.setText(String.format("%s/%s/%s", Template.getCurrentTemplate().getDatabaseSetting().getRegionName(),sTownName,sVillageName));

        tvTitleRight.setOnClickListener(v -> {
            startActivity(NABatchGroupingConfigActivity.class);
        });

        DialogManager.showAlertDialogWithConfirm(this,"Excel导入将清除本地已分组记录！",null);

        ESONObject eConfig = Constants.Config.getBatchGroupingConfig();

        int path = eConfig.getJSONValue("path",0);
        mPresenter.setPath(path);
        switch (path){
            case 0:
                rbRootPath.setChecked(true);
                break;
            case 1:
                rbWX.setChecked(true);
                break;
            case 2:
                rbQQ.setChecked(true);
                break;
        }

        rbRootPath     .setOnCheckedChangeListener(onPathChanged);
        rbQQ           .setOnCheckedChangeListener(onPathChanged);
        rbWX           .setOnCheckedChangeListener(onPathChanged);

        ivStartLine .tvName.getPaint().setFakeBoldText(true);
        ivSheetIndex.tvName.getPaint().setFakeBoldText(true);

        svGroupReadExcel     .tvValue.setOnClickListener(v -> showReadGroupExcelDialog());
        svGroupExcelErr      .tvValue.setOnClickListener(v -> showReadGroupExcelErrorDialog());
        svGroupLimitErr      .tvValue.setOnClickListener(v -> showReadGroupLimitErrorDialog());
        svMemberEmpty        .tvValue.setOnClickListener(v -> showReadGroupMemberEmptyDialog());
        svGroupDuplicated    .tvValue.setOnClickListener(v -> showReadGroupDuplicatedDialog());

        svGroupSaveOk        .tvValue.setOnClickListener(v -> showSaveGroupSuccessPreviewDialog());
        svGroupSaveErr       .tvValue.setOnClickListener(v -> showSaveGroupFailurePreviewDialog());

        unlockView();
    }

    @Override
    protected void onDestroy() {
        Template.getCurrentTemplate().getNoneGroupingDatabase().sync(null);
        super.onDestroy();
    }

    //序号 组号 身份号 姓名 操作
    private void showPreviewExcelDialog(List<Pair<Integer,ESONObject>> lstData, String errTag, String title){
        if(lstData == null || lstData.isEmpty()){
            ToastUtils.show(errTag);
            return;
        }
        Point p = DensityUtils.getScreenSize();
        new EasyDialog(R.layout.dialog_batch_grouping_preview_5,this)
                .setOnBindDialogListener(new EasyDialog.OnBindDialogListener() {
                    @Override
                    public void onBindDialog(EasyDialogHolder holder) {
                        holder.setOnClickListener(R.id.iv_close,v -> holder.dismissDialog());
                        holder.setText(R.id.tv_title,title);
                        RecyclerView rv = holder.getView(R.id.rv_preview);
                        rv.setHasFixedSize(true);
                        rv.setLayoutManager(new LinearLayoutManager(NABatchGroupingActivity.this));
                        rv.setAdapter(new EasyAdapter(NABatchGroupingActivity.this, R.layout.recy_batch_grouping_preview_5, lstData, (EasyAdapter.IEasyAdapter<Pair<Integer, ESONObject>>) (holder1, data, position) -> {
                            holder1.setText(R.id.tv_index,String.valueOf(data.first+1));
                            holder1.setText(R.id.tv_group_id,data.second.getJSONValue(mPresenter.sGroupName,""));
                            holder1.setText(R.id.tv_name,data.second.getJSONValue(mPresenter.sNameFieldName,""));
                            holder1.setText(R.id.tv_id_card_no,data.second.getJSONValue(mPresenter.sIdCardNoName,""));
                            holder1.setOnClickListener(R.id.tv_detail,v->showDetailExcelDialog(data.second));
                        }));
                        rv.invalidate();
                    }
                })
                .setForegroundResource(R.drawable.shape_dialog_corner_with_tl_tr)
                .setDialogParams(p.x,p.y- ((int)DensityUtils.dp2px(80f)), Gravity.BOTTOM)
                .setAllowDismissWhenTouchOutside(false)
                .setAllowDismissWhenBackPressed(false)
                .showDialog();
    }

    private void showDetailExcelDialog(ESONObject eData){
        Point p = DensityUtils.getScreenSize();
        new EasyDialog(R.layout.dialog_batch_grouping_detail,this)
                .setOnBindDialogListener(new EasyDialog.OnBindDialogListener() {
                    @Override
                    public void onBindDialog(EasyDialogHolder holder) {
                        holder.setOnClickListener(R.id.iv_close,v -> holder.dismissDialog());

                        RecyclerView rv = holder.getView(R.id.rv_preview);
                        rv.setHasFixedSize(true);
                        rv.setLayoutManager(new LinearLayoutManager(NABatchGroupingActivity.this));
                        List<ApiParam> lstParams = Template.getCurrentTemplate().getNucleicAcidGroupingDatabase().getConfig().getFields();
                        EasyAdapter adapter = new EasyAdapter(NABatchGroupingActivity.this, R.layout.view_shown, lstParams, (EasyAdapter.IEasyAdapter<ApiParam>) (holder1, data, position) -> {
                            holder1.setText(R.id.tv_name,data.getDescription());
                            holder1.setText(R.id.tv_value,eData.getJSONValue(data.getName(),""));
                        });
                        rv.setAdapter(adapter);
                    }
                })
                .setForegroundResource(R.drawable.shape_dialog_corner_with_tl_tr)
                .setDialogParams(p.x,p.y- ((int)DensityUtils.dp2px(140f)), Gravity.BOTTOM)
                .showDialog();
    }

    //序号 组号 身份号
    private void showPreviewExcelWithoutDetailDialog(List<Pair<Integer,ESONObject>> lstData, String errTag, String title){
        if(lstData == null || lstData.isEmpty()){
            ToastUtils.show(errTag);
            return;
        }
        Point p = DensityUtils.getScreenSize();
        new EasyDialog(R.layout.dialog_batch_grouping_preview_3,this)
                .setOnBindDialogListener(new EasyDialog.OnBindDialogListener() {
                    @Override
                    public void onBindDialog(EasyDialogHolder holder) {
                        holder.setOnClickListener(R.id.iv_close,v -> holder.dismissDialog());
                        holder.setText(R.id.tv_title,title);
                        RecyclerView rv = holder.getView(R.id.rv_preview);
                        rv.setHasFixedSize(true);
                        rv.setLayoutManager(new LinearLayoutManager(NABatchGroupingActivity.this));
                        rv.setAdapter(new EasyAdapter(NABatchGroupingActivity.this, R.layout.recy_batch_grouping_preview_3, lstData, (EasyAdapter.IEasyAdapter<Pair<Integer, ESONObject>>) (holder1, data, position) -> {
                            holder1.setText(R.id.tv_index,String.valueOf(data.first+1));
                            holder1.setText(R.id.tv_group_id,data.second.getJSONValue(mPresenter.sGroupName,""));
                            holder1.setText(R.id.tv_id_card_no,data.second.getJSONValue(mPresenter.sIdCardNoName,""));
                        }));
                        rv.invalidate();
                    }
                })
                .setForegroundResource(R.drawable.shape_dialog_corner_with_tl_tr)
                .setDialogParams(p.x,p.y- ((int)DensityUtils.dp2px(80f)), Gravity.BOTTOM)
                .setAllowDismissWhenTouchOutside(false)
                .setAllowDismissWhenBackPressed(false)
                .showDialog();
    }
    //序号 组号 身份号
    private void showReadGroupExcelErrorDialog() {
        showPreviewExcelWithoutDetailDialog(mPresenter.getReadGroupExcelFailure(),"没有导入错误记录！","查看导入错误记录");
    }
    //序号 组号 身份号
    private void showReadGroupDuplicatedDialog() {
        showPreviewExcelWithoutDetailDialog(mPresenter.getReadGroupExcelDuplicated(),"Excel内没有重复记录！","Excel内重复记录");
    }
    //序号 组号 身份号
    private void showReadGroupLimitErrorDialog(){
        showPreviewExcelWithoutDetailDialog(mPresenter.getReadGroupLimitError(),"没有分组超限记录！","查看分组超限记录");
    }
    //序号 组号 身份号 姓名 操作
    private void showReadGroupExcelDialog() {
        showPreviewExcelDialog(mPresenter.getReadGroupExcelSuccess(),"没有读取到任何记录！","查看Excel读取记录");
    }
    //序号 组号 身份号
    private void showReadGroupMemberEmptyDialog() {
        showPreviewExcelWithoutDetailDialog(mPresenter.getReadGroupMemberEmpty(),"没有本地人员未匹配记录！","查看本地人员未匹配记录");
    }

    void lockView(){
        rbRootPath     .setEnabled(false);
        rbQQ           .setEnabled(false);
        rbWX           .setEnabled(false);


        svGroupReadExcel     .tvValue.setEnabled(false);
        svGroupExcelErr      .tvValue.setEnabled(false);
        svMemberEmpty        .tvValue.setEnabled(false);
        svGroupDuplicated    .tvValue.setEnabled(false);
        svGroupLimitErr      .tvValue.setEnabled(false);

        svGroupSaveOk        .tvValue.setEnabled(false);
        svGroupSaveErr       .tvValue.setEnabled(false);

        tvSelectFile.setVisibility(View.INVISIBLE);
    }

    void unlockView(){
        rbRootPath     .setEnabled(true);
        rbQQ           .setEnabled(true);
        rbWX           .setEnabled(true);

        svGroupReadExcel     .tvValue.setEnabled(false);
        svGroupExcelErr      .tvValue.setEnabled(false);
        svMemberEmpty        .tvValue.setEnabled(false);
        svGroupDuplicated    .tvValue.setEnabled(false);
        svGroupLimitErr      .tvValue.setEnabled(false);

        svGroupSaveOk  .tvValue.setEnabled(false);
        svGroupSaveErr .tvValue.setEnabled(false);

        pbProcess.setProgress(0);
        svSelectedFile .tvValue.setText("未选择");

        svGroupReadExcel     .tvValue.setText("等待文件选择");
        svGroupExcelErr      .tvValue.setText("等待文件选择");
        svMemberEmpty        .tvValue.setText("等待文件选择");
        svGroupDuplicated    .tvValue.setText("等待文件选择");
        svGroupLimitErr      .tvValue.setText("等待文件选择");

        svGroupSaveOk  .tvValue.setText("等待文件选择");
        svGroupSaveErr .tvValue.setText("等待文件选择");

        svProgress.tvValue.setText("");

        tvSelectFile.setVisibility(View.VISIBLE);
    }

    void showExitAlertToContinue(View.OnClickListener onConfirm, View.OnClickListener onCancel){
        DialogManager.showAlertDialog(this,"提示","当前导入未完成，确认要结束吗？",onCancel,onConfirm);
    }

    @Override
    public void onBackPressed() {
        if(mPresenter.getProgress() == mPresenter.PROGRESS_PROCESSING){
            if(!mPresenter.getSaveGroupFailure().isEmpty() ) {
                showExitAlertToContinue(
                        v ->
                                DialogManager.showAlertDialog(this,"提示","要保存已还原记录吗？",
                                        v1 -> {
                                            mPresenter.markTransactionFailure();
                                            super.onBackPressed();
                                        },
                                        v2 -> {
                                            mPresenter.markTransactionSuccess();
                                            super.onBackPressed();
                                        }
                                ),
                        v ->{}
//                        v -> {
//                            if(!mPresenter.getReadGroupExcelDuplicated().isEmpty()){
//                                showReadGroupDuplicatedDialog();
//                            }
//                        }
                );
                return;
            }
        }
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == 1000) {
                List<String> list = data.getStringArrayListExtra(Constant.RESULT_INFO);
                File f = new File(list.isEmpty() ? "" : list.get(0));
                if (!f.exists()) {
                    Toast.makeText(this, "文件不可读请重新选择！", Toast.LENGTH_SHORT).show();
                    return;
                }
                svSelectedFile.tvValue.setText(f.getAbsolutePath());
                int startRow = 0;
                EditText etStartLineValue = ivStartLine.tvValue;
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

                EditText etSheetIndexValue = ivSheetIndex.tvValue;
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
                onStartRead(f.getAbsolutePath(),sheetIndex,startRow);
            }
        }
    }

    @OnClick(R.id.tv_start)
    void onSelectFileClicked(){
        requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, new EasyPermission.PermissionResultListener() {
            @Override
            public void onPermissionGranted() {
                String sInitPath = mPresenter.sRootPath;
                if(rbQQ.isChecked()) sInitPath = mPresenter.sQQRootPath;
                if(rbWX.isChecked()) sInitPath = mPresenter.sWXRootPath;

                Constants.Config.setBatchGroupingConfig(new ESONObject().putValue("path",mPresenter.getPath()));

                new LFilePicker()
                        .withActivity(NABatchGroupingActivity.this)
                        .withRequestCode(1000)
                        .withTitle("文件选择")
                        .withIconStyle(Constant.ICON_STYLE_BLUE)
                        .withBackIcon(Constant.BACKICON_STYLETWO)
                        .withMutilyMode(false)
                        .withMaxNum(1)
                        .withStartPath(sInitPath)//指定初始显示路径
                        .withNotFoundBooks("取消选择")
                        .withIsGreater(true)
                        .withFileSize(0)
                        .withChooseMode(true)
                        .withFileFilter(new String[]{"xls","xlsx"})
                        .start();
            }

            @Override
            public void onPermissionDenied() {
                ToastUtils.show("没有授予读取外置存储器权限，无法读取文件！");
            }
        });
    }

    private void onStartRead(String path, int sheet, int startLine){
        lockView();

        pbProcess.setProgress(0);

        svGroupReadExcel     .tvValue.setText("无记录");
        svGroupExcelErr      .tvValue.setText("无记录");
        svMemberEmpty        .tvValue.setText("无记录");
        svGroupDuplicated    .tvValue.setText("无记录");
        svGroupLimitErr      .tvValue.setText("无记录");

        svGroupSaveOk  .tvValue.setText("等待记录读取完毕！");
        svGroupSaveErr .tvValue.setText("等待记录读取完毕！");

        LoadingDialog.showDialog("Read Excel",this);

        try {
            mPresenter.read(path,sheet,startLine);
        }  catch (FileNotFoundException e) {
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

    @Override
    public void onReading(int row, int column, int progress, int count, String value) {
        pbProcess.setProgress(progress);
        svProgress.tvValue.setText(String.format("正在读取%3d行%3d列: %s",row,column,value));
    }

    @Override
    public void onReadingGroupExcelCount(int count) {
        svGroupReadExcel.tvValue.setText(String.format("共%d条，读取完毕后查看。",count));
    }

    @Override
    public void onReadingGroupErrorCount(int count) {
        svGroupExcelErr.tvValue.setText(String.format("共%d条，读取完毕后查看。",count));
    }

    @Override
    public void onReadingGroupDuplicatedCount(int count) {
        svGroupDuplicated.tvValue.setText(String.format("共%d条，读取完毕后查看。",count));
    }

    @Override
    public void onReadingGroupLimitErrorCount(int count) {
        svGroupLimitErr.tvValue.setText(String.format("共%d条，读取完毕后查看。",count));
    }

    @Override
    public void onReadingGroupMemberEmpty(int count) {
        svMemberEmpty.tvValue.setText(String.format("共%d条，读取完毕后查看。",count));
    }

    @Override
    public void onReadSuccess() {
        LoadingDialog.dismissDialog("Read Excel");

        svProgress.tvValue.setText("Excel读取完毕");

        pbProcess.setProgress(100);

        svGroupReadExcel     .tvValue.setEnabled(true);
        svGroupExcelErr      .tvValue.setEnabled(true);
        svMemberEmpty        .tvValue.setEnabled(true);
        svGroupDuplicated    .tvValue.setEnabled(true);
        svGroupLimitErr      .tvValue.setEnabled(true);

        svGroupReadExcel     .tvValue.setText(svGroupReadExcel     .tvValue.getText().toString().replaceAll("读取完毕后查看","点击查看"));
        svGroupExcelErr      .tvValue.setText(svGroupExcelErr      .tvValue.getText().toString().replaceAll("读取完毕后查看","点击查看"));
        svMemberEmpty        .tvValue.setText(svMemberEmpty        .tvValue.getText().toString().replaceAll("读取完毕后查看","点击查看"));
        svGroupDuplicated    .tvValue.setText(svGroupDuplicated    .tvValue.getText().toString().replaceAll("读取完毕后查看","点击查看"));
        svGroupLimitErr      .tvValue.setText(svGroupLimitErr      .tvValue.getText().toString().replaceAll("读取完毕后查看","点击查看"));

        svGroupSaveOk  .tvValue.setText("无记录");
        svGroupSaveErr .tvValue.setText("无记录");

        showReadDataPreviewDialog();
    }

    @Override
    public void onReadError(String err) {
        unlockView();
        LoadingDialog.dismissDialog("Read Excel");
        DialogManager.showAlertDialogWithConfirm(this,"通知","数据读取失败："+err,null);
    }

    private void showReadDataPreviewDialog(){
        List<Pair<Integer, ESONObject>> lstLimitErr = mPresenter.getReadGroupLimitError();
        if(!lstLimitErr.isEmpty()){
            showReadGroupLimitErrorDialog();
            DialogManager.showAlertDialogWithConfirm(this,"分组内含有超限记录，导入失败！",null);
            unlockView();
            return;
        }
        List<Pair<Integer, ESONObject>> lstReadSuccess = mPresenter.getReadGroupExcelSuccess();
        if(lstReadSuccess.isEmpty()){
            DialogManager.showAlertDialogWithConfirm(this,"没有读取到数据！",null);
            return;
        }

        Point p = DensityUtils.getScreenSize();
        new EasyDialog(R.layout.dialog_batch_grouping_preview_and_continue,this)
                .setOnBindDialogListener(new EasyDialog.OnBindDialogListener() {
                    @Override
                    public void onBindDialog(EasyDialogHolder holder) {
                        holder.setText(R.id.tv_path,String.format("路径：%s",svSelectedFile.tvValue.getText().toString()));

                        RecyclerView rv = holder.getView(R.id.rv_view_excel);
                        rv.setHasFixedSize(true);
                        rv.setLayoutManager(new LinearLayoutManager(NABatchGroupingActivity.this));
                        rv.setAdapter(new EasyAdapter(NABatchGroupingActivity.this, R.layout.recy_batch_grouping_preview_5, mPresenter.getReadGroupExcelSuccess(), (EasyAdapter.IEasyAdapter<Pair<Integer, ESONObject>>) (holder1, data, position) -> {
                            holder1.setText(R.id.tv_index,String.valueOf(data.first+1));
                            holder1.setText(R.id.tv_group_id,data.second.getJSONValue(mPresenter.sGroupName,""));
                            holder1.setText(R.id.tv_name,data.second.getJSONValue(mPresenter.sNameFieldName,""));
                            holder1.setText(R.id.tv_id_card_no,data.second.getJSONValue(mPresenter.sIdCardNoName,""));
                            holder1.setOnClickListener(R.id.tv_detail,v->showDetailExcelDialog(data.second));
                        }));
                        rv.invalidate();

                        holder.setOnClickListener(R.id.tv_re_pick,v -> {
                            unlockView();
                            holder.dismissDialog();
                        });

                        holder.setOnClickListener(R.id.tv_start,v->{
                            holder.dismissDialog();
                            startSaving();
                        });
                    }
                })
                .setForegroundResource(R.drawable.shape_dialog_corner_with_tl_tr)
                .setDialogParams(p.x,p.y- ((int)DensityUtils.dp2px(50f)), Gravity.BOTTOM)
                .setAllowDismissWhenTouchOutside(false)
                .setAllowDismissWhenBackPressed(false)
                .showDialog();
    }

    private void startSaving(){
        LoadingDialog.showDialog("Save Excel",this);

        svGroupSaveOk  .tvValue.setText("无记录");
        svGroupSaveErr .tvValue.setText("无记录");

        pbProcess.setProgress(0);
        svProgress.tvValue.setText("开始保存分组记录！");

        mPresenter.autoSave();
    }

    @Override
    public void onSaving(int progress) {
        pbProcess.setProgress(progress);
    }

    @Override
    public void onSavingGroupSuccessCount(int count) {
        if(mPresenter.isEndAutoSave()){
            svGroupSaveOk.tvValue.setText(String.format("共%d条，点击查看。",mPresenter.getSaveGroupSuccess().size()));
        }
        else{
            svGroupSaveOk.tvValue.setText(String.format("共%d条，保存完毕后查看。",count));
        }
    }

    @Override
    public void onSavingGroupFailureCount(int count) {
        if(mPresenter.isEndAutoSave()){
            svGroupSaveErr.tvValue.setText(String.format("共%d条，点击查看。",mPresenter.getSaveGroupFailure().size()));
        }
        else{
            svGroupSaveErr.tvValue.setText(String.format("共%d条，保存完毕后查看。",count));
        }
    }

    @Override
    public void onSaveSuccess() {
        LoadingDialog.dismissDialog("Save Excel");


        svGroupSaveOk  .tvValue.setText(svGroupSaveOk  .tvValue.getText().toString().replaceAll("保存完毕后查看","点击查看"));
        svGroupSaveErr .tvValue.setText(svGroupSaveErr .tvValue.getText().toString().replaceAll("保存完毕后查看","点击查看"));

        svGroupSaveOk  .tvValue.setEnabled(true);
        svGroupSaveErr .tvValue.setEnabled(true);

        if(mPresenter.getProgress() == mPresenter.PROGRESS_SUCCESS && !mPresenter.getSaveGroupFailure().isEmpty()){
            svProgress.tvValue.setText("Excel导入部分成功！");
            showSaveGroupFailurePreviewDialog();
            return;
        }

        pbProcess.setProgress(100);
        svProgress.tvValue.setText("Excel导入成功！");
        setResult(RESULT_OK);
        mPresenter.markTransactionSuccess();
        DialogManager.showAlertDialogWithConfirm(this,"数据还原成功！",null);
    }

    @Override
    public void onSaveFailure(String err) {
        ToastUtils.show(err);
    }

    //序号 组号 身份号 姓名 操作 操作
    private void showPreviewGroupDialog(List<Pair<Integer,ESONObject>> lstData, String errTag, String title){
        if(lstData == null || lstData.isEmpty()){
            ToastUtils.show(errTag);
            return;
        }
        Point p = DensityUtils.getScreenSize();
        new EasyDialog(R.layout.dialog_batch_grouping_preview_6,this)
                .setOnBindDialogListener(new EasyDialog.OnBindDialogListener() {
                    @Override
                    public void onBindDialog(EasyDialogHolder holder) {
                        holder.setOnClickListener(R.id.iv_close,v -> holder.dismissDialog());
                        holder.setText(R.id.tv_title,title);
                        RecyclerView rv = holder.getView(R.id.rv_preview);
                        rv.setHasFixedSize(true);
                        rv.setLayoutManager(new LinearLayoutManager(NABatchGroupingActivity.this));
                        rv.setAdapter(new EasyAdapter(NABatchGroupingActivity.this, R.layout.recy_batch_grouping_preview_6, mPresenter.getReadGroupExcelSuccess(), (EasyAdapter.IEasyAdapter<Pair<Integer, ESONObject>>) (holder1, data, position) -> {
                            holder1.setText(R.id.tv_index,String.valueOf(data.first+1));
                            holder1.setText(R.id.tv_group_id,data.second.getJSONValue(mPresenter.sGroupName,""));
                            holder1.setText(R.id.tv_name,data.second.getJSONValue(mPresenter.sNameFieldName,""));
                            holder1.setText(R.id.tv_id_card_no,data.second.getJSONValue(mPresenter.sIdCardNoName,""));
                            holder1.setOnClickListener(R.id.tv_detail,v->showDetailExcelDialog(data.second));
                            holder1.setOnClickListener(R.id.tv_re_upload,v -> mPresenter.saveGroup(data.first, data.second, new ISetterListener() {
                                @Override
                                public void onSuccess() {
                                    ToastUtils.show("保存成功！");
                                    lstData.remove(position);
                                    rv.getAdapter().notifyDataSetChanged();
                                    if(lstData.isEmpty()){
                                        svGroupSaveErr .tvValue.setText("无记录");
                                        holder.dismissDialog();
                                    }
                                }

                                @Override
                                public void onFailure(String err) {
                                    ToastUtils.show("保存失败："+err);
                                }
                            }));
                        }));
                        rv.invalidate();
                    }
                })
                .setForegroundResource(R.drawable.shape_dialog_corner_with_tl_tr)
                .setDialogParams(p.x,p.y- ((int)DensityUtils.dp2px(80f)), Gravity.BOTTOM)
                .setAllowDismissWhenTouchOutside(false)
                .showDialog();
    }

    //序号 组号 身份号 姓名 操作 操作
    private void showSaveGroupFailurePreviewDialog() {
        showPreviewGroupDialog(mPresenter.getSaveGroupFailure(),"没有任何保存失败记录！","查看保存失败记录");
    }
    //序号 组号 身份号 姓名 操作
    private void showSaveGroupSuccessPreviewDialog() {
        showPreviewExcelDialog(mPresenter.getSaveGroupSuccess(),"没有任何保存成功记录！","查看保存成功记录");
    }

    @Override
    public void onProgress(int progress) {
        pbProcess.setProgress(progress);
    }

    @Override
    public void onProcess(String msg) {
        svProgress.tvValue.setText(msg);
    }

}
