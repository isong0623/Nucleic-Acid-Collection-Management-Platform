package com.dreaming.hscj.utils;

import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

/**
 * Created by zangpeng on 2018/2/24.
 */

public class RomUtils {
    class AvailableRomType {
        public static final int MIUI = 1;
        public static final int FLYME = 2;
        public static final int ANDROID_NATIVE = 3;
        public static final int NA = 4;
        public static final int UNKNOW = 5;
    }


    private static volatile Integer type = AvailableRomType.UNKNOW;
    public static int getLightStatusBarAvailableRomType() {
        if(type == 5){
            synchronized (type){
                if(type==5){
                    if (isFlymeV4OrAbove()) {
                        return type = AvailableRomType.FLYME;
                    }

                    if (isMiUIV7OrAbove()) {
                        return type = AvailableRomType.ANDROID_NATIVE;
                    }

                    if (isMiUIV6OrAbove()) {
                        return type = AvailableRomType.MIUI;
                    }

                    if (isAndroidMOrAbove()) {
                        return type = AvailableRomType.ANDROID_NATIVE;
                    }
                }
            }
        }
        return type = AvailableRomType.NA;
    }

    //Flyme V4的displayId格式为 [Flyme OS 4.x.x.xA]
    //Flyme V5的displayId格式为 [Flyme 5.x.x.x beta]
    private static boolean isFlymeV4OrAbove() {
        String displayId = Build.DISPLAY;
        if (!TextUtils.isEmpty(displayId) && displayId.contains("Flyme")) {
            String[] displayIdArray = displayId.split(" ");
            for (String temp : displayIdArray) {
                //版本号4以上，形如4.x.
                if (temp.matches("^[4-9]\\.(\\d+\\.)+\\S*")) {
                    return true;
                }
            }
        }
        return false;
    }

    //Android Api 23以上
    private static boolean isAndroidMOrAbove() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    private static final String KEY_MIUI_VERSION_CODE = "ro.miui.ui.version.code";

    private static boolean isMiUIV6OrAbove() {
        try {
            final Properties properties = new Properties();
            properties.load(new FileInputStream(new File(Environment.getRootDirectory(), "build.prop")));
            String uiCode = properties.getProperty(KEY_MIUI_VERSION_CODE, null);
            if (uiCode != null) {
                int code = Integer.parseInt(uiCode);
                return code >= 4;
            } else {
                return false;
            }

        } catch (final Exception e) {
            return false;
        }

    }

    static boolean isMiUIV7OrAbove() {
        try {
            final Properties properties = new Properties();
            properties.load(new FileInputStream(new File(Environment.getRootDirectory(), "build.prop")));
            String uiCode = properties.getProperty(KEY_MIUI_VERSION_CODE, null);
            if (uiCode != null) {
                int code = Integer.parseInt(uiCode);
                return code >= 5;
            } else {
                return false;
            }

        } catch (final Exception e) {
            return false;
        }

    }

}
