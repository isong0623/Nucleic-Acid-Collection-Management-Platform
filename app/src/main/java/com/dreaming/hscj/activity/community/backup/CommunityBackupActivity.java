package com.dreaming.hscj.activity.community.backup;

import android.Manifest;
import android.content.DialogInterface;
import android.view.View;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.dreaming.hscj.Constants;
import com.dreaming.hscj.R;
import com.dreaming.hscj.base.contract.BaseMVPActivity;
import com.dreaming.hscj.dialog.DialogManager;
import com.dreaming.hscj.dialog.loading.LoadingDialog;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.template.database.DatabaseTemplate;
import com.dreaming.hscj.utils.EasyPermission;
import com.dreaming.hscj.utils.ToastUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import priv.songxusheng.easyjson.ESONObject;

public class CommunityBackupActivity extends BaseMVPActivity<CommunityBackupPresenter> implements ICommunityBackupContract.View{
    @Override
    public int getContentViewResId() {
        return R.layout.activity_community_backup;
    }

    @Override
    protected void onBindView(View vContent) {
        ButterKnife.bind(this,vContent);
    }

    @BindView(R.id.part_database_info)
    CardView cvDbInfo;
    @BindView(R.id.part_db_1_info)
    CardView cvDb1Info;
    @BindView(R.id.part_db_2_info)
    CardView cvDb2Info;

    TextView tvDbInfoName;
    TextView tvDbInfoValue;
    TextView tvDb1InfoName;
    TextView tvDb1InfoValue;
    TextView tvDb2InfoName;
    TextView tvDb2InfoValue;

    @Override
    public void initView() {
        setCenterText("社区数据库备份");
        tvDbInfoName = cvDbInfo.findViewById(R.id.tv_name);
        tvDb1InfoName = cvDb1Info.findViewById(R.id.tv_name);
        tvDb2InfoName = cvDb2Info.findViewById(R.id.tv_name);
        tvDbInfoValue = cvDbInfo.findViewById(R.id.tv_value);
        tvDb1InfoValue = cvDb1Info.findViewById(R.id.tv_value);
        tvDb2InfoValue = cvDb2Info.findViewById(R.id.tv_value);

        tvDbInfoName.setText("当前数据库：");
        tvDbInfoName.setWidth(dp2px(150f));
        tvDbInfoName.invalidate();

        tvDb1InfoName.setText("社区成员记录：");
        tvDb1InfoName.setWidth(dp2px(150f));
        tvDb1InfoName.invalidate();

        tvDb2InfoName.setText("核酸分组记录：");
        tvDb2InfoName.setWidth(dp2px(150f));
        tvDb2InfoName.invalidate();

        ESONObject object = Constants.DBConfig.getSelectedDatabase();
        String sTownName    = object.getJSONValue("townName"   ,"");
        String sVillageName = object.getJSONValue("villageName","");
        tvDbInfoValue.setText(String.format("%s/%s/%s", Template.getCurrentTemplate().getDatabaseSetting().getRegionName(),sTownName,sVillageName));

        tvDb1InfoValue.setText("共？条");
        tvDb2InfoValue.setText("共？组");

        mPresenter.queryDb1Num();
        mPresenter.queryDb2Num();
    }

    @Override
    public void setDb1Num(int num) {
        tvDb1InfoValue.setText(String.format("共%d条",num));
    }

    @Override
    public void setDb2Num(int num) {
        tvDb2InfoValue.setText(String.format("共%d组",num));
    }

    @Override
    public void onBackUpFailure(String err) {
        LoadingDialog.dismissDialog("Backup");
        ToastUtils.show("备份失败："+err);
    }

    @Override
    public void onBackUpSuccess(String path) {
        LoadingDialog.dismissDialog("Backup");
        ToastUtils.show("备份成功！");
        DialogManager.showAlertDialogWithConfirm(this, "备份成功","备份路径：" + path, new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                finish();
            }
        });
    }

    @OnClick(R.id.tv_start)
    void onBackupClicked(){
        requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, new EasyPermission.PermissionResultListener() {
            @Override
            public void onPermissionGranted() {
                LoadingDialog.showDialog("Backup",CommunityBackupActivity.this);
                mPresenter.backup();
            }

            @Override
            public void onPermissionDenied() {
                ToastUtils.show("无法读写外置存储器，无法备份！");
            }
        });
    }
}
