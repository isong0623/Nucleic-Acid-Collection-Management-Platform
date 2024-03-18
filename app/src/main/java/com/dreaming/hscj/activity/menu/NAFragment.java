package com.dreaming.hscj.activity.menu;

import android.util.Pair;

import com.dreaming.hscj.R;
import com.dreaming.hscj.activity.nucleic_acid.exchange.NAExchangeActivity;
import com.dreaming.hscj.activity.nucleic_acid.export.NAExportActivity;
import com.dreaming.hscj.activity.nucleic_acid.grouping.NAGroupingActivity;
import com.dreaming.hscj.activity.nucleic_acid.offline_sampling.NAOfflineSamplingActivity;
import com.dreaming.hscj.activity.nucleic_acid.sampling.NASamplingActivity;
import com.dreaming.hscj.activity.nucleic_acid.searching.NASearchingActivity;

import java.util.ArrayList;
import java.util.List;

public class NAFragment extends BaseMenuFragment{

    public final List<Pair<Integer,String>> lstAdapterConfig = new ArrayList(){{
        add(new Pair(R.drawable.ic_menu_online_sampling       ,"核酸在线采样"));
        add(new Pair(R.drawable.ic_menu_offline_sampling      ,"核酸离线采样"));
        add(new Pair(R.drawable.ic_menu_grouping              ,"核酸成员分组"));
        add(new Pair(R.drawable.ic_menu_sampling_search_local ,"本地采样查询"));
        add(new Pair(R.drawable.ic_menu_sampling_search_online,"网络采样查询"));
        add(new Pair(R.drawable.ic_menu_exchange              ,"条码转移"    ));
        add(new Pair(R.drawable.ic_menu_export                ,"采样记录导出"));
    }};

    public final List<Class> lstJumpingConfig = new ArrayList<Class>(){{
        add(NASamplingActivity.class);
        add(NAOfflineSamplingActivity.class);
        add(NAGroupingActivity.class);
        add(null);
        add(NASearchingActivity.class);
        add(NAExchangeActivity.class);
        add(NAExportActivity.class);
    }};

    public final List<Boolean> lstLoginConfig = new ArrayList<Boolean>(){{
        add(true);
        add(true);
        add(true);
        add(true);
        add(true);
        add(true);
        add(true);
    }};

    public final List<Boolean> lstCreateDBConfig = new ArrayList<Boolean>(){{
        add(true);
        add(true);
        add(true);
        add(true);
        add(false);
        add(true);
        add(true);
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
