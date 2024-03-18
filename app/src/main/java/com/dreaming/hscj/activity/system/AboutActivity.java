package com.dreaming.hscj.activity.system;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dreaming.hscj.R;
import com.dreaming.hscj.base.BaseActivity;
import com.dreaming.hscj.base.ToastDialog;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.template.database.DatabaseTemplate;
import com.dreaming.hscj.utils.ToastUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class AboutActivity extends BaseActivity {

    @Override
    protected void onBindView(View vContent) {
        ButterKnife.bind(this,vContent);
    }

    @Override
    public int getContentViewResId() {
        return R.layout.activity_about;
    }

    @OnClick({R.id.tv_tech_support,R.id.tv_copyright})
    void jumpingWeibo(){
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("sinaweibo://userinfo?uid=7711875625"));
        Intent chooseIntent = Intent.createChooser(intent, "Weibo");
        startActivity(chooseIntent);
        ToastDialog.showCenter(this,"若跳转失败，请安装最新版微博客户端！");
    }

    @BindView(R.id.ll_third_party_info)
    LinearLayout llThirdPartyInfo;
    @BindView(R.id.tv_api_provider)
    TextView tvApiProvider;

    @BindView(R.id.ll_more_info)
    LinearLayout llMoreInfo;

    @BindView(R.id.tv_app_version)
    TextView tvAppVersion;

    public String getVersionName(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(
                    context.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void initView() {
        setCenterText("关于我们");
        setRightText("使用说明");
        tvTitleRight.setOnClickListener(v -> startActivity(HelpActivity.class));
        tvAppVersion.setText("版本号：V"+ getVersionName(this));
        tvApiProvider.setText(Template.getCurrentTemplate().getDatabaseSetting().getNetApiProvider());
        List<String> lstMoreInfo = Template.getCurrentTemplate().getDatabaseSetting().getNetApiIntroduce();
        for(int i=0,ni=lstMoreInfo.size();i<ni;++i){
            TextView tv = new TextView(this);
            tv.setText(lstMoreInfo.get(i));
            tv.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp2px(20f));
            params.gravity = Gravity.CENTER;
            llMoreInfo.addView(tv,params);
        }
    }

    @OnClick(R.id.tv_ocr_provider)
    void jumpingBaiduPaddle(){
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paddlepaddle.org.cn/")));
        } catch (Exception e) {
            Toast.makeText(this, "网页打开失败！\n\nhttps://www.paddlepaddle.org.cn/", Toast.LENGTH_LONG).show();
        }
    }
}
