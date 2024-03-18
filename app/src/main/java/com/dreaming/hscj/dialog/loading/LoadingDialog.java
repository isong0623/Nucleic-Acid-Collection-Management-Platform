package com.dreaming.hscj.dialog.loading;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;

import androidx.appcompat.app.AppCompatDialog;

import com.dreaming.hscj.App;
import com.dreaming.hscj.R;

import java.util.HashMap;
import java.util.Map;

import priv.songxusheng.easydialog.EasyDialog;

import static android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND;

public class LoadingDialog extends AppCompatDialog {
    public LoadingDialog(Context context) {
        super(context);
    }

    public LoadingDialog(Context context, int theme) {
        super(context, theme);
    }

    protected LoadingDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_loading);
        setCanceledOnTouchOutside(false);
    }

    private static Map<String,LoadingDialog> mShowedDialog = new HashMap<>();
    public static void showDialog(String dismissTag, Context ctx){
        if(mShowedDialog.containsKey(dismissTag)) return;
        final LoadingDialog dialog = new LoadingDialog(ctx,R.style.BaseDialogStyle);
        mShowedDialog.put(dismissTag,dialog);

        App.Post(()->{
            try {
                dialog.getWindow().clearFlags(FLAG_DIM_BEHIND);
                dialog.setCanceledOnTouchOutside(false);
                dialog.setCancelable(false);
                dialog.setOnKeyListener(new OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                        if(keyCode == 4) return true;
                        return false;
                    }
                });
                dialog.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void dismissDialog(String tag){
        LoadingDialog dialog = mShowedDialog.get(tag);
        if(dialog == null) return;
        App.Post(()->{
            try { dialog.dismiss(); } catch (Exception e) { e.printStackTrace(); }
        });
        mShowedDialog.remove(tag);
    }
}
