package com.dreaming.hscj.activity.community.info;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;

import com.dreaming.hscj.Constants;
import com.dreaming.hscj.R;
import com.dreaming.hscj.activity.login.LoginActivity;
import com.dreaming.hscj.activity.template.adapt.TemplateAdaptActivity;
import com.dreaming.hscj.base.contract.BaseMVPActivity;
import com.dreaming.hscj.core.ThreadPoolProvider;
import com.dreaming.hscj.dialog.DialogManager;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.template.api.ApiProvider;
import com.dreaming.hscj.template.api.ApiTemplate;
import com.dreaming.hscj.template.api.impl.Api;
import com.scwang.smart.refresh.layout.api.RefreshLayout;

import java.util.Iterator;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import priv.songxusheng.easyjson.ESONObject;

public class CommunityInfoActivity extends BaseMVPActivity<CommunityInfoPresenter> implements ICommunityInfoContract.View{
    @Override
    public int getContentViewResId() {
        return R.layout.activity_community_info;
    }

    @Override
    protected void onBindView(View vContent) {
        ButterKnife.bind(this,vContent);
    }

    @Override
    public void initView() {
        setCenterText("社区信息");
        setRightText("APP模板适配");
        srlMain.setEnableLoadMore(false);
        tvTitleRight.setOnClickListener(v->{
            startActivity(TemplateAdaptActivity.class);
        });
        updateView();
    }

    @Override
    protected boolean hasRefreshBar() {
        return true;
    }

    @BindView(R.id.ll_container)
    LinearLayout llContainer;

    private void updateView(){
        llContainer.removeAllViews();

        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            ESONObject e = Constants.Community.getInfo();
            for (Iterator<String> it = e.keys(); it.hasNext(); ) {
                String key = it.next();
                String name = Template.getCurrentTemplate().apiOf(0).getResponse().getParamNameMapper().get(key);
                if(name == null){
                    name = Template.getCurrentTemplate().apiOf(1).getResponse().getParamNameMapper().get(key);
                }
                if(name == null) continue;
                final String sName = name;
                final String sValue = e.getJSONValue(key,"");
                post(()->{
                    CardView cv = (CardView) LayoutInflater.from(CommunityInfoActivity.this).inflate(R.layout.view_shown,null,false);
                    TextView tvName = cv.findViewById(R.id.tv_name);
                    TextView tvValue= cv.findViewById(R.id.tv_value);
                    tvName.setText(sName);
                    tvValue.setText(sValue);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp2px(40f));
                    params.topMargin = dp2px(0.5f);
                    llContainer.addView(cv,params);
                });
            }
            post(()->{
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp2px(80f));
                params.topMargin = dp2px(0.5f);
                llContainer.addView(new View(this),params);
            });
        });

    }

    @OnClick(R.id.tv_logout)
    void onLogoutClicked(){
        DialogManager.showAlertDialog(this,"提示","确定退出登录吗？",null,v1 -> {
            LoginActivity.doLogout();
            finish();
            startActivity(LoginActivity.class);
        });
    }

    @Override
    public void onRefresh(@NonNull RefreshLayout refreshLayout) {
        refreshLayout.setEnableLoadMore(false);
        ApiProvider.requestCommunityInfo(new ApiProvider.IUpdateCommunityInfoListener() {
            @Override
            public void onSuccess() {
                updateView();
                refreshLayout.finishRefresh();
            }

            @Override
            public void onFailure() {
                refreshLayout.finishRefresh(false);
                Toast.makeText(CommunityInfoActivity.this, "数据获取失败！", Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    public void onLoadMore(@NonNull RefreshLayout refreshLayout) {
        refreshLayout.finishLoadMore(0,true,true);
    }
}
