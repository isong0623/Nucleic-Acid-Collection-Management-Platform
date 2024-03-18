package com.dreaming.hscj.activity.system;

import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.Toast;

import com.dreaming.hscj.R;
import com.dreaming.hscj.base.BaseActivity;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class RecommendActivity extends BaseActivity {
    @Override
    public int getContentViewResId() {
        return R.layout.activity_recommend;
    }

    @Override
    protected void onBindView(View vContent) {
        ButterKnife.bind(this,vContent);
    }

    @Override
    public void initView() {
        setCenterText("工具推荐");
    }

    @OnClick(R.id.iv_qr)
    void onJumpApkDownloadClicked(){
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://best335.lanzouv.com/i9xBR07mvrng")));
        } catch (Exception e) {
            Toast.makeText(this, "网页打开失败！\n\nhttps://best335.lanzouv.com/i9xBR07mvrng", Toast.LENGTH_LONG).show();
        }
    }

    @OnClick(R.id.tv_plugin)
    void onJumpPluginDownloadClicked(){
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://best335.lanzouv.com/iUMhW08cxugf")));
        } catch (Exception e) {
            Toast.makeText(this, "网页打开失败！\n\nhttps://best335.lanzouv.com/iUMhW08cxugf", Toast.LENGTH_LONG).show();
        }
    }
}
