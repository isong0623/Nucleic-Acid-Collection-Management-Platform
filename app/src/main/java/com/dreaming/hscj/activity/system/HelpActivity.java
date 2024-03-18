package com.dreaming.hscj.activity.system;

import android.view.View;
import android.widget.ImageView;


import com.dreaming.hscj.App;
import com.dreaming.hscj.R;
import com.dreaming.hscj.base.BaseActivity;
import com.dreaming.hscj.core.ThreadPoolProvider;
import com.dreaming.hscj.dialog.loading.LoadingDialog;
import com.dreaming.hscj.utils.FileUtils;
import com.dreaming.hscj.utils.ToastUtils;
import com.dreaming.hscj.widget.WebView;

import net.lingala.zip4j.ZipFile;

import java.io.File;
import java.io.FileOutputStream;

import butterknife.BindView;
import butterknife.ButterKnife;

public class HelpActivity extends BaseActivity {

    @Override
    public int getContentViewResId() {
        return R.layout.activity_help;
    }

    @BindView(R.id.wv)
    WebView webView;
    @BindView(R.id.iv_top_1)
    ImageView ivTop1;
    @BindView(R.id.iv_top_2)
    ImageView ivTop2;

    @Override
    protected void onBindView(View vContent) {
        ButterKnife.bind(this,vContent);
    }

    @Override
    public void initView() {
        setCenterText("全民核酸采集管理平台使用说明");

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

        LoadingDialog.showDialog("Extract Zip",this);
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            File f = new File(App.sInstance.getCacheDir(),"html.zip");
            String pwd = "*aR[fS(wEy552!gCVL!TM?$.d[w4jQ;6<@Hw!hp[as{<jd3<x,}!J29:Ub#gB\\T<iU1t>tb]AHv6jN'\"g;_-5W!@oN{`~A42?Bd,amfoF=z)V'jO41f2heHph%qa:eUI(se8\\b<+8$s+'%)ki?zKFCG4{.SI(OLR+(jq%0Df9LP}}l{6B:Pz+$eoo2kX`(*z\"57`qp6,CX^c,3P$|=l1S1Y^L('ErZ|njwGG/4EP%{kOy[!Egz+l\\\\;[09wJ\\QI?";
            ZipFile zip = new ZipFile(f,pwd.toCharArray());
            try {
                FileUtils.copy(getResources().openRawResource(R.raw.app_help),new FileOutputStream(f));
                zip.extractAll(new File(App.sInstance.getCacheDir(),"html").getAbsolutePath());
                File fHtml = new File(App.sInstance.getCacheDir().getAbsolutePath()+File.separator+"html"+File.separator+"app_help.html");
                String htmlData = FileUtils.readAll(fHtml.getAbsolutePath());
                File fRes  = new File(fHtml.getParentFile(),"app_help.files");
                App.Post(()->{
                    webView.getSettings().setAllowFileAccess(true);
                    webView.getSettings().setAllowContentAccess(true);

                    webView.loadDataWithBaseURL("file:///"+fRes.getAbsolutePath(),htmlData,"text/html", "utf-8", null);
                    LoadingDialog.dismissDialog("Extract Zip");
                });

            } catch (Exception e) {
                e.printStackTrace();
                App.Post(()->{
                    LoadingDialog.dismissDialog("Extract Zip");
                    ToastUtils.show("加载失败！");
                    finish();
                });
            }
        });

    }


    void reloadWebView(){
        File fHtml = new File(App.sInstance.getCacheDir().getAbsolutePath()+File.separator+"html"+File.separator+"app_help.html");
        String htmlData = FileUtils.readAll(fHtml.getAbsolutePath());
        File fRes  = new File(fHtml.getParentFile(),"app_help.files");
        webView.loadDataWithBaseURL("file:///"+fRes.getAbsolutePath(),htmlData,"text/html", "utf-8", null);
    }
}
