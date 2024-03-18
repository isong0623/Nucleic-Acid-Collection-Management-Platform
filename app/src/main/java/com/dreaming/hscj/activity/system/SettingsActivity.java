package com.dreaming.hscj.activity.system;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.dreaming.hscj.Constants;
import com.dreaming.hscj.R;
import com.dreaming.hscj.base.BaseActivity;
import com.dreaming.hscj.utils.ToastUtils;
import com.google.zxing.BarcodeFormat;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SettingsActivity extends BaseActivity {
    @Override
    public int getContentViewResId() {
        return R.layout.activity_settings;
    }

    @BindView(R.id.sp_display_code_style)
    Spinner spDisplayCodeStyle;

    @Override
    protected void onBindView(View vContent) {
        ButterKnife.bind(this,vContent);
    }

    ArrayAdapter<BarcodeFormat> adapter;
    @Override
    public void initView() {
        setCenterText("软件设置");
        adapter = new ArrayAdapter(this,android.R.layout.simple_spinner_item,BarcodeFormat.values());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDisplayCodeStyle.setAdapter(adapter);
        spDisplayCodeStyle.setSelection(Constants.User.getDefaultCodeDisplayStyle(BarcodeFormat.QR_CODE.ordinal()));
        spDisplayCodeStyle.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (BarcodeFormat.values()[position]){
                    case RSS_EXPANDED:
                    case UPC_EAN_EXTENSION:
                    case MAXICODE:
                    case RSS_14:
                        ToastUtils.show("不支持的编码格式！");
                        spDisplayCodeStyle.setSelection(Constants.User.getDefaultCodeDisplayStyle(BarcodeFormat.QR_CODE.ordinal()));
                        break;
                    default:
                        Constants.User.setkeyOfDefaultCodeDisplayStyle(position%BarcodeFormat.values().length);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }
}
