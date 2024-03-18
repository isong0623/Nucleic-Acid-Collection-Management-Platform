package com.dreaming.hscj.utils;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.Display;

import com.dreaming.hscj.App;

/**
 * Created by liutao on 5/6/16.
 */
public class DensityUtils {
    public static int dp2px(Resources resources, float dp) {
        final float scale = resources.getDisplayMetrics().density;
        return  (int) (dp * scale + 0.5f);
    }

    public static int sp2px(Resources resources, float sp){
        final float scale = resources.getDisplayMetrics().scaledDensity;
        return (int) (sp * scale);
    }

    public static float px2dp(float px) {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        return px / ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    public static float dp2px(float dp) {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        return dp * ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    public static int sp2px(float sp) {
        Resources res = Resources.getSystem();
        Configuration config = new Configuration();
        config.setToDefaults();
        res.updateConfiguration(config, res.getDisplayMetrics());
        final float fontScale = res.getDisplayMetrics().scaledDensity;
        return (int) (sp * fontScale + 0.5f);
    }

    public static Point getScreenSize(){
        Display defaultDisplay = App.sInstance.getCurrentActivity().getWindowManager().getDefaultDisplay();
        Point point = new Point();
        defaultDisplay.getSize(point);
        return point;
    }
}
