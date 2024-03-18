package com.dreaming.hscj.activity.donate;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.dreaming.hscj.R;
import com.dreaming.hscj.base.ToastDialog;
import com.dreaming.hscj.base.contract.BaseMVPActivity;
import com.dreaming.hscj.utils.EasyPermission;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class DonateActivity extends BaseMVPActivity<DonatePresenter> implements IDonateContract.View{

    @BindView(R.id.tv_zfb)
    TextView tvZfb;
    @BindView(R.id.tv_wechat)
    TextView tvWechat;
    @BindView(R.id.img_zfb)
    ImageView imgZfb;
    @BindView(R.id.img_wechat)
    ImageView imgWechat;
    @BindView(R.id.tv_donate)
    TextView tvDonate;

    @Override
    public int getContentViewResId() {
        return R.layout.activity_donate;
    }

    @Override
    protected void onBindView(View vContent) {
        super.onBindView(vContent);
        ButterKnife.bind(this,vContentView);
    }

    @Override
    public void initView() {
        super.initView();
        setCenterText("赞助");
    }

    @OnClick(R.id.tv_zfb)
    void onSwitchToZFBClicked(){
        tvZfb.setEnabled(false);
        tvWechat.setEnabled(true);
        imgZfb.setVisibility(View.VISIBLE);
        imgWechat.setVisibility(View.GONE);
        updateBtn();
    }

    @OnClick(R.id.tv_wechat)
    void onSwitchToWeChatClicked(){
        tvZfb.setEnabled(true);
        tvWechat.setEnabled(false);
        imgWechat.setVisibility(View.VISIBLE);
        imgZfb.setVisibility(View.GONE);
        updateBtn();
        ToastDialog.showCenter(this,"请先完成截屏，再点击跳转按钮~");
    }

    @OnClick(R.id.tv_donate)
    void onDonateClicked(){
        if(tvWechat.isEnabled()){
            donateByZhiFuBao(this);
        }
        if(tvZfb.isEnabled()){
            donateByWeChat();
        }
    }

    void updateBtn(){
        tvDonate.setText(!tvZfb.isEnabled()?"跳转到支付宝开始赞助":"截屏后跳转微信扫描图片赞助");
    }

    //跳转到支付宝付款界面
    private void donateByZhiFuBao(Context context) {
        String intentFullUrl = "intent://platformapi/startapp?saId=10000007&" +
                "qrcode=https://qr.alipay.com//fkx13576fbbct1s1e3dvs5d#Intent;" +
                "scheme=alipayqr;package=com.eg.android.AlipayGphone;end";
        try {
            Intent intent = Intent.parseUri(intentFullUrl, Intent.URI_INTENT_SCHEME);
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void jumpingToWX4Donate() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.tencent.mm", "com.tencent.mm.ui.LauncherUI"));
        intent.putExtra("LauncherUI.From.Scaner.Shortcut", true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//335544320
        intent.setAction("android.intent.action.VIEW");
        mHandler.postDelayed(()->{
            try { startActivity(intent); } catch (Exception e) { }
        },1000);
    }

    //唤醒微信扫描
    private void donateByWeChat() {
        requestPermission(EasyPermission.WRITE_EXTERNAL_STORAGE, new EasyPermission.PermissionResultListener() {
            @Override
            public void onPermissionGranted() {
                jumpingToWX4Donate();
            }

            @Override
            public void onPermissionDenied() {
                ToastDialog.showCenter(DonateActivity.this,"无法获取读写外置存储器权限，图片保存失败，赞助中断！");
            }
        });

    }
}
