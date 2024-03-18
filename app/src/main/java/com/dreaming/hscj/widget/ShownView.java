package com.dreaming.hscj.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import com.dreaming.hscj.R;
import com.dreaming.hscj.utils.DensityUtils;

public class ShownView extends CardView {
    public ShownView(@NonNull Context context) {
        this(context,null);
    }

    public ShownView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public ShownView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public TextView tvName;
    public TextView tvValue;
    private void init(Context context, AttributeSet attrs){
        CardView v = (CardView) LayoutInflater.from(getContext()).inflate(R.layout.view_shown,null,false);
        LinearLayout ll = (LinearLayout) v.getChildAt(0);
        v.removeAllViews();
        addView(ll,ll.getLayoutParams());

        tvName  = findViewById(R.id.tv_name);
        tvValue = findViewById(R.id.tv_value);

        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ShownView);
        String nText = ta.getString(R.styleable.ShownView_nText);

        float nSize  = ta.getDimensionPixelSize(R.styleable.ShownView_nTextSize, DensityUtils.sp2px(14));
        int nColor   = ta.getColor(R.styleable.ShownView_nTextColor,Color.parseColor("#616161"));
        tvName.setText(nText == null ? "" : nText);
        tvName.setTextColor(nColor);
        tvName.setTextSize(TypedValue.COMPLEX_UNIT_PX,nSize);

        String vText = ta.getString(R.styleable.ShownView_vText);
        float vSize  = ta.getDimensionPixelSize(R.styleable.ShownView_vTextSize, DensityUtils.sp2px(14));
        int vColor   = ta.getColor(R.styleable.ShownView_vTextColor,Color.parseColor("#616161"));
        tvValue.setText(vText == null ? "" : vText);
        tvValue.setTextColor(vColor);
        tvValue.setTextSize(TypedValue.COMPLEX_UNIT_PX,vSize);

        ta.recycle();
    }
}
