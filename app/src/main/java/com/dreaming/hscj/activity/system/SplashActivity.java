package com.dreaming.hscj.activity.system;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;

import com.baidu.paddle.lite.demo.ocr.PredictEngine;
import com.dreaming.hscj.App;
import com.dreaming.hscj.activity.menu.MenuActivity;
import com.dreaming.hscj.base.BaseActivity;

public class SplashActivity extends BaseActivity {
    @Override
    public int getContentViewResId() { return 0; }

    @Override
    protected boolean isPreLoadMode() { return true; }

    @Override
    protected void onResume() {
        super.onResume();
        PredictEngine.init(this);
        App.PostDelayed(()->{
            startActivity(MenuActivity.class);
            finish();
        },500);
    }

    @Override
    public Resources getResources() {
        Resources res = super.getResources();
        Configuration config = new Configuration();
        config.setToDefaults();
        res.updateConfiguration(config, res.getDisplayMetrics());
        return res;
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(Intent.ACTION_MAIN){{ addCategory(Intent.CATEGORY_HOME);}});
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0,0);
    }
}
