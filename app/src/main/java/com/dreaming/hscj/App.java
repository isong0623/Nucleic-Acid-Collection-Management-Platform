package com.dreaming.hscj;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;

import com.air4.chinesetts.tts.TtsManager;
import com.dreaming.hscj.activity.menu.MenuActivity;
import com.dreaming.hscj.utils.FileUtils;
import com.tencent.bugly.Bugly;
import com.tencent.bugly.crashreport.CrashReport;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.File;
import java.security.Security;
import java.util.LinkedList;
import java.util.List;

public class App extends MultiDexApplication {
    private static final String TAG = App.class.getSimpleName();
    public static final Handler HANDLER = new Handler(Looper.getMainLooper());
    public static App sInstance;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        Bugly.init(getApplicationContext(), "031e4fbe9b", !BuildConfig.DEBUG);

        TtsManager.getInstance().init(this);

        File fDumpDir = new File(getFilesDir().getParentFile().getAbsolutePath(),"/dump/");
        FileUtils.delete(fDumpDir);
        if(!fDumpDir.exists()) fDumpDir.mkdirs();

        FileUtils.delete(getCacheDir());
        
        registerLifecycle();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    private void registerLifecycle(){
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {

            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {  }

            @Override
            public void onActivityStarted(@NonNull Activity activity) { }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                App.this.activity = activity;
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) { }

            @Override
            public void onActivityStopped(@NonNull Activity activity) { }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) { }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) { }
        });
    }

    private Activity activity;
    public Activity getCurrentActivity(){
        return activity;
    }

    public static void Post(Runnable r){
        HANDLER.post(r);
    }

    public static void PostDelayed(Runnable r, long delayMillis) {
        HANDLER.postDelayed(r, delayMillis);
    }


}
