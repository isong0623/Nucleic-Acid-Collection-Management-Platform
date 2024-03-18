package com.dreaming.hscj.core;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.cardview.widget.CardView;

import com.dreaming.hscj.App;
import com.dreaming.hscj.R;
import com.dreaming.hscj.base.BaseActivity;
import com.dreaming.hscj.base.ToastDialog;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.template.api.ApiTemplate;
import com.dreaming.hscj.utils.EasyPermission;
import com.dreaming.hscj.utils.ToastUtils;
import com.tencent.bugly.proguard.v;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class ViewInjector {

    public static void inject(CardView cv){
        final LinearLayout llRoot = (LinearLayout) cv.getChildAt(0);
        final TextView tv = (TextView) llRoot.getChildAt(0);
        final TextView v  = (TextView) llRoot.getChildAt(1);
        tv.setOnClickListener(v1->{
//            Object o = v.getTag(R.id.action0);
//            long lastClickedTimestamp = o==null? 0 : (long) o;
//            long now = System.currentTimeMillis();
//            if(now - lastClickedTimestamp<200L){
//                tv.setTag(R.id.action0,now);
//                String text = tv.getText().toString();
//                if(text.isEmpty()) return;
//                TTSEngine.speakChinese(text);
//            }
            copyClipboard(v.getText().toString());
        });
    }

    public static void copyClipboard(String text){
        Activity activity = App.sInstance.getCurrentActivity();
        //获取剪贴板管理器：
        ClipboardManager cm = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
        // 创建普通字符型ClipData
        ClipData mClipData = ClipData.newPlainText("Label", text);
        // 将ClipData内容放到系统剪贴板里。
        cm.setPrimaryClip(mClipData);
        ToastDialog.showCenter(activity,"已复制\""+text+"\"到粘贴板！");
    }

    public static void callPhone(BaseActivity activity, String phone){
        if(!activity.hasPermision(Manifest.permission.CALL_PHONE)){
            ToastUtils.show("请授予程序拨打电话权限！");
        }
        activity.requestPermission(Manifest.permission.CALL_PHONE, new EasyPermission.PermissionResultListener() {
            @Override
            public void onPermissionGranted() {
                Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phone));
                activity.startActivity(intent);
            }

            @Override
            public void onPermissionDenied() {
                ToastUtils.show("没有获取到拨打电话权限！");
            }
        });
    }

    public static void injectPhone(BaseActivity activity,CardView cv){
        final LinearLayout llRoot = (LinearLayout) cv.getChildAt(0);
        final TextView v1  = (TextView) llRoot.getChildAt(1);
        View v2 = llRoot.getChildAt(2);
        v2.setOnClickListener(v->callPhone(activity,v1.getText().toString()));
    }

    public static void injectDate(BaseActivity activity, CardView cv, String format){
        EditText etDate =cv.findViewById(R.id.ev_date);
        View vSelectDate = cv.findViewById(R.id.iv_date);

        vSelectDate.setOnClickListener(v1->{
            Calendar c = Calendar.getInstance();

            new DatePickerDialog(activity, new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                    Date date = null;
                    try {
                        date = new SimpleDateFormat("yyyy-MM-dd").parse(String.format("%s-%s-%s",year,month+1,dayOfMonth));
                    } catch (Exception e) {}
                    etDate.setText(new SimpleDateFormat(format).format(date));
                }
            },c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    public static void injectOfflineDate(BaseActivity activity, EditText etDate, ImageView ivBtn){
        final Calendar c = Calendar.getInstance();
        ivBtn.setOnClickListener(v->{
            new DatePickerDialog(activity, new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                    new TimePickerDialog(activity, new TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                            etDate.setText(String.format("%02d月%02d日%02d时",month+1,dayOfMonth,hourOfDay,minute));
                        }
                    },c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE),true).show();
                }
            },c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show();
        });
    }
}
