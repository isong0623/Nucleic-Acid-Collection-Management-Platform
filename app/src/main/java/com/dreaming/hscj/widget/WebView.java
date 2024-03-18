package com.dreaming.hscj.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.dreaming.hscj.App;

import java.util.Map;

public class WebView extends com.tencent.smtt.sdk.WebView {
    public WebView(Context context, boolean b) {
        super(context, b);
    }

    public WebView(Context context) {
        super(context);
    }

    public WebView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public WebView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public WebView(Context context, AttributeSet attributeSet, int i, boolean b) {
        super(context, attributeSet, i, b);
    }

    public WebView(Context context, AttributeSet attributeSet, int i, Map<String, Object> map, boolean b) {
        super(context, attributeSet, i, map, b);
    }

    public interface OnScrollStateChangedListener{
        void onMoving();
        void onStop();
    }
    private OnScrollStateChangedListener onScrollStateChangedListener;
    public void setOnScrollStateChangedListener(OnScrollStateChangedListener onScrollStateChangedListener){
        this.onScrollStateChangedListener = onScrollStateChangedListener;
    }

    long lLastChangeTime = 0L;
    boolean bIsMoving = false;
    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        if(onScrollStateChangedListener != null){
            if(!bIsMoving){
                onScrollStateChangedListener.onMoving();
            }
            bIsMoving = true;
            lLastChangeTime = System.currentTimeMillis();
            App.PostDelayed(
                    ()->{
                        long now = System.currentTimeMillis();
                        if(now - lLastChangeTime < 1000L) return;
                        bIsMoving = false;
                        onScrollStateChangedListener.onStop();
                    },
                    1000
            );
        }
        super.onScrollChanged(l, t, oldl, oldt);
    }
}
