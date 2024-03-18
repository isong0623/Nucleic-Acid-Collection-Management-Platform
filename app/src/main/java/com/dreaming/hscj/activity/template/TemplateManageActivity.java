package com.dreaming.hscj.activity.template;

import android.Manifest;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dreaming.hscj.App;
import com.dreaming.hscj.Constants;
import com.dreaming.hscj.R;
import com.dreaming.hscj.activity.template.test.ApiTestActivity;
import com.dreaming.hscj.base.BaseActivity;
import com.dreaming.hscj.core.BundleBuilder;
import com.dreaming.hscj.core.EasyAdapter.EasyAdapter;
import com.dreaming.hscj.dialog.DialogManager;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.utils.EasyPermission;
import com.dreaming.hscj.utils.ToastUtils;
import com.dreaming.hscj.widget.InputView;
import com.dreaming.hscj.widget.ShownView;
import com.leon.lfilepickerlibrary.LFilePicker;
import com.leon.lfilepickerlibrary.utils.Constant;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import priv.songxusheng.easydialog.EasyDialog;
import priv.songxusheng.easydialog.EasyDialogHolder;
import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

public class TemplateManageActivity extends BaseActivity {
    private static final String TAG = TemplateManageActivity.class.getSimpleName();
    @Override
    public int getContentViewResId() {
        return R.layout.activity_template_adapt_manage;
    }

    @Override
    protected void onBindView(View vContent) {
        ButterKnife.bind(this,vContent);
    }

    @BindView(R.id.rv_template)
    RecyclerView rvTemplate;
    List<ESONObject> lstData = new ArrayList<>();

    void loadData(){
        lstData.clear();
        ESONArray a = Constants.TemplateConfig.getTemplateList();
        for(int i=0,ni=a.length();i<ni;++i){
            ESONObject o = a.getArrayValue(i,new ESONObject());
            if(o.length()==0) continue;

            lstData.add(o);
        }
        rvTemplate.getAdapter().notifyDataSetChanged();
    }

    @Override
    public void initView() {

        setCenterText("APP模板管理");

        setRightText("新增适配");

        tvTitleCenter.setOnClickListener(new View.OnClickListener() {
            long lClickTime = 0L;
            int clickCount = 0;
            @Override
            public void onClick(View v) {
                long now = System.currentTimeMillis();
                if(now - lClickTime < 5000){
                    ++clickCount;
                    if(clickCount == 6){
                        lClickTime = 0L;
                        new EasyDialog(R.layout.dialog_api_login,TemplateManageActivity.this)
                                .setOnBindDialogListener(new EasyDialog.OnBindDialogListener() {
                                    @Override
                                    public void onBindDialog(EasyDialogHolder holder) {
                                        InputView ivPassword = holder.getView(R.id.iv_pwd);
                                        holder.setOnClickListener(R.id.iv_close,v->holder.dismissDialog());
                                        holder.setOnClickListener(R.id.tv_login,v->{
                                            if(Template.getCurrentTemplate().getUserInputGuideDatabase().isValidLoginPassword(ivPassword.tvValue.getText().toString())){
                                                ToastUtils.show("密码正确");
                                                holder.dismissDialog();
                                                startActivity(ApiTestActivity.class, BundleBuilder.create("password",ivPassword.tvValue.getText().toString()).build());
                                            }
                                            else{
                                                ToastUtils.show("密码错误！");
                                                holder.dismissDialog();
                                            }
                                        });
                                    }
                                })
                                .setDialogHeight(dp2px(130))
                                .setForegroundResource(R.drawable.shape_common_dialog)
                                .setAllowDismissWhenTouchOutside(false)
                                .showDialog();
                    }
                    return;
                }
                lClickTime = now;
                clickCount = 0;
            }
        });

        tvTitleRight.setOnClickListener(v -> selectTemplateFile());

        rvTemplate.setLayoutManager(new LinearLayoutManager(this){
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                try {
                    super.onLayoutChildren( recycler, state );
                } catch (IndexOutOfBoundsException e) {
                    e.printStackTrace();
                }
            }
        });
        rvTemplate.setAdapter(new EasyAdapter(this, R.layout.recy_template_choose, lstData, (EasyAdapter.IEasyAdapter<ESONObject>) (holder, data, position) -> {
            TextView tvId         = holder.getView(R.id.tv_id_value);
            TextView tvRegionName = holder.getView(R.id.tv_region_name_value);
            TextView tvRegionCode = holder.getView(R.id.tv_region_code_value);
            TextView tvProvider   = holder.getView(R.id.tv_provider_value);
            TextView tvVersion    = holder.getView(R.id.tv_version_value);

            String unity = data.getJSONValue("unity","");
            if(unity.replaceAll("0","").length() == 0){
                unity = "******************";
            }
            else{
                unity = unity.substring(0,4)+"**********"+unity.substring(14);
            }

            tvId         .setText(unity);
            tvRegionName .setText(data.getJSONValue("region name",""));
            tvRegionCode .setText(data.getJSONValue("region code",""));
            tvProvider   .setText(data.getJSONValue("provider",""));
            tvVersion    .setText(data.getJSONValue("version",""));

            Log.e(TAG,"item:"+position +" " + data.toString());

            CheckBox cb = holder.getView(R.id.cb_choose);
            if(Constants.TemplateConfig.getCurrentTemplate().getJSONValue("id",-1) == data.getJSONValue("id",-2)){
                cb.setChecked(true);
                holder.getRootView().setBackground(getResources().getDrawable(R.drawable.shape_recy_choose_template_2));
            }
            else {
                cb.setChecked(false);
                holder.getRootView().setBackground(getResources().getDrawable(R.drawable.shape_recy_choose_template_1));
                cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if(isChecked){
                            DialogManager.showAlertDialog(TemplateManageActivity.this,"提示","确定要切换模板吗？", v -> cb.setChecked(false), v -> {
                                boolean bSwitchResult = false;
                                if(Template.getDefault().getDatabaseSetting().getUnifySocialCreditCodes().equalsIgnoreCase(tvId.getText().toString())&&
                                        Template.getDefault().getDatabaseSetting().getRegionCode().equals(tvRegionCode.getText().toString())){
                                    try {
                                        Template.getDefault().setCurrentTemplate();
                                        bSwitchResult = true;
                                    } catch (Exception e) {
                                    }
                                }
                                else{
                                    try {
                                        Template.read(data.getJSONValue("path","")).setCurrentTemplate();
                                        bSwitchResult = false;
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                                if(bSwitchResult){
                                    ToastUtils.show("切换失败！");
                                    return;
                                }
                                for(int i=0;i<10;++i){
                                    if(Constants.TemplateConfig.setCurrentTemplate(data)){
                                        break;
                                    }
                                }
                                if(Constants.TemplateConfig.getCurrentTemplate().getJSONValue("id",-1) == data.getJSONValue("id",-2)){
                                    ToastUtils.show("切换成功！");
                                    rvTemplate.getAdapter().notifyDataSetChanged();
                                    if(Constants.User.isLogin()){
                                        Template.getCurrentTemplate().getUserOverallDatabase().sync();
                                    }
                                    Template.getCurrentTemplate().getNoneGroupingDatabase().sync(null);
                                }
                                else {
                                    ToastUtils.show("切换失败！");
                                }
                            });
                        }
                    }
                });
            }

        },false));

        loadData();
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

                readTemplateFile(f.getAbsolutePath());
            }
        }
    }

    void selectTemplateFile(){
        new EasyDialog(R.layout.dialog_template_select_file,this)
                .setOnBindDialogListener(new EasyDialog.OnBindDialogListener() {

                    private String path1 = App.sInstance.getExternalCacheDir().getAbsolutePath();
                    private String path2 = path1.lastIndexOf("Android") >-1 ? path1.substring(0,path1.lastIndexOf("Android")) : "/storage/emulated/0";

                    public final String sQQRootPath = path2+"/tencent/QQfile_recv";
                    public final String sWXRootPath = path2+"/tencent/MicroMsg/Download";
                    public final String sRootPath   = path2;

                    String sInitPath = sRootPath;

                    @Override
                    public void onBindDialog(EasyDialogHolder holder) {

                        holder.setOnClickListener(R.id.iv_close,v -> holder.dismissDialog());

                        RadioButton rbRootPath = holder.getView(R.id.rb_root_path);
                        RadioButton rbWX       = holder.getView(R.id.rb_wx);
                        RadioButton rbQQ       = holder.getView(R.id.rb_qq);

                        CompoundButton.OnCheckedChangeListener listener = new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                if(isChecked){
                                    switch (buttonView.getId()){
                                        case R.id.rb_root_path:
                                            sInitPath = sRootPath;
                                            break;
                                        case R.id.rb_qq:
                                            if(!new File(sQQRootPath).exists()){
                                                rbRootPath.setChecked(true);
                                                Toast.makeText(TemplateManageActivity.this, "未找到QQ存储文件夹！", Toast.LENGTH_SHORT).show();
                                            }
                                            else{
                                                sInitPath = sQQRootPath;
                                            }
                                            break;
                                        case R.id.rb_wx:
                                            if(!new File(sWXRootPath).exists()){
                                                rbRootPath.setChecked(true);
                                                Toast.makeText(TemplateManageActivity.this, "未找到微信存储文件夹！", Toast.LENGTH_SHORT).show();
                                            }
                                            else{
                                                sInitPath = sWXRootPath;
                                            }
                                            break;
                                    }
                                }
                            }
                        };

                        rbRootPath .setOnCheckedChangeListener(listener);
                        rbWX       .setOnCheckedChangeListener(listener);
                        rbQQ       .setOnCheckedChangeListener(listener);

                       holder.setOnClickListener(R.id.tv_choose_file,v->{
                           holder.dismissDialog();
                           requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, new EasyPermission.PermissionResultListener() {
                               @Override
                               public void onPermissionGranted() {
                                   new LFilePicker()
                                           .withActivity(TemplateManageActivity.this)
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
                                           .withFileFilter(new String[]{"template"})
                                           .start();
                               }

                               @Override
                               public void onPermissionDenied() {
                                    ToastUtils.show("没有获取外置存储权限，无法读取模板文件！");
                               }
                           });
                       });
                    }
                })
                .setDialogHeight(dp2px(210f))
                .setForegroundResource(R.drawable.shape_common_dialog)
                .showDialog();
    }

    void readTemplateFile(String path){
        try {
            Template template = Template.read(path);
            onReadTemplateSuccess(template);
        } catch (Exception e) {
            onReadTemplateFailure(e.getMessage());
        }
    }

    void onReadTemplateSuccess(Template template){
        new EasyDialog(R.layout.dialog_template_shown,this)
                .setOnBindDialogListener(new EasyDialog.OnBindDialogListener() {
                    @Override
                    public void onBindDialog(EasyDialogHolder holder) {
                        ShownView svId         = holder.getView(R.id.sv_id);
                        ShownView svRegionName = holder.getView(R.id.sv_region_name);
                        ShownView svRegionCode = holder.getView(R.id.sv_region_code);
                        ShownView svProvider   = holder.getView(R.id.sv_provider);
                        ShownView svVersion    = holder.getView(R.id.sv_version);

                        svId        .tvValue.setText(template.getDatabaseSetting().getUnifySocialCreditCodes());
                        svRegionName.tvValue.setText(template.getDatabaseSetting().getRegionName());
                        svRegionCode.tvValue.setText(template.getDatabaseSetting().getRegionCode());
                        svProvider  .tvValue.setText(template.getDatabaseSetting().getNetApiProvider());
                        svVersion   .tvValue.setText(template.getDatabaseSetting().getNetApiVersion()+"");

                        holder.setOnClickListener(R.id.tv_re_pick, v -> holder.dismissDialog());
                        holder.setOnClickListener(R.id.tv_start,v -> {
                            ESONObject e = new ESONObject()
                                    .putValue("path",template.getPath())
                                    .putValue("crc32",template.getTempCode())
                                    .putValue("region code",template.getDatabaseSetting().getRegionCode())
                                    .putValue("region name",template.getDatabaseSetting().getRegionName())
                                    .putValue("provider",template.getDatabaseSetting().getNetApiProvider())
                                    .putValue("version",template.getDatabaseSetting().getNetApiVersion())
                                    .putValue("unity",template.getDatabaseSetting().getUnifySocialCreditCodes());

                            switch (Constants.TemplateConfig.addTemplate(e)){
                                case 0:
                                    ToastUtils.show("模板添加成功！");
                                    try {
                                        template.setCurrentTemplate();
                                    } catch (IOException ex) {
                                        ex.printStackTrace();
                                    }
                                    loadData();
                                    holder.dismissDialog();
                                    break;
                                case 1:
                                    ToastUtils.show("配置已存在！");
                                    break;
                                case 2:
                                    ToastUtils.show("保存失败！");
                                    break;
                                case 3:
                                    ToastUtils.show("保存失败，本地已有更高版本的模板，禁止模板降级!");
                                    break;
                            }
                        });
                    }
                })
                .setDialogHeight(dp2px(230f))
                .setForegroundResource(R.drawable.shape_common_dialog)
                .setAllowDismissWhenBackPressed(false)
                .setAllowDismissWhenTouchOutside(false)
                .showDialog();
    }

    void onReadTemplateFailure(String err){
        DialogManager.showAlertDialogWithConfirm(this,"提示","模板读入失败："+err,null);
    }

}
