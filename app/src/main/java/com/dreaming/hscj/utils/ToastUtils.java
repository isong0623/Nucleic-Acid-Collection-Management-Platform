package com.dreaming.hscj.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.dreaming.hscj.App;
import com.dreaming.hscj.R;


public class ToastUtils {

    public static void show(String msg) {
        show(App.sInstance, msg, Toast.LENGTH_SHORT);
    }

    public static void show(String msg, int duration) {
        show(App.sInstance, msg, duration);
    }

    public static void show(Context context, String msg) {
        show(context, msg, Toast.LENGTH_SHORT);
    }

    public static void show(Context context, String msg, int duration) {
        Toast.makeText(context, msg, duration).show();
    }

    public static void show(Context context, String msg, int duration, int gravity) {
        Toast toast = Toast.makeText(context, msg, duration);
        toast.setGravity(gravity, 0, 0);
        toast.show();
    }

    public static void showCustom(Context context, String msg, int gravity) {
        Toast toast = new Toast(context);
        View view = LayoutInflater.from(context).inflate(R.layout.widget_toast, null);
        TextView tvMsg = view.findViewById(R.id.tv_tips);
        tvMsg.setText(msg);
        toast.setView(view);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setGravity(gravity, 0, 0);
        toast.show();
    }
}
