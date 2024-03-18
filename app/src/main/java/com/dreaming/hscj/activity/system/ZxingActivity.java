package com.dreaming.hscj.activity.system;

import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.dreaming.hscj.Constants;
import com.dreaming.hscj.R;
import com.dreaming.hscj.base.BaseActivity;
import com.dreaming.hscj.utils.ToastUtils;
import com.google.zxing.DecodeHintType;
import com.google.zxing.client.android.Intents;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.BarcodeView;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.Decoder;
import com.journeyapps.barcodescanner.DecoderResultPointCallback;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.DefaultDecoderFactory;

import static com.google.zxing.integration.android.IntentIntegrator.REQUEST_CODE;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ZxingActivity extends BaseActivity {
    private static final String tag = "Zxing";

    private CaptureManager capture;
    private DecoratedBarcodeView bv_barcode;

    public interface IScanListener{
        void onSuccess(String code);
        void onFailure();
    }

    public static void Scan(BaseActivity activity, IScanListener listener){
        activity.startActivityForResult(ZxingActivity.class, REQUEST_CODE, (resultCode, data) -> {
            IntentResult result = IntentIntegrator.parseActivityResult(REQUEST_CODE, resultCode, data);
            if (result != null && result.getContents()!=null) {
                String strCode = result.getContents();
                Log.e(tag,"扫描结果："+strCode);
                try { listener.onSuccess(strCode); } catch (Exception e) { e.printStackTrace(); }
            }
            else{
                Log.e(tag,"扫描结果：空");
                try { listener.onFailure(); } catch (Exception e) { e.printStackTrace(); }
            }
        });
    }

    @Override
    public boolean hasTitleBar() {
        return false;
    }

    @Override
    public int getContentViewResId() {
        return R.layout.zxing;
    }

    BarcodeView barcodeView;
    @Override
    protected void onBindView(View vContent) {
        ButterKnife.bind(this,vContent);
        bv_barcode = (DecoratedBarcodeView) findViewById(R.id.bv_barcode);
        capture = new CaptureManager(this, bv_barcode);

        Intent intent = new Intent(Intents.Scan.ACTION);
        intent.putExtra(Intents.Scan.CAMERA_ID,Constants.Config.getZxingCameraIndex());
        barcodeView = bv_barcode.findViewById(com.google.zxing.client.android.R.id.zxing_barcode_surface);
        capture.initializeFromIntent(intent, null);
        capture.decode();
        capture.onResume();
    }

    int getNumberOfCameras(){
        try {
            int num = Camera.getNumberOfCameras();
            if(num>0) return num;
        } catch (Exception e) {}

        int count = 0;
        for (int i = 0; i < 10; i++) {
            String dev = "/dev/video" + i;
            File file = new File(dev);
            if (file.exists()) count++;
        }
        return Math.max(1,count);
    }

    @OnClick(R.id.iv_switch_camera)
    void onSwitchCameraClicked(){
        int cameraCount = getNumberOfCameras();
        int currentCamera = Constants.Config.getZxingCameraIndex();
        barcodeView.getCameraSettings().setRequestedCameraId((currentCamera+1)%cameraCount);
        Constants.Config.setZxingCameraIndex((currentCamera+1)%cameraCount);
        barcodeView.pause();
        barcodeView.resume();
        capture.onResume();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(capture!=null) capture.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(capture!=null) capture.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(capture!=null) capture.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(capture!=null) capture.onSaveInstanceState(outState);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if(capture!=null) capture.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return bv_barcode!=null&&bv_barcode.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }
}
