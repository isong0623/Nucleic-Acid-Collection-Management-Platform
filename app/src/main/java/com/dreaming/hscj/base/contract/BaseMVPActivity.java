package com.dreaming.hscj.base.contract;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;

import androidx.annotation.Nullable;

import com.dreaming.hscj.App;
import com.dreaming.hscj.activity.system.ZxingActivity;
import com.dreaming.hscj.base.BaseActivity;
import com.dreaming.hscj.core.ocr.IDCardRecognizer;
import com.dreaming.hscj.utils.ReflectUtils;


public abstract class BaseMVPActivity<P extends BasePresenter> extends BaseActivity {

    protected P mPresenter;
    protected boolean shouldInitPresenter(){
        return true;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(shouldInitPresenter()){
            mPresenter = ReflectUtils.getParameterizeTypeInstance(this, 0);
            if (this instanceof BaseView)
                mPresenter.setView(this);
        }
    }

    protected boolean enableVolumeUpScanBarcode(){ return false; }
    protected void onScanBarcodeSuccess(String barcode){}
    protected void onScanBarcodeFailure(){}
    protected void scanBarcode(){
        ZxingActivity.Scan(this, new ZxingActivity.IScanListener() {
            @Override
            public void onSuccess(String code) {
                onScanBarcodeSuccess(code);
            }

            @Override
            public void onFailure() {
                onScanBarcodeFailure();
            }
        });
    }

    protected boolean enableVolumeDownRecognizeIDCard(){ return false ;}
    protected void onRecognizeCardSuccess(String idCard, String type){}
    protected void onRecognizeCardFailure(){}
    protected void recognizeIDCard(){
        IDCardRecognizer.recognize(this, new IDCardRecognizer.IDCardRecognizeListener() {
            @Override
            public void onSuccess(String id, String type) {
                onRecognizeCardSuccess(id,type);
            }

            @Override
            public void onFailure() {
                onRecognizeCardFailure();
            }
        });
    }

    protected boolean onKeyEnterDown(){
        return false;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if(event!=null&&event.getAction()==MotionEvent.ACTION_DOWN){
            if(event.getKeyCode()==KeyEvent.KEYCODE_VOLUME_UP) {
                if(enableVolumeUpScanBarcode()){
                    scanBarcode();
                    return true;
                }
            }
            if(event.getKeyCode()==KeyEvent.KEYCODE_VOLUME_DOWN) {
                if(enableVolumeDownRecognizeIDCard()){
                    recognizeIDCard();
                    return true;
                }
            }
            if(event.getKeyCode()==KeyEvent.KEYCODE_ENTER){
                if(onKeyEnterDown()){
                    return true;
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }

    protected void post(Runnable r){
        App.Post(r);
    }

    protected void postDelayed(Runnable r, long delayMillis) {
        App.PostDelayed(r, delayMillis);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPresenter != null) {
            mPresenter.onDestroy();
//            mPresenter = null;
        }
    }
}