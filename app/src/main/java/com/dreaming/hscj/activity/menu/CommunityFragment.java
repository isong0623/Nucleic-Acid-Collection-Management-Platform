package com.dreaming.hscj.activity.menu;

import android.util.Pair;

import com.dreaming.hscj.R;
import com.dreaming.hscj.activity.community.backup.CommunityBackupActivity;
import com.dreaming.hscj.activity.community.info.CommunityInfoActivity;
import com.dreaming.hscj.activity.community.input.CommunityInputActivity;
import com.dreaming.hscj.activity.community.manage.CommunityManageActivity;
import com.dreaming.hscj.activity.community.recovery.CommunityRecoveryActivity;
import com.dreaming.hscj.activity.community.search.CommunitySearchActivity;

import java.util.ArrayList;
import java.util.List;

public class CommunityFragment extends BaseMenuFragment{

    private final List<Pair<Integer,String>> lstAdapterConfig = new ArrayList(){{
        add(new Pair(R.drawable.ic_menu_input    ,"成员录入"));
        add(new Pair(R.drawable.ic_menu_manage   ,"成员管理"));
        add(new Pair(R.drawable.ic_menu_search   ,"成员查询"));
        add(new Pair(R.drawable.ic_menu_community,"社区信息"));
        add(new Pair(R.drawable.ic_menu_backup   ,"数据备份"));
        add(new Pair(R.drawable.ic_menu_recovery ,"数据还原"));
    }};

    private final List<Class> lstJumpingConfig = new ArrayList<Class>(){{
        add(CommunityInputActivity.class);
        add(CommunityManageActivity.class);
        add(CommunitySearchActivity.class);
        add(CommunityInfoActivity.class);
        add(CommunityBackupActivity.class);
        add(CommunityRecoveryActivity.class);
    }};

    private final List<Boolean> lstLoginConfig = new ArrayList<Boolean>(){{
        add(true);
        add(false);
        add(false);
        add(true);
        add(false);
        add(false);
    }};

    private final List<Boolean> lstCreateDBConfig = new ArrayList<Boolean>(){{
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
