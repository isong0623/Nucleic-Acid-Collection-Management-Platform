package com.dreaming.hscj.activity.menu;

import android.util.Pair;

import com.dreaming.hscj.R;
import com.dreaming.hscj.activity.nucleic_acid.exchange.NAExchangeActivity;
import com.dreaming.hscj.activity.nucleic_acid.export.NAExportActivity;
import com.dreaming.hscj.activity.nucleic_acid.offline_sampling.NAOfflineSamplingActivity;
import com.dreaming.hscj.activity.system.AboutActivity;
import com.dreaming.hscj.activity.nucleic_acid.grouping.NAGroupingActivity;
import com.dreaming.hscj.activity.nucleic_acid.searching.NASearchingActivity;
import com.dreaming.hscj.activity.community.backup.CommunityBackupActivity;
import com.dreaming.hscj.activity.community.info.CommunityInfoActivity;
import com.dreaming.hscj.activity.community.input.CommunityInputActivity;
import com.dreaming.hscj.activity.community.manage.CommunityManageActivity;
import com.dreaming.hscj.activity.community.recovery.CommunityRecoveryActivity;
import com.dreaming.hscj.activity.community.search.CommunitySearchActivity;
import com.dreaming.hscj.activity.nucleic_acid.sampling.NASamplingActivity;
import com.dreaming.hscj.activity.system.FeedbackActivity;
import com.dreaming.hscj.activity.system.QRGeneratorActivity;
import com.dreaming.hscj.activity.system.RecommendActivity;

import java.util.ArrayList;
import java.util.List;

public class MenuPresenter extends IMenuContract.Presenter{
    public final List<List<Pair<Integer,String>>> lstAdapterConfig = new ArrayList(){{
        add(new ArrayList(){{
            add(new Pair(R.drawable.ic_menu_online_sampling       ,"核酸在线采样"));
            add(new Pair(R.drawable.ic_menu_offline_sampling      ,"核酸离线采样"));
            add(new Pair(R.drawable.ic_menu_grouping              ,"核酸成员分组"));
            add(new Pair(R.drawable.ic_menu_sampling_search_local ,"本地采样查询"));
            add(new Pair(R.drawable.ic_menu_sampling_search_online,"网络采样查询"));
            add(new Pair(R.drawable.ic_menu_exchange              ,"条码转移"));
            add(new Pair(R.drawable.ic_menu_export                ,"采样记录导出"));
        }});
        add(new ArrayList(){{
            add(new Pair(R.drawable.ic_menu_community,"社区信息"));
            add(new Pair(R.drawable.ic_menu_manage   ,"成员管理"));
            add(new Pair(R.drawable.ic_menu_search   ,"成员查询"));
            add(new Pair(R.drawable.ic_menu_input    ,"成员录入"));
            add(new Pair(R.drawable.ic_menu_backup   ,"数据备份"));
            add(new Pair(R.drawable.ic_menu_recovery ,"数据还原"));
        }});
        add(new ArrayList(){{
            add(new Pair(R.drawable.ic_menu_feedback    ,"反馈建议"));
            add(new Pair(R.drawable.ic_menu_about       ,"关于软件"));
            add(new Pair(R.drawable.ic_menu_recommend   ,"工具推荐"));
            add(new Pair(R.drawable.ic_menu_qr_generate ,"扫码生成"));
        }});
    }};

    public final List<List<Class>> lstJumpingConfig = new ArrayList(){{
        add(new ArrayList<Class>(){{
            add(NASamplingActivity.class);
            add(NAOfflineSamplingActivity.class);
            add(NAGroupingActivity.class);
            add(null);
            add(NASearchingActivity.class);
            add(NAExchangeActivity.class);
            add(NAExportActivity.class);
        }});
        add(new ArrayList<Class>(){{
            add(CommunityInfoActivity.class);
            add(CommunityManageActivity.class);
            add(CommunitySearchActivity.class);
            add(CommunityInputActivity.class);
            add(CommunityBackupActivity.class);
            add(CommunityRecoveryActivity.class);
        }});
        add(new ArrayList<Class>(){{
            add(FeedbackActivity.class);
            add(AboutActivity.class);
            add(RecommendActivity.class);
            add(QRGeneratorActivity.class);
        }});
    }};

    public final List<List<Boolean>> lstLoginConfig = new ArrayList(){{
        add(new ArrayList<Boolean>(){{
            add(true);
            add(true);
            add(true);
            add(true);
            add(true);
            add(true);
            add(true);
        }});
        add(new ArrayList<Boolean>(){{
            add(true);
            add(false);
            add(false);
            add(true);
            add(false);
            add(false);
        }});
        add(new ArrayList<Boolean>(){{
            add(false);
            add(false);
            add(false);
            add(false);
        }});
    }};

    public final List<List<Boolean>> lstCreateDBConfig = new ArrayList(){{
        add(new ArrayList<Boolean>(){{
            add(true);
            add(true);
            add(true);
            add(true);
            add(false);
            add(true);
            add(true);
        }});
        add(new ArrayList<Boolean>(){{
            add(false);
            add(true);
            add(true);
            add(true);
            add(true);
            add(true);
        }});
        add(new ArrayList<Boolean>(){{
            add(false);
            add(false);
            add(false);
            add(false);
        }});
    }};

    @Override
    boolean shouldLogin(int type, int position) {
        if(type < lstLoginConfig.size()){
            if(position<lstLoginConfig.get(type).size()){
                return lstLoginConfig.get(type).get(position);
            }
        }
        return false;
    }

    @Override
    boolean shouldCreateDB(int type, int position) {
        if(type < lstCreateDBConfig.size()){
            if(position<lstCreateDBConfig.get(type).size()){
                return lstCreateDBConfig.get(type).get(position);
            }
        }
        return false;
    }

    @Override
    Class getJumpingClass(int type, int position) {
        if(type < lstJumpingConfig.size()){
            List<Class> lst = lstJumpingConfig.get(type);
            if(position<lst.size()){
                return lst.get(position);
            }
        }
        return null;
    }

    private boolean bIsInit = true;
    @Override
    boolean isInitializing() {
        return bIsInit;
    }
    @Override
    void onInitSuccess() {
        bIsInit = false;
    }


}
