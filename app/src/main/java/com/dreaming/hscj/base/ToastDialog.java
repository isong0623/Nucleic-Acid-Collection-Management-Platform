
package com.dreaming.hscj.base;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.dreaming.hscj.R;


public class ToastDialog extends Dialog {

    private static ToastDialog customToastDialog = null;
    private Activity activity;
    boolean bCanDismissInBackKey = false;

    public ToastDialog(Context context, int theme, Activity activity) {
        super(context, theme);
        this.activity = activity;
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        try {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if(bCanDismissInBackKey){
                    if (activity != null) {
                        activity.finish();
                        activity = null;
                        dismissDialog();
                    }
                    dismissDialog();
                }
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return super.onKeyDown(keyCode, event);
    }

    private static long lShowTime = System.currentTimeMillis()-1500;

    public static void showTop(Activity activity, String msg) {
        if(msg==null||msg.trim().length()==0) return;
        lShowTime = System.currentTimeMillis();
        createDialog(activity,msg, Gravity.TOP).show();
        new Handler(Looper.getMainLooper()).postDelayed(()->{
            if(System.currentTimeMillis()-lShowTime>1500L) dismissDialog();
        },1600L);
    }

    public static void showCenter(Activity activity, String msg) {
        if(msg==null||msg.trim().length()==0) return;
        lShowTime = System.currentTimeMillis();
        createDialog(activity,msg, Gravity.CENTER).show();
        new Handler(Looper.getMainLooper()).postDelayed(()->{
            if(System.currentTimeMillis()-lShowTime>1500L) dismissDialog();
        },1600L);
    }

    public static void showBottom(Activity activity, String msg) {
        if(msg==null||msg.trim().length()==0) return;
        lShowTime = System.currentTimeMillis();
        createDialog(activity,msg, Gravity.BOTTOM).show();
        new Handler(Looper.getMainLooper()).postDelayed(()->{
            if(System.currentTimeMillis()-lShowTime>1500L) dismissDialog();
        },1600L);
    }

    private static ToastDialog createDialog(final Activity activity, final String msg, int toastGravity) {
        try{if(customToastDialog !=null) customToastDialog.dismiss();} catch (Exception e){}
        try {
            customToastDialog = new ToastDialog(activity, R.style.LoadingDialogStyle, activity);
            customToastDialog.setCancelable(false);
            customToastDialog.setContentView(R.layout.widget_toast);
            ((TextView) customToastDialog.findViewById(R.id.tv_tips)).setText(msg);
            customToastDialog.getWindow().getAttributes().gravity = toastGravity;
            customToastDialog.getWindow().getAttributes().width = -1;
            customToastDialog.getWindow().getAttributes().height = -1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return customToastDialog;
    }

    /**
     * 隐藏dialog
     */
    public static void dismissDialog() {
        try {
            if (customToastDialog != null) {
                customToastDialog.dismiss();
                if(customToastDialog.isShowing()){
                    new Handler(Looper.getMainLooper()).postDelayed(()-> dismissDialog(),333L);
                    return;
                }
                customToastDialog.dismiss();
                customToastDialog = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
