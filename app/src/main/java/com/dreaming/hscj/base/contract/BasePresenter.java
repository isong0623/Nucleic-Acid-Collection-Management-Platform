package com.dreaming.hscj.base.contract;

import android.app.Activity;
import android.os.Build;
import android.view.View;

import androidx.annotation.RequiresApi;


public abstract class BasePresenter<V> {

    protected V mView;
    public void setView(V v) {
        mView = v;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    boolean isActive(){
        if(mView == null) return false;
        if(mView instanceof Activity && ((Activity)mView).isDestroyed()) return false;
        if(mView instanceof View && ((View)mView).getContext() instanceof Activity && ((Activity)((View)mView).getContext()).isDestroyed()) return false;
        return true;
    }

    public void onDestroy() { }

}