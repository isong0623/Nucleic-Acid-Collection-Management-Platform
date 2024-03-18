package com.dreaming.hscj.activity.community.recovery;

import android.Manifest;
import android.content.Intent;
import android.graphics.Point;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
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

import com.dreaming.hscj.App;
import com.dreaming.hscj.Constants;
import com.dreaming.hscj.R;
import com.dreaming.hscj.activity.community.manage.CommunityMemberDetailActivity;
import com.dreaming.hscj.activity.nucleic_acid.grouping.NAGroupInDetailActivity;
import com.dreaming.hscj.base.contract.BaseMVPActivity;
import com.dreaming.hscj.core.EasyAdapter.EasyAdapter;
import com.dreaming.hscj.core.ThreadPoolProvider;
import com.dreaming.hscj.dialog.DialogManager;
import com.dreaming.hscj.dialog.loading.LoadingDialog;
import com.dreaming.hscj.template.DataParser;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.template.api.wrapper.ApiParam;
import com.dreaming.hscj.template.database.wrapper.DatabaseConfig;
import com.dreaming.hscj.template.database.wrapper.ISetterListener;
import com.dreaming.hscj.utils.DensityUtils;
import com.dreaming.hscj.utils.EasyPermission;
import com.dreaming.hscj.utils.JsonUtils;
import com.dreaming.hscj.utils.ToastUtils;
import com.dreaming.hscj.widget.InputView;
import com.dreaming.hscj.widget.ShownView;
import com.leon.lfilepickerlibrary.LFilePicker;
import com.leon.lfilepickerlibrary.utils.Constant;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import priv.songxusheng.easydialog.EasyDialog;
import priv.songxusheng.easydialog.EasyDialogHolder;
import priv.songxusheng.easyjson.ESONObject;

public class CommunityRecoveryActivity extends BaseMVPActivity<CommunityRecoveryPresenter> implements ICommunityRecoveryContract.View{
    //region 常规生命周期
    @Override
    public int getContentViewResId() {
        return R.layout.activity_community_recovery;
    }

    @Override
    protected void onBindView(View vContent) {
        ButterKnife.bind(this,vContent);
    }

    //还原模式
    @BindView(R.id.rb_comb)
    RadioButton rbComb;
    @BindView(R.id.rb_only_rec)
    RadioButton rbOnlyRecovery;

    CompoundButton.OnCheckedChangeListener onModeChanged = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(!isChecked) return;
            switch (buttonView.getId()){
                case R.id.rb_comb:
                    cvPartPriority.setVisibility(View.VISIBLE);
                    mPresenter.setMode(0);
                    break;
                case R.id.rb_only_rec:
                    cvPartPriority.setVisibility(View.GONE);
                    mPresenter.setMode(1);
                    break;
            }
        }
    };

    //数据优先级
    @BindView(R.id.part_priority)
    CardView cvPartPriority;
    @BindView(R.id.rv_recovery_first)
    RadioButton rbRecoveryFirst;
    @BindView(R.id.rb_local_fist)
    RadioButton rbLocalFirst;
    @BindView(R.id.rb_user_determine)
    RadioButton rbUserDetermine;

    CompoundButton.OnCheckedChangeListener onPriorityChanged = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(!isChecked) return;
            switch (buttonView.getId()){
                case R.id.rv_recovery_first:
                    mPresenter.setPriority(0);
                    break;
                case R.id.rb_local_fist:
                    mPresenter.setPriority(1);
                    break;
                case R.id.rb_user_determine:
                    mPresenter.setPriority(2);
                    break;
            }
        }
    };

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

    @BindView(R.id.pb_process)
    ProgressBar pbProcess;

    @BindView(R.id.sv_selected_file)
    ShownView svSelectedFile;
    @BindView(R.id.sv_progress)
    ShownView svProgress;
    @BindView(R.id.sv_member_read_local)
    ShownView svMemberReadLocal;
    @BindView(R.id.sv_member_read_recovery)
    ShownView svMemberReadRecovery;
    @BindView(R.id.sv_member_read_err)
    ShownView svMemberReadErr;
    @BindView(R.id.sv_group_read_local)
    ShownView svGroupReadLocal;
    @BindView(R.id.sv_group_read_recovery)
    ShownView svGroupReadRecovery;
    @BindView(R.id.sv_group_read_err)
    ShownView svGroupReadErr;
    @BindView(R.id.sv_save_member_ok)
    ShownView svMemberSaveOk;
    @BindView(R.id.sv_save_member_err)
    ShownView svMemberSaveErr;
    @BindView(R.id.sv_save_group_ok)
    ShownView svGroupSaveOk;
    @BindView(R.id.sv_save_group_err)
    ShownView svGroupSaveErr;

    @BindView(R.id.tv_start)
    TextView tvSelectFile;

    @Override
    public void initView() {
        setCenterText("社区数据库还原");

        DialogManager.showAlertDialogWithConfirm(this,"警告","还原有风险，为避免极少概率的还原失败，请在数据还原前进行备份，并关注后续版本。",null);

        ESONObject eConfig = Constants.Config.getRecoveryDbConfig();
        int mode = eConfig.getJSONValue("mode",0);
        mPresenter.setMode(mode);
        switch (mode){
            case 0:
                rbComb.setChecked(true);
                break;
            case 1:
                rbOnlyRecovery.setChecked(true);
                cvPartPriority.setVisibility(View.GONE);
                break;
        }
        int priority = eConfig.getJSONValue("priority",0);
        mPresenter.setPriority(priority);
        switch (priority){
            case 0:
                rbRecoveryFirst.setChecked(true);
                break;
            case 1:
                rbLocalFirst.setChecked(true);
                break;
            case 2:
                rbUserDetermine.setChecked(true);
                break;
        }
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

        rbComb         .setOnCheckedChangeListener(onModeChanged);
        rbOnlyRecovery .setOnCheckedChangeListener(onModeChanged);
        rbLocalFirst   .setOnCheckedChangeListener(onPriorityChanged);
        rbRecoveryFirst.setOnCheckedChangeListener(onPriorityChanged);
        rbUserDetermine.setOnCheckedChangeListener(onPriorityChanged);
        rbRootPath     .setOnCheckedChangeListener(onPathChanged);
        rbQQ           .setOnCheckedChangeListener(onPathChanged);
        rbWX           .setOnCheckedChangeListener(onPathChanged);


        svMemberReadLocal    .tvValue.setOnClickListener(v -> showReadMemberLocalDialog());
        svMemberReadRecovery .tvValue.setOnClickListener(v -> showReadMemberRecoveryDialog());
        svMemberReadErr      .tvValue.setOnClickListener(v -> showReadMemberDuplicatedDialog());
        svGroupReadLocal     .tvValue.setOnClickListener(v -> showReadGroupLocalDialog());
        svGroupReadRecovery  .tvValue.setOnClickListener(v -> showReadGroupRecoveryDialog());
        svGroupReadErr       .tvValue.setOnClickListener(v -> showReadGroupDuplicatedDialog());

        svMemberSaveOk       .tvValue.setOnClickListener(v -> showSaveMemberSuccessPreviewDialog());
        svMemberSaveErr      .tvValue.setOnClickListener(v -> showSaveMemberFailurePreviewDialog());
        svGroupSaveOk        .tvValue.setOnClickListener(v -> showSaveGroupSuccessPreviewDialog());
        svGroupSaveErr       .tvValue.setOnClickListener(v -> showSaveGroupFailurePreviewDialog());

        unlockView();
    }

    void lockView(){
        rbComb         .setEnabled(false);
        rbOnlyRecovery .setEnabled(false);
        rbLocalFirst   .setEnabled(false);
        rbRecoveryFirst.setEnabled(false);
        rbUserDetermine.setEnabled(false);
        rbRootPath     .setEnabled(false);
        rbQQ           .setEnabled(false);
        rbWX           .setEnabled(false);

        svMemberReadLocal    .tvValue.setEnabled(false);
        svMemberReadRecovery .tvValue.setEnabled(false);
        svMemberReadErr      .tvValue.setEnabled(false);
        svGroupReadLocal     .tvValue.setEnabled(false);
        svGroupReadRecovery  .tvValue.setEnabled(false);
        svGroupReadErr       .tvValue.setEnabled(false);

        svGroupReadErr       .tvValue.setEnabled(false);
        svMemberSaveOk       .tvValue.setEnabled(false);
        svMemberSaveErr      .tvValue.setEnabled(false);
        svGroupSaveOk        .tvValue.setEnabled(false);
        svGroupSaveErr       .tvValue.setEnabled(false);

        tvSelectFile.setVisibility(View.INVISIBLE);
    }

    void unlockView(){
        rbComb         .setEnabled(true);
        rbOnlyRecovery .setEnabled(true);
        rbLocalFirst   .setEnabled(true);
        rbRecoveryFirst.setEnabled(true);
        rbUserDetermine.setEnabled(true);
        rbRootPath     .setEnabled(true);
        rbQQ           .setEnabled(true);
        rbWX           .setEnabled(true);

        svMemberReadLocal    .tvValue.setEnabled(false);
        svMemberReadRecovery .tvValue.setEnabled(false);
        svMemberReadErr      .tvValue.setEnabled(false);
        svGroupReadLocal     .tvValue.setEnabled(false);
        svGroupReadRecovery  .tvValue.setEnabled(false);
        svGroupReadErr       .tvValue.setEnabled(false);

        svGroupReadErr .tvValue.setEnabled(false);
        svMemberSaveOk .tvValue.setEnabled(false);
        svMemberSaveErr.tvValue.setEnabled(false);
        svGroupSaveOk  .tvValue.setEnabled(false);
        svGroupSaveErr .tvValue.setEnabled(false);

        pbProcess.setProgress(0);
        svSelectedFile .tvValue.setText("未选择");

        svMemberReadLocal    .tvValue.setText("等待文件选择");
        svMemberReadRecovery .tvValue.setText("等待文件选择");
        svMemberReadErr      .tvValue.setText("等待文件选择");
        svGroupReadLocal     .tvValue.setText("等待文件选择");
        svGroupReadRecovery  .tvValue.setText("等待文件选择");
        svGroupReadErr       .tvValue.setText("等待文件选择");

        svMemberSaveOk .tvValue.setText("等待文件选择");
        svMemberSaveErr.tvValue.setText("等待文件选择");
        svGroupSaveOk  .tvValue.setText("等待文件选择");
        svGroupSaveErr .tvValue.setText("等待文件选择");

        tvSelectFile.setVisibility(View.VISIBLE);
    }

    void showExitAlertToContinue(View.OnClickListener onConfirm, View.OnClickListener onCancel){
        DialogManager.showAlertDialog(this,"提示","当前还原未完成，确认要结束吗？",onCancel,onConfirm);
    }

    @Override
    public void onBackPressed() {
        if(mPresenter.getProgress() == mPresenter.PROGRESS_PROCESSING){
            if(!mPresenter.getReadMemberDuplicated().isEmpty() || !mPresenter.getReadGroupDuplicated().isEmpty()) {
                showExitAlertToContinue(
                        v ->
//                                DialogManager.showAlertDialog(this,"提示","要保存已还原记录吗？",
//                                        v1 -> {
//                                            mPresenter.markTransactionFailure();
//                                            super.onBackPressed();
//                                        },
//                                        v2 -> {
//                                            mPresenter.markTransactionSuccess();
//                                            super.onBackPressed();
//                                        }
//                                ),
                                super.onBackPressed(),
                        v -> {
                            if(!mPresenter.getReadMemberDuplicated().isEmpty()){
                                showReadMemberDuplicatedDialog();
                                return;
                            }
                            if(!mPresenter.getReadGroupDuplicated().isEmpty()){
                                showReadGroupDuplicatedDialog();
                            }
                        }
                );
                return;
            }
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        mPresenter.release();
        super.onDestroy();
    }

    //endregion

    //region 1文件处理
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
                onStartExtract(f.getAbsolutePath());
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



                new LFilePicker()
                        .withActivity(CommunityRecoveryActivity.this)
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
                        .withFileFilter(new String[]{"mbu"})
                        .start();
            }

            @Override
            public void onPermissionDenied() {
                ToastUtils.show("没有授予读取外置存储器权限，无法读取文件！");
            }
        });
    }

    @Override
    public void onProcess(String msg) {
        svProgress.tvValue.setText(msg);
    }

    private void onStartExtract(String path){
        lockView();
        svSelectedFile.tvValue.setText(path);
        mPresenter.start(path);
    }

    @Override
    public void onExtracting(int progress) {
        pbProcess.setProgress(progress);
    }

    @Override
    public void onExtractSuccess() {
        pbProcess.setProgress(100);
    }

    @Override
    public void onExtractFailure(String err) {
        DialogManager.showAlertDialogWithConfirm(this,"通知","还原数据解析失败："+err,null);
        unlockView();
    }

    @Override
    public void onVerifying(int progress) {
        pbProcess.setProgress(progress);
    }

    @Override
    public void onVerifySuccess() {
        pbProcess.setProgress(100);
        showSetTargetDatabaseDialog();
    }

    @Override
    public void onVerifyFailure(String err) {
        DialogManager.showAlertDialogWithConfirm(this,"通知","还原数据验证失败："+err,null);
        unlockView();
    }

    private void showPreviewDialog(List lstData, boolean bIsRead, boolean bIsMember, boolean bIsDuplicated){
        if(lstData == null || lstData.isEmpty()){
            ToastUtils.show("没有相关数据！");
            return;
        }

        Point p = DensityUtils.getScreenSize();
        new EasyDialog(R.layout.dialog_view_recovery_duplicated_preview,this)
                .setOnBindDialogListener(new EasyDialog.OnBindDialogListener() {

                    EasyAdapter adapter;

                    @Override
                    public void onBindDialog(EasyDialogHolder holder) {
                        CardView cvTitleMember = holder.getView(R.id.part_recy_title_member);
                        CardView cvTitleGroup  = holder.getView(R.id.part_recy_title_group);

                        holder.setOnClickListener(R.id.iv_close,v -> holder.dismissDialog());

                        RecyclerView rv = holder.getView(R.id.rv_recovery);
                        rv.setHasFixedSize(true);
                        rv.setLayoutManager(new LinearLayoutManager(CommunityRecoveryActivity.this));

                        cvTitleMember.setVisibility(bIsMember?View.VISIBLE:View.INVISIBLE);
                        cvTitleGroup .setVisibility(bIsMember?View.INVISIBLE:View.VISIBLE);

                        int layoutId = View.NO_ID;
                        String title = "";
                        if(bIsDuplicated){
                            if(bIsMember){
                                title = "社区成员冲突数据处理";
                                layoutId = R.layout.recy_recovery_duplicated_member;
                            }
                            else{
                                title = "核酸分组冲突数据处理";
                                layoutId = R.layout.recy_recovery_duplicated_group;
                            }
                        }
                        else{
                            if(bIsMember){
                                title = "查看社区成员数据";
                                layoutId = R.layout.recy_detail_recovery_member;
                            }
                            else{
                                title = "查看核酸分组数据";
                                layoutId = R.layout.recy_detail_recovery_group;
                            }
                        }

                        holder.setText(R.id.tv_title,title);

                        adapter = new EasyAdapter(CommunityRecoveryActivity.this, layoutId, lstData, (EasyAdapter.IEasyAdapter) (holder1, data, position) -> {
                            ESONObject e = bIsDuplicated ? ((Pair<ESONObject,ESONObject>)(data)).first : ((ESONObject)data);
                            String idCardNo = e.getJSONValue(mPresenter.sIdCardNo,"");
                            String name     = e.getJSONValue(mPresenter.sName    ,"");
                            String group    = e.getJSONValue(mPresenter.sGroupId ,"");

                            if(bIsDuplicated){
                                if(!bIsMember){
                                    holder1.setText(R.id.tv_group_id,group);
                                }
                                holder1.setOnClickListener(R.id.tv_detail,v -> {
                                    ThreadPoolProvider.getFixedThreadPool().execute(()->{
                                        App.Post(()-> LoadingDialog.showDialog("Wait For Show Dialog",CommunityRecoveryActivity.this));
                                        ESONObject eLocal    = DataParser.parseDatabaseToShown(bIsMember? DatabaseConfig.TYPE_USER_OVERALL : DatabaseConfig.TYPE_NA_GROUPING,((Pair<ESONObject,ESONObject>)(data)).first );
                                        ESONObject eRecovery = DataParser.parseDatabaseToShown(bIsMember? DatabaseConfig.TYPE_USER_OVERALL : DatabaseConfig.TYPE_NA_GROUPING,((Pair<ESONObject,ESONObject>)(data)).second);
                                        List<ApiParam> lst = bIsMember ? mPresenter.getDb2Local().getConfig().getFields() : mPresenter.getDb3Local().getConfig().getFields();
                                        List<Pair<ESONObject,ESONObject>> lstFields = new ArrayList<>();
                                        for(int i=0,ni=lst.size();i<ni;++i){
                                            ApiParam p = lst.get(i);

                                            ESONObject eLocalShown    = new ESONObject();
                                            ESONObject eRecoveryShown = new ESONObject();

                                            String key           = p.getName();
                                            String description   = p.getDescription();
                                            String localValue    = eLocal   .getJSONValue(p.getName(),"");
                                            String recoveryValue = eRecovery.getJSONValue(p.getName(),"");

                                            eLocalShown   .putValue("key",key);
                                            eRecoveryShown.putValue("key",key);

                                            eLocalShown   .putValue("name",description);
                                            eRecoveryShown.putValue("name",description);

                                            eLocalShown   .putValue("value",localValue   );
                                            eRecoveryShown.putValue("value",recoveryValue);

                                            lstFields.add(new Pair<>(eLocalShown,eRecoveryShown));
                                        }
                                        App.Post(()->LoadingDialog.dismissDialog("Wait For Show Dialog"));
                                        App.Post(()->showDetailDialog(lstFields, bIsMember, new ISetterListener() {
                                            @Override
                                            public void onSuccess() {
                                                ToastUtils.show("数据保存成功！");
                                                lstData.remove(position);
                                                if(lstData.isEmpty()){
                                                    if(mPresenter.getReadMemberDuplicated().isEmpty() && mPresenter.getReadGroupDuplicated().isEmpty()) {
                                                        holder.dismissDialog();
                                                        onSaveSuccess();
                                                    }
                                                }
                                                adapter.notifyDataSetChanged();
                                                if(bIsMember){
                                                    if(lstData.isEmpty()){
                                                        svMemberReadErr.tvValue.setText("无记录");
                                                    }
                                                    else{
                                                        svMemberReadErr.tvValue.setText(String.format("共%d条，点击查看。",lstData.size()));
                                                    }
                                                }
                                                else{
                                                    if(lstData.isEmpty()){
                                                        svGroupReadErr.tvValue.setText("无记录");
                                                    }
                                                    else{
                                                        svGroupReadErr.tvValue.setText(String.format("共%d条，点击查看。",lstData.size()));
                                                    }
                                                }
                                            }

                                            @Override
                                            public void onFailure(String err) {
                                                ToastUtils.show("数据保存失败："+err);
                                            }
                                        }));
                                    });
                                });
                            }
                            else{
                                if(bIsMember){
                                    holder1.setOnClickListener(R.id.tv_detail,v -> CommunityMemberDetailActivity.shownMember(CommunityRecoveryActivity.this,e));
                                }
                                else{
                                    holder1.setText(R.id.tv_group_id,group);
                                    holder1.setOnClickListener(R.id.tv_detail,v -> NAGroupInDetailActivity.shownMember(CommunityRecoveryActivity.this,e));
                                }
                            }

                            holder1.setText(R.id.tv_index,String.valueOf(position+1));
                            holder1.setText(R.id.tv_name,name);
                            holder1.setText(R.id.tv_id_card_no,idCardNo);
                        });

                        rv.setAdapter(adapter);
                    }
                })
                .setForegroundResource(R.drawable.shape_dialog_corner_with_tl_tr)
                .setDialogParams(p.x,p.y- ((int)DensityUtils.dp2px(80f)), Gravity.BOTTOM)
                .setAllowDismissWhenTouchOutside(false)
                .showDialog();
    }

    private void showDetailDialog(List<Pair<ESONObject,ESONObject>> lstData, boolean bIsMember, ISetterListener listener){
        Point p = DensityUtils.getScreenSize();
        new EasyDialog(R.layout.dialog_recovery_duplacated_process,this)
                .setOnBindDialogListener(new EasyDialog.OnBindDialogListener() {

                    ESONObject eConfig = bIsMember ? Constants.Config.getRecoverySaveMemberConfig() : Constants.Config.getRecoverySaveGroupConfig();
                    ESONObject eWaitForSave = new ESONObject();

                    @Override
                    public void onBindDialog(EasyDialogHolder holder) {
                        RecyclerView rv = holder.getView(R.id.rv_process);
                        rv.setHasFixedSize(true);
                        rv.setLayoutManager(new LinearLayoutManager(CommunityRecoveryActivity.this));

                        holder.setOnClickListener(R.id.iv_close,v -> holder.dismissDialog());

                        EasyAdapter adapter = new EasyAdapter(CommunityRecoveryActivity.this, R.layout.recy_recovery_determine, lstData, (EasyAdapter.IEasyAdapter<Pair<ESONObject,ESONObject>>) (holder1, data, position) -> {
                            String name = data.first.getJSONValue("name","");
                            String key  = data.first.getJSONValue("key","");
                            String local= data.first.getJSONValue("value","");
                            String recovery = data.second.getJSONValue("value","");

                            holder1.setText(R.id.tv_name,name);
                            holder1.setText(R.id.tv_local   ,local);
                            holder1.setText(R.id.tv_recovery,recovery);

                            EditText etValue = holder1.getView(R.id.ev_value);
                            if(etValue.getTag() == null){
                                etValue.setTag("");
                                etValue.addTextChangedListener(new TextWatcher() {
                                    @Override
                                    public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                                    @Override
                                    public void onTextChanged(CharSequence s, int start, int before, int count) { }
                                    @Override
                                    public void afterTextChanged(Editable s) {
                                        eWaitForSave.putValue(key,s.toString());
                                    }
                                });
                            }

                            CheckBox cbLocal    = holder1.getView(R.id.cb_local);
                            CheckBox cbRecovery = holder1.getView(R.id.cb_recovery);
                            CheckBox cbDetermine= holder1.getView(R.id.cb_input);

                            int check = eConfig.getJSONValue(key,0);
                            (check == 0 ? cbLocal :(check == 1 ? cbRecovery : cbDetermine)).setChecked(true);
                            eWaitForSave.putValue(key,check == 0 ? local : (check == 1 ? recovery : ""));

                            CompoundButton.OnCheckedChangeListener onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
                                @Override
                                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                    if(!isChecked) return;
                                    switch (buttonView.getId()){
                                        case R.id.cb_local:
                                            cbRecovery.setChecked(false);
                                            cbDetermine.setChecked(false);
                                            eConfig.putValue(key,0);
                                            eWaitForSave.putValue(key,local);
                                            break;
                                        case R.id.tv_recovery:
                                            cbLocal.setChecked(false);
                                            cbDetermine.setChecked(false);
                                            eConfig.putValue(key,1);
                                            eWaitForSave.putValue(key,recovery);
                                            break;
                                        case R.id.ev_value:
                                            cbLocal.setChecked(false);
                                            cbRecovery.setChecked(false);
                                            eConfig.putValue(key,2);
                                            eWaitForSave.putValue(key,etValue.getText().toString());
                                            break;
                                    }
                                }
                            };

                            cbLocal    .setOnCheckedChangeListener(onCheckedChangeListener);
                            cbRecovery .setOnCheckedChangeListener(onCheckedChangeListener);
                            cbDetermine.setOnCheckedChangeListener(onCheckedChangeListener);
                        });

                        rv.setAdapter(adapter);

                        holder.setOnClickListener(R.id.tv_start,v -> {
                            if(bIsMember){
                                Constants.Config.setRecoverySaveMemberConfig(eConfig);
                            }
                            else{
                                Constants.Config.setRecoverySaveGroupConfig(eConfig);
                            }

                            showSavePreviewDialog(lstData, eWaitForSave, bIsMember, new ISetterListener() {
                                @Override
                                public void onSuccess() {
                                    listener.onSuccess();
                                    holder.dismissDialog();
                                }

                                @Override
                                public void onFailure(String err) {
                                    listener.onFailure(err);
                                }
                            });
                        });
                    }
                })
                .setForegroundResource(R.drawable.shape_dialog_corner_with_tl_tr)
                .setDialogParams(p.x,p.y/2, Gravity.BOTTOM)
                .setAllowDismissWhenTouchOutside(false)
                .showDialog();
    }

    private void showSavePreviewDialog(List<Pair<ESONObject,ESONObject>> lstData, ESONObject eData, boolean bIsMember, ISetterListener listener){
        Point p = DensityUtils.getScreenSize();
        new EasyDialog(R.layout.dialog_recovery_saving_preview,this)
                .setOnBindDialogListener(new EasyDialog.OnBindDialogListener() {
                    @Override
                    public void onBindDialog(EasyDialogHolder holder) {
                        holder.setOnClickListener(R.id.iv_close,v -> holder.dismissDialog());

                        RecyclerView rv = holder.getView(R.id.rv_preview);
                        rv.setHasFixedSize(true);
                        rv.setLayoutManager(new LinearLayoutManager(CommunityRecoveryActivity.this));
                        EasyAdapter adapter = new EasyAdapter(CommunityRecoveryActivity.this, R.layout.view_shown, lstData, (EasyAdapter.IEasyAdapter<Pair<ESONObject,ESONObject>>) (holder1, data, position) -> {
                            String name = data.first.getJSONValue("name","");
                            String key  = data.first.getJSONValue("key","");
                            String value= eData.getJSONValue(key,"");
                            holder1.setText(R.id.tv_name,name);
                            holder1.setText(R.id.tv_value,value);
                        });
                        rv.setAdapter(adapter);

                        holder.setOnClickListener(R.id.tv_start,v -> {
                            if(bIsMember){
                                ESONObject data = DataParser.parseShownToDatabase(DatabaseConfig.TYPE_USER_OVERALL,eData);
                                mPresenter.saveMember(data, new ISetterListener() {
                                    @Override
                                    public void onSuccess() {
                                        holder.dismissDialog();
                                        listener.onSuccess();
                                    }

                                    @Override
                                    public void onFailure(String err) {
                                        listener.onFailure(err);
                                    }
                                });
                            }
                            else{
                                ESONObject data = DataParser.parseShownToDatabase(DatabaseConfig.TYPE_NA_GROUPING,eData);
                                mPresenter.saveGroup(data,new ISetterListener() {
                                    @Override
                                    public void onSuccess() {
                                        holder.dismissDialog();
                                        listener.onSuccess();
                                    }

                                    @Override
                                    public void onFailure(String err) {
                                        listener.onFailure(err);
                                    }
                                });
                            }
                        });
                    }
                })
                .setForegroundResource(R.drawable.shape_dialog_corner_with_tl_tr)
                .setDialogParams(p.x,p.y- ((int)DensityUtils.dp2px(140f)), Gravity.BOTTOM)
                .setAllowDismissWhenTouchOutside(false)
                .showDialog();
    }
    //endregion

    //region 2数据库读取
    private void showSetTargetDatabaseDialog(){
        Point p = DensityUtils.getScreenSize();
        new EasyDialog(R.layout.dialog_recovery_set_target_db,this)
                .setOnBindDialogListener(new EasyDialog.OnBindDialogListener() {

                    InputView ivTargetDbTownName;
                    InputView ivTargetDbVillageName;

                    void showSelectDatabaseDialog(View.OnClickListener onDismiss){
                        new EasyDialog(R.layout.dialog_select_datebase,CommunityRecoveryActivity.this)
                                .setOnBindDialogListener(new EasyDialog.OnBindDialogListener() {
                                    EasyAdapter adapter;
                                    int selectedId = mPresenter.getSelectedDatabaseId();
                                    String selectedTownName;
                                    String selectedVillageName;
                                    @Override
                                    public void onBindDialog(EasyDialogHolder dialogHolder) {
                                        RecyclerView rvDb = dialogHolder.getView(R.id.rv_select_db);
                                        rvDb.setLayoutManager(new LinearLayoutManager(CommunityRecoveryActivity.this){
                                            @Override
                                            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                                                try {
                                                    super.onLayoutChildren( recycler, state );
                                                } catch (IndexOutOfBoundsException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        });
                                        List<ESONObject> lstData = JsonUtils.parseToList(Constants.DBConfig.getAllDatabase());
                                        rvDb.setAdapter(adapter = new EasyAdapter(CommunityRecoveryActivity.this, R.layout.recy_database_choose, lstData, (EasyAdapter.IEasyAdapter<ESONObject>) (holder, data, position) -> {
                                            holder.setText(R.id.tv_id_value,String.valueOf(data.getJSONValue("id",-1)));
                                            holder.setText(R.id.tv_db_value, Template.getCurrentTemplate().getDatabaseSetting().getRegionName()+"/"+data.getJSONValue("townName","")+"/"+data.getJSONValue("villageName",""));
                                            CheckBox cb = holder.getView(R.id.cb_choose);
                                            if(selectedId == data.getJSONValue("id",-2)){
                                                cb.setChecked(true);
                                                holder.getRootView().setBackground(getResources().getDrawable(R.drawable.shape_recy_choose_database_2));
                                            }
                                            else{
                                                holder.getRootView().setBackground(getResources().getDrawable(R.drawable.shape_recy_choose_database_1));
                                                cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                                    @Override
                                                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                                        if(isChecked){
                                                            selectedTownName = data.getJSONValue("townName","");
                                                            selectedVillageName = data.getJSONValue("villageName","");
                                                            selectedId = data.getJSONValue("id",-1);
                                                        }
                                                    }
                                                });
                                            }

                                        },false));

                                        dialogHolder.setOnClickListener(R.id.tv_start,v->{
                                            if(selectedId != mPresenter.getSelectedDatabaseId()){
                                                mPresenter.setSelectedDatabaseId(selectedId);
                                                mPresenter.setSelectedTownName(selectedTownName);
                                                mPresenter.setSelectedVillageName(selectedVillageName);
                                            }
                                            dialogHolder.dismissDialog();
                                        });
                                    }
                                })
                                .setAllowDismissWhenTouchOutside(false)
                                .setAllowDismissWhenBackPressed(false)
                                .setDialogParams(p.x/3*2,p.y/2, Gravity.BOTTOM)
                                .setForegroundResource(R.drawable.shape_dialog_corner_with_tl_tr)
                                .showDialog();
                    }
                    RadioButton rbDefault;
                    RadioButton rbSelect;
                    RadioButton rbDiy;
                    CompoundButton.OnCheckedChangeListener onTargetChanged = new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            if(!isChecked) return;
                            switch (buttonView.getId()){
                                case R.id.rb_default:
                                    ivTargetDbVillageName.tvValue.setEnabled(false);
                                    ivTargetDbVillageName.tvValue.setText(mPresenter.getDefaultVillageName());
                                    ivTargetDbTownName.tvValue.setEnabled(false);
                                    ivTargetDbTownName.tvValue.setText(mPresenter.getDefaultTownName());
                                    mPresenter.setTarget(0);
                                    break;
                                case R.id.rb_select:
                                    if(mPresenter.getSelectedTownName().isEmpty() || mPresenter.getSelectedVillageName().isEmpty()){
                                        ESONObject e = Constants.DBConfig.getSelectedDatabase();
                                        mPresenter.setSelectedDatabaseId(e.getJSONValue("id",-1));
                                        mPresenter.setSelectedTownName(e.getJSONValue("townName",""));
                                        mPresenter.setSelectedVillageName(e.getJSONValue("villageName",""));
                                    }
                                    showSelectDatabaseDialog(v -> {
                                        if(mPresenter.getSelectedDatabaseId() == -1){
                                            ToastUtils.show("该模板并没有数据库！");
                                            rbDefault.setChecked(true);
                                            return;
                                        }
                                        ivTargetDbTownName.tvValue.setEnabled(false);
                                        ivTargetDbTownName.tvValue.setText(mPresenter.getSelectedTownName());
                                        ivTargetDbVillageName.tvValue.setEnabled(false);
                                        ivTargetDbVillageName.tvValue.setText(mPresenter.getSelectedVillageName());
                                    });
                                    ivTargetDbTownName.tvValue.setEnabled(false);
                                    ivTargetDbTownName.tvValue.setText(mPresenter.getSelectedTownName());
                                    ivTargetDbVillageName.tvValue.setEnabled(false);
                                    ivTargetDbVillageName.tvValue.setText(mPresenter.getSelectedVillageName());
                                    mPresenter.setTarget(1);
                                    break;
                                case R.id.rb_diy:
                                    ivTargetDbTownName.tvValue.setEnabled(true);
                                    ivTargetDbTownName.tvValue.setText(mPresenter.getDiyTownName());
                                    ivTargetDbVillageName.tvValue.setEnabled(true);
                                    ivTargetDbVillageName.tvValue.setText(mPresenter.getDiyVillageName());
                                    if(ivTargetDbTownName.tvValue.getText().toString().isEmpty()){
                                        showKeyboard(ivTargetDbTownName.tvValue);
                                    }
                                    else if(ivTargetDbVillageName.tvValue.getText().toString().isEmpty()){
                                        showKeyboard(ivTargetDbVillageName.tvValue);
                                    }
                                    mPresenter.setTarget(2);
                                    break;
                            }
                        }
                    };

                    @Override
                    public void onBindDialog(EasyDialogHolder dialogHolder) {
                        ivTargetDbTownName = dialogHolder.getView(R.id.iv_target_db_town_name);
                        ivTargetDbVillageName = dialogHolder.getView(R.id.iv_target_db_village_name);

                        rbDefault = dialogHolder.getView(R.id.rb_default);
                        rbSelect  = dialogHolder.getView(R.id.rb_select);
                        rbDiy     = dialogHolder.getView(R.id.rb_diy);

                        switch(mPresenter.getTarget()){
                            case 0:
                                rbDefault.setChecked(true);
                                ivTargetDbTownName.tvValue.setText(mPresenter.getDefaultTownName());
                                ivTargetDbVillageName.tvValue.setText(mPresenter.getDefaultVillageName());
                                ivTargetDbTownName.tvValue.setEnabled(false);
                                ivTargetDbVillageName.tvValue.setEnabled(false);
                                break;
                            case 1:
                                rbSelect.setChecked(true);
                                ivTargetDbTownName.tvValue.setText(mPresenter.getSelectedTownName());
                                ivTargetDbVillageName.tvValue.setText(mPresenter.getSelectedVillageName());
                                ivTargetDbTownName.tvValue.setEnabled(false);
                                ivTargetDbVillageName.tvValue.setEnabled(false);
                                break;
                            case 2:
                                rbDiy.setChecked(true);
                                ivTargetDbTownName.tvValue.setText(mPresenter.getDiyTownName());
                                ivTargetDbVillageName.tvValue.setText(mPresenter.getDiyVillageName());
                                ivTargetDbTownName.tvValue.setEnabled(true);
                                ivTargetDbVillageName.tvValue.setEnabled(true);
                                break;
                        }

                        rbDefault .setOnCheckedChangeListener(onTargetChanged);
                        rbSelect  .setOnCheckedChangeListener(onTargetChanged);
                        rbDiy     .setOnCheckedChangeListener(onTargetChanged);


                        dialogHolder.setOnClickListener(R.id.tv_re_pick,v -> {
                            dialogHolder.dismissDialog();
                            unlockView();
                        });

                        dialogHolder.setOnClickListener(R.id.tv_start,v -> {
                            switch (mPresenter.getTarget()){
                                case 0:
                                    break;
                                case 1:
                                    if(mPresenter.getSelectedVillageName().isEmpty() || mPresenter.getSelectedTownName().isEmpty() || mPresenter.getSelectedDatabaseId() == -1){
                                        ToastUtils.show("请选择数据库！");
                                        return;
                                    }
                                    break;
                                case 2:
                                    if(ivTargetDbTownName.tvValue.getText().toString().isEmpty()){
                                        showKeyboard(ivTargetDbTownName.tvValue);
                                        return;
                                    }
                                    if(ivTargetDbVillageName.tvValue.getText().toString().isEmpty()){
                                        showKeyboard(ivTargetDbVillageName.tvValue);
                                        return;
                                    }
                                    mPresenter.setDiyTownName(ivTargetDbTownName.tvValue.getText().toString());
                                    mPresenter.setDiyVillageName(ivTargetDbVillageName.tvValue.getText().toString());
                                    break;
                            }
                            dialogHolder.dismissDialog();
                            onStartRead();
                        });
                    }
                })
                .setAllowDismissWhenBackPressed(false)
                .setAllowDismissWhenTouchOutside(false)
                .setForegroundResource(R.drawable.shape_common_dialog)
                .setDialogHeight((int) DensityUtils.dp2px(300f))
                .showDialog();
    }

    private void onStartRead(){
        pbProcess.setProgress(0);

        svMemberReadLocal    .tvValue.setText("无记录");
        svMemberReadRecovery .tvValue.setText("无记录");
        svMemberReadErr      .tvValue.setText("无记录");
        svGroupReadLocal     .tvValue.setText("无记录");
        svGroupReadRecovery  .tvValue.setText("无记录");
        svGroupReadErr       .tvValue.setText("无记录");

        svMemberSaveOk .tvValue.setText("等待记录读取完毕！");
        svMemberSaveErr.tvValue.setText("等待记录读取完毕！");
        svGroupSaveOk  .tvValue.setText("等待记录读取完毕！");
        svGroupSaveErr .tvValue.setText("等待记录读取完毕！");

        mPresenter.read();
    }

    @Override
    public void onReading(int progress) {
        pbProcess.setProgress(progress);
    }

    @Override
    public void onReadingMemberLocalCount(int count) {
        svMemberReadLocal.tvValue.setText(String.format("共%d条，读取完毕后查看。",count));
    }

    @Override
    public void onReadingMemberRecoveryCount(int count) {
        svMemberReadRecovery.tvValue.setText(String.format("共%d条，读取完毕后查看。",count));
    }

    @Override
    public void onReadingMemberFailureCount(int count) {
        svMemberReadErr.tvValue.setText(String.format("共%d条，读取完毕后查看。",count));
    }

    @Override
    public void onReadingGroupLocalCount(int count) {
        svGroupReadLocal.tvValue.setText(String.format("共%d条，读取完毕后查看。",count));
    }

    @Override
    public void onReadingGroupRecoveryCount(int count) {
        svGroupReadRecovery.tvValue.setText(String.format("共%d条，读取完毕后查看。",count));
    }

    @Override
    public void onReadingGroupFailureCount(int count) {
        svGroupReadErr.tvValue.setText(String.format("共%d条，读取完毕后查看。",count));
    }

    @Override
    public void onReadSuccess() {
        pbProcess.setProgress(100);

        svMemberReadLocal    .tvValue.setEnabled(true);
        svMemberReadRecovery .tvValue.setEnabled(true);
        svMemberReadErr      .tvValue.setEnabled(true);
        svGroupReadLocal     .tvValue.setEnabled(true);
        svGroupReadRecovery  .tvValue.setEnabled(true);
        svGroupReadErr       .tvValue.setEnabled(true);

        svMemberReadLocal    .tvValue.setText(svMemberReadLocal    .tvValue.getText().toString().replaceAll("读取完毕后查看","点击查看"));
        svMemberReadRecovery .tvValue.setText(svMemberReadRecovery .tvValue.getText().toString().replaceAll("读取完毕后查看","点击查看"));
        svMemberReadErr      .tvValue.setText(svMemberReadErr      .tvValue.getText().toString().replaceAll("读取完毕后查看","点击查看"));
        svGroupReadLocal     .tvValue.setText(svGroupReadLocal     .tvValue.getText().toString().replaceAll("读取完毕后查看","点击查看"));
        svGroupReadRecovery  .tvValue.setText(svGroupReadRecovery  .tvValue.getText().toString().replaceAll("读取完毕后查看","点击查看"));
        svGroupReadErr       .tvValue.setText(svGroupReadErr       .tvValue.getText().toString().replaceAll("读取完毕后查看","点击查看"));

        svMemberSaveOk .tvValue.setText("无记录");
        svMemberSaveErr.tvValue.setText("无记录");
        svGroupSaveOk  .tvValue.setText("无记录");
        svGroupSaveErr .tvValue.setText("无记录");

        showReadDataPreviewDialog();
    }

    @Override
    public void onReadError(String err) {
        DialogManager.showAlertDialogWithConfirm(this,"通知","数据读取失败："+err,null);
        unlockView();
    }

    //读取完毕后展示数据
    private void showReadDataPreviewDialog(){
        if(mPresenter.getReadMemberRecovery().isEmpty()){
            DialogManager.showAlertDialogWithConfirm(this,"没有读取到任何记录！", null);
            return;
        }
        Point p = DensityUtils.getScreenSize();
        new EasyDialog(R.layout.dialog_view_recovery_and_continue,this)
                .setOnBindDialogListener(new EasyDialog.OnBindDialogListener() {

                    EasyAdapter adapterMember;
                    EasyAdapter adapterGroup;

                    @Override
                    public void onBindDialog(EasyDialogHolder holder) {
                        holder.setText(R.id.tv_path,String.format("路径：%s",svSelectedFile.tvValue.getText().toString()));

                        CardView cvTitleMember = holder.getView(R.id.part_recy_title_member);
                        CardView cvTitleGroup  = holder.getView(R.id.part_recy_title_group);

                        TextView tvTabMember = holder.getView(R.id.tv_member);
                        TextView tvTabGroup  = holder.getView(R.id.tv_group);


                        RecyclerView rv = holder.getView(R.id.rv_view_recovery);
                        rv.setHasFixedSize(true);
                        rv.setLayoutManager(new LinearLayoutManager(CommunityRecoveryActivity.this));
                        adapterMember = new EasyAdapter(CommunityRecoveryActivity.this, R.layout.recy_detail_recovery_member, mPresenter.getReadMemberRecovery(), (EasyAdapter.IEasyAdapter<ESONObject>) (holder1, data, position) -> {
                            String idCardNo = data.getJSONValue(mPresenter.sIdCardNo,"");
                            String name     = data.getJSONValue(mPresenter.sName,"");
                            holder1.setText(R.id.tv_index,String.valueOf(position+1));
                            holder1.setText(R.id.tv_name,name);
                            holder1.setText(R.id.tv_id_card_no,idCardNo);

                            holder1.setOnClickListener(R.id.tv_detail,v -> CommunityMemberDetailActivity.shownMember(CommunityRecoveryActivity.this,data));
                        });

                        adapterGroup = new EasyAdapter(CommunityRecoveryActivity.this, R.layout.recy_detail_recovery_group, mPresenter.getReadGroupRecovery(), (EasyAdapter.IEasyAdapter<ESONObject>) (holder1, data, position) -> {
                            String idCardNo = data.getJSONValue(mPresenter.sIdCardNo,"");
                            String name     = data.getJSONValue(mPresenter.sName,"");
                            String group    = data.getJSONValue(mPresenter.sGroupId,"");
                            holder1.setText(R.id.tv_index,String.valueOf(position+1));
                            holder1.setText(R.id.tv_group_id,group);
                            holder1.setText(R.id.tv_name,name);
                            holder1.setText(R.id.tv_id_card_no,idCardNo);

                            holder1.setOnClickListener(R.id.tv_detail,v -> NAGroupInDetailActivity.shownMember(CommunityRecoveryActivity.this,data));
                        });

                        rv.setAdapter(adapterMember);
                        rv.invalidate();

                        tvTabMember.setOnClickListener(v -> {
                            tvTabMember.setEnabled(false);
                            tvTabGroup.setEnabled(true);
                            cvTitleMember.setVisibility(View.VISIBLE);
                            cvTitleGroup.setVisibility(View.INVISIBLE);
                            rv.setAdapter(adapterMember);
                            adapterMember.notifyDataSetChanged();
                        });

                        tvTabGroup.setOnClickListener(v -> {
                            tvTabMember.setEnabled(true);
                            tvTabGroup.setEnabled(false);
                            cvTitleMember.setVisibility(View.INVISIBLE);
                            cvTitleGroup.setVisibility(View.VISIBLE);
                            rv.setAdapter(adapterGroup);
                            adapterGroup.notifyDataSetChanged();
                        });

                        holder.setOnClickListener(R.id.tv_re_pick,v -> {
                            unlockView();
                            holder.dismissDialog();
                        });

                        holder.setOnClickListener(R.id.tv_start,v->{
                            holder.dismissDialog();
                            onStartSaving();
                        });
                    }
                })
                .setForegroundResource(R.drawable.shape_dialog_corner_with_tl_tr)
                .setDialogParams(p.x,p.y- ((int)DensityUtils.dp2px(80f)), Gravity.BOTTOM)
                .setAllowDismissWhenTouchOutside(false)
                .setAllowDismissWhenBackPressed(false)
                .showDialog();

    }

    private void showReadMemberLocalDialog(){
        showPreviewDialog(mPresenter.getReadMemberLocal(),true,true,false);
    }
    private void showReadMemberRecoveryDialog(){
        showPreviewDialog(mPresenter.getReadMemberRecovery(),true,true,false);
    }
    private void showReadMemberDuplicatedDialog(){
        if(mPresenter.getMode() == 0 && mPresenter.getPriority() == 2){
            showPreviewDialog(mPresenter.getReadMemberDuplicated(),true,true,true);
        }
        else{
            List<ESONObject> lstData = new ArrayList<>();
            List<Pair<ESONObject,ESONObject>> lst = mPresenter.getReadMemberDuplicated();
            for(int i=0,ni=lst.size();i<ni;++i){
                lstData.add(lst.get(i).first);
            }
            showPreviewDialog(lstData,true,true,false);
        }
    }
    private void showReadGroupLocalDialog(){
        showPreviewDialog(mPresenter.getReadGroupLocal(),true,false,false);
    }
    private void showReadGroupRecoveryDialog(){
        showPreviewDialog(mPresenter.getReadGroupRecovery(),true,false,false);
    }
    private void showReadGroupDuplicatedDialog(){
        if(mPresenter.getMode() == 0 && mPresenter.getPriority() == 2){
            showPreviewDialog(mPresenter.getReadGroupDuplicated(),true,false,true);
        }
        else{
            List<ESONObject> lstData = new ArrayList<>();
            List<Pair<ESONObject,ESONObject>> lst = mPresenter.getReadGroupDuplicated();
            for(int i=0,ni=lst.size();i<ni;++i){
                lstData.add(lst.get(i).first);
            }
            showPreviewDialog(lstData,true,false,false);
        }
    }
    //endregion

    //region 3保存数据

    private void onStartSaving(){
        mPresenter.autoSave();
    }

    @Override
    public void onSaving(int progress) {
        pbProcess.setProgress(progress);
    }

    @Override
    public void onSavingMemberSuccessCount(int count) {
        svMemberSaveOk.tvValue.setText(String.format("共%d条，保存完毕后查看。",count));
    }

    @Override
    public void onSavingMemberFailureCount(int count) {
        svMemberSaveErr.tvValue.setText(String.format("共%d条，保存完毕后查看。",count));
    }

    @Override
    public void onSavingGroupSuccessCount(int count) {
        svGroupSaveOk.tvValue.setText(String.format("共%d条，保存完毕后查看。",count));
    }

    @Override
    public void onSavingGroupFailureCount(int count) {
        svGroupSaveErr.tvValue.setText(String.format("共%d条，保存完毕后查看。",count));
    }

    @Override
    public void onSaveSuccess() {
        mPresenter.markTransactionSuccess();
        pbProcess.setProgress(100);

        svMemberSaveOk .tvValue.setText(svMemberSaveOk .tvValue.getText().toString().replaceAll("保存完毕后查看","点击查看"));
        svMemberSaveErr.tvValue.setText(svMemberSaveErr.tvValue.getText().toString().replaceAll("保存完毕后查看","点击查看"));
        svGroupSaveOk  .tvValue.setText(svGroupSaveOk  .tvValue.getText().toString().replaceAll("保存完毕后查看","点击查看"));
        svGroupSaveErr .tvValue.setText(svGroupSaveErr .tvValue.getText().toString().replaceAll("保存完毕后查看","点击查看"));

        svMemberSaveOk .tvValue.setEnabled(true);
        svMemberSaveErr.tvValue.setEnabled(true);
        svGroupSaveOk  .tvValue.setEnabled(true);
        svGroupSaveErr .tvValue.setEnabled(true);

        if(mPresenter.getProgress() == mPresenter.PROGRESS_SUCCESS){
            DialogManager.showAlertDialogWithConfirm(this,"数据还原成功！",null);
        }
        else{
            if(!mPresenter.getReadMemberDuplicated().isEmpty()){
                showReadMemberDuplicatedDialog();
                return;
            }
            if(!mPresenter.getReadGroupDuplicated().isEmpty()){
                showReadGroupDuplicatedDialog();
                return;
            }
            DialogManager.showAlertDialogWithConfirm(this,"数据还原成功！",null);
        }
    }

    @Override
    public void onSaveFailure(String err) {
        ToastUtils.show(err);
    }

    private void showSaveMemberSuccessPreviewDialog(){
        showPreviewDialog(mPresenter.getSaveMemberSuccess(),false,true,false);
    }

    private void showSaveMemberFailurePreviewDialog(){
        showPreviewDialog(mPresenter.getSaveMemberFailure(),false,true,false);
    }

    private void showSaveGroupSuccessPreviewDialog(){
        showPreviewDialog(mPresenter.getSaveGroupSuccess(),false,false,false);
    }

    private void showSaveGroupFailurePreviewDialog(){
        showPreviewDialog(mPresenter.getSaveGroupFailure(),false,false,false);
    }
    //endregion

}
