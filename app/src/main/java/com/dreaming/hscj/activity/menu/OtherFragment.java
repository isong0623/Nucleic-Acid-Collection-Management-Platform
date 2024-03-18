package com.dreaming.hscj.activity.menu;

import android.util.Pair;

import com.dreaming.hscj.R;
import com.dreaming.hscj.activity.system.AboutActivity;
import com.dreaming.hscj.activity.system.FeedbackActivity;
import com.dreaming.hscj.activity.system.QRGeneratorActivity;
import com.dreaming.hscj.activity.system.RecommendActivity;
import com.dreaming.hscj.activity.system.SettingsActivity;

import java.util.ArrayList;
import java.util.List;

public class OtherFragment extends BaseMenuFragment{

    public final List<Pair<Integer,String>> lstAdapterConfig = new ArrayList(){{
        add(new Pair(R.drawable.ic_menu_qr_generate ,"扫码生成"));
        add(new Pair(R.drawable.ic_menu_recommend   ,"工具推荐"));
        add(new Pair(R.drawable.ic_menu_feedback    ,"反馈建议"));
        add(new Pair(R.drawable.ic_menu_settings    ,"软件设置"));
        add(new Pair(R.drawable.ic_menu_about       ,"关于软件"));
    }};

    public final List<Class> lstJumpingConfig = new ArrayList<Class>(){{
        add(QRGeneratorActivity.class);
        add(RecommendActivity.class);
        add(FeedbackActivity.class);
        add(SettingsActivity.class);
        add(AboutActivity.class);
    }};

    public final List<Boolean> lstLoginConfig = new ArrayList<Boolean>(){{
        add(false);
        add(false);
        add(false);
        add(false);
        add(false);
    }};

    public final List<Boolean> lstCreateDBConfig = new ArrayList<Boolean>(){{
        add(false);
        add(false);
        add(false);
        add(false);
        add(false);
    }};


    @Override
    List<Pair<Integer, String>> getAdapterConfig() {
        return lstAdapterConfig;
    }

    @Override
    List<Class> getJumpingClassConfig() {
        return lstJumpingConfig;
    }

    @Override
    List<Boolean> getLoginConfig() {
        return lstLoginConfig;
    }

    @Override
    List<Boolean> getDbCreateConfig() {
        return lstCreateDBConfig;
    }
}
