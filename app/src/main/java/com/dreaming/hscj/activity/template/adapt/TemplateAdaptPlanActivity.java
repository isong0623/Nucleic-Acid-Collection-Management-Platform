package com.dreaming.hscj.activity.template.adapt;

import android.Manifest;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.dreaming.hscj.App;
import com.dreaming.hscj.R;
import com.dreaming.hscj.base.BaseActivity;
import com.dreaming.hscj.core.ThreadPoolProvider;
import com.dreaming.hscj.dialog.DialogManager;
import com.dreaming.hscj.dialog.loading.LoadingDialog;
import com.dreaming.hscj.utils.EasyPermission;
import com.dreaming.hscj.utils.FileUtils;
import com.dreaming.hscj.utils.ToastUtils;
import com.dreaming.hscj.widget.WebView;


import java.io.File;
import java.io.FileOutputStream;

import butterknife.BindView;
import butterknife.ButterKnife;

public class TemplateAdaptPlanActivity extends BaseActivity {

    @Override
    public int getContentViewResId() {
        return R.layout.activity_adapt_plan;
    }

    @BindView(R.id.wv)
    WebView webView;

    @Override
    protected void onBindView(View vContent) {
        ButterKnife.bind(this,vContent);
    }

    @BindView(R.id.iv_top_1)
    ImageView ivTop1;
    @BindView(R.id.iv_top_2)
    ImageView ivTop2;

    @Override
    public void initView() {
        setCenterText("模板适配说明");
        setRightText("导出适配文件");

        ivTop1.setOnClickListener(v -> reloadWebView());
        ivTop2.setOnClickListener(v -> reloadWebView());

        webView.setOnScrollStateChangedListener(new WebView.OnScrollStateChangedListener() {
            @Override
            public void onMoving() {
                ivTop1.setVisibility(View.INVISIBLE);
                ivTop2.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onStop() {
                ivTop1.setVisibility(View.VISIBLE);
                ivTop2.setVisibility(View.VISIBLE);
            }
        });

        tvTitleRight.setOnClickListener(v -> {
            if(!hasPermision(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                ToastUtils.show("导出到外置存储，请给予存储权限！");
            }
            requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, new EasyPermission.PermissionResultListener() {
                @Override
                public void onPermissionGranted() {
                    try {
                        File fDestDir = new File(FileUtils.getExternalDir(),"hscj_template_adapt.zip");
                        FileUtils.copy(getResources().openRawResource(R.raw.hscj),new FileOutputStream(fDestDir));

                        DialogManager.showAlertDialogWithConfirm(TemplateAdaptPlanActivity.this,"导出成功："+fDestDir.getAbsolutePath(),null);
                    } catch (Exception e) {
                        DialogManager.showAlertDialogWithConfirm(TemplateAdaptPlanActivity.this,"导出失败！",null);
                    }
                }

                @Override
                public void onPermissionDenied() {
                    ToastUtils.show("没有写入存储权限，导出失败！");
                }
            });

        });

        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowContentAccess(true);

        LoadingDialog.showDialog("Copy Html",this);

        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            File fDst = new File(getCacheDir(),"template_adapt.html");
            if(fDst.exists()) fDst.delete();
            else if(!fDst.getParentFile().exists()) fDst.getParentFile().mkdirs();

            try {
                FileUtils.copy(getResources().openRawResource(R.raw.template_adapt_direction), new FileOutputStream(fDst));
                App.Post(()->webView.loadUrl("file:///"+fDst.getAbsolutePath()));
            } catch (Exception e) {
                ToastUtils.show("文件打开失败！");
            }

            App.Post(()->LoadingDialog.dismissDialog("Copy Html"));
        });

    }

    void reloadWebView(){
        File fDst = new File(getCacheDir(),"template_adapt.html");
        webView.loadUrl("file:///"+fDst.getAbsolutePath());
    }

}
