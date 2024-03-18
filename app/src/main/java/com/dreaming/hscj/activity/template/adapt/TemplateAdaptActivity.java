package com.dreaming.hscj.activity.template.adapt;

import android.content.Intent;
import android.net.Uri;
import android.view.View;

import com.dreaming.hscj.R;
import com.dreaming.hscj.activity.template.TemplateManageActivity;
import com.dreaming.hscj.base.BaseActivity;
import com.dreaming.hscj.base.ToastDialog;
import com.dreaming.hscj.base.contract.BaseMVPActivity;
import com.dreaming.hscj.core.ViewInjector;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class TemplateAdaptActivity extends BaseMVPActivity<TemplateAdaptPresenter> implements ITemplateAdaptContract.View{
    public interface IAdaptCallback{
        void onAdaptSuccess();
        void onAdaptCancel();
    }
    public static void doAdapt(BaseActivity activity,IAdaptCallback callback){
        activity.startActivityForResult(TemplateAdaptActivity.class, 5001, new OnActivityResultItemCallBack() {
            @Override
            public void OnActivityRequestResult(int resultCode, Intent data) {
                if(resultCode == RESULT_OK){
                    callback.onAdaptSuccess();
                }
                else{
                    callback.onAdaptCancel();
                }
            }
        });
    }

    @Override
    public int getContentViewResId() {
        return R.layout.activity_template_adapt;
    }

    @Override
    protected void onBindView(View vContent) {
        ButterKnife.bind(this,vContent);
    }

    @Override
    public void initView() {
        setCenterText("APP模板适配向导");
    }

    @OnClick(R.id.tv_jump_manage)
    void onJumpingManage(){
        startActivityForResult(TemplateManageActivity.class, 5000, new OnActivityResultItemCallBack() {
            @Override
            public void OnActivityRequestResult(int resultCode, Intent data) {
                if(resultCode == RESULT_OK){
                    setResult(RESULT_OK);
                }
            }
        });
    }

    @OnClick(R.id.tv_jump_plan)
    void onJumpingPlan(){
        startActivity(TemplateAdaptPlanActivity.class);
    }

}
