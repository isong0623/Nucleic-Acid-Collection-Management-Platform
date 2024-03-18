package com.dreaming.hscj.activity.community.manage;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.dreaming.hscj.R;
import com.dreaming.hscj.base.BaseActivity;
import com.dreaming.hscj.core.BundleBuilder;
import com.dreaming.hscj.utils.ToastUtils;
import com.dreaming.hscj.utils.ZxingUtils;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CommunityMemberIDQRShownActivity extends BaseActivity {

    public static void showQR(BaseActivity activity, String id, String name){
        activity.startActivityForResult(
                CommunityMemberIDQRShownActivity.class,
                BundleBuilder
                        .create()
                        .put("id", id)
                        .put("name", name)
                        .build(),
                1024,
                new OnActivityResultItemCallBack() {
                    @Override
                    public void OnActivityRequestResult(int resultCode, Intent data) {
                        activity.finish();
                    }
                }
        );
    }

    @Override
    public int getContentViewResId() {
        return R.layout.activity_community_member_id_qr_shown;
    }

    @Override
    protected void onBindView(View vContent) {
        ButterKnife.bind(this,vContent);
    }

    @BindView(R.id.iv_qr)
    ImageView ivQR;

    @BindView(R.id.tv_shown)
    TextView tvShown;

    @Override
    public void initView() {
        setCenterText("身份号二维码展示");

        String id   = getIntent().getStringExtra("id");
        if(id == null) id = "";
        String name = getIntent().getStringExtra("name");
        if(name == null) name = "";
        Point p = new Point();
        getWindowManager().getDefaultDisplay().getSize(p);
        ivQR.setImageBitmap(ZxingUtils.autoCreateCodeBitmap(id,p.x,p.x,"utf-8","","", Color.BLACK, Color.WHITE));
        tvShown.setText(id+":"+name);
        ToastUtils.show("10秒后自动关闭此二维码页面！");
        mHandler.postDelayed(() -> {
            if(isDestroyed()) return;
            ToastUtils.show("5秒后自动关闭此二维码页面！");
        }, 5000L);
        mHandler.postDelayed(() -> {
            if(isDestroyed()) return;
            finish();
        }, 10000L);
    }
}
