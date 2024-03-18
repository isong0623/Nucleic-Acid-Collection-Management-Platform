package com.dreaming.hscj.activity.community.search;

import android.graphics.Color;
import android.graphics.Point;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.dreaming.hscj.R;
import com.dreaming.hscj.activity.community.input.CommunityInputActivity;
import com.dreaming.hscj.base.BaseActivity;
import com.dreaming.hscj.core.BundleBuilder;
import com.dreaming.hscj.utils.ZxingUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class CommunitySearchShownQRActivity extends BaseActivity {

    public static void shownQR(BaseActivity activity, String type, String id){
        activity.startActivity(CommunitySearchShownQRActivity.class, BundleBuilder.create().put("type",type).put("id",id).build());
    }

    @Override
    public int getContentViewResId() {
        return R.layout.activity_community_search_shown_qr;
    }

    @Override
    protected void onBindView(View vContent) {
        ButterKnife.bind(this,vContent);
    }

    @BindView(R.id.iv_qr)
    ImageView ivQR;

    @BindView(R.id.tv_shown)
    TextView tvShown;

    String id;
    String type;
    @Override
    public void initView() {
        setCenterText("身份号识别");

        id = getIntent().getStringExtra("id");
        if(id == null) id = "";
        type = getIntent().getStringExtra("type");
        if(type == null) type = "";

        Point p = new Point();
        getWindowManager().getDefaultDisplay().getSize(p);
        ivQR.setImageBitmap(ZxingUtils.autoCreateCodeBitmap(id,p.x,p.x,"utf-8","","", Color.BLACK, Color.WHITE));
        tvShown.setText(type+":"+id);

        ivQR.setVisibility(View.VISIBLE);
        tvShown.setVisibility(View.VISIBLE);
    }

    @OnClick(R.id.tv_save)
    void onSaveClicked(){
        CommunityInputActivity.doInputOneWithId(this,id);
    }
}
