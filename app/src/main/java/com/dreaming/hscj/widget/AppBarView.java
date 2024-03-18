package com.dreaming.hscj.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dreaming.hscj.R;

public class AppBarView extends FrameLayout {
    public AppBarView(@NonNull Context context) {
        super(context);
    }

    public AppBarView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public AppBarView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }
    @TargetApi(21)
    public AppBarView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    TextView tv;
    ImageView img;
    int resSelected,resUnselect;
    private void init(@Nullable AttributeSet attrs){
        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.AppBarView);
        LayoutInflater.from(getContext()).inflate(R.layout.activity_main_app_bar_item,this);
        tv = findViewById(R.id.tv);
        img = findViewById(R.id.img);
        String text = typedArray.getString(R.styleable.AppBarView_texts);
        tv.setText(text==null?"":text);
        resUnselect = typedArray.getResourceId(R.styleable.AppBarView_iv_unselect,-1);
        resSelected = typedArray.getResourceId(R.styleable.AppBarView_iv_selected,-1);
        isSelected = typedArray.getBoolean(R.styleable.AppBarView_is_select,false);
        img.setImageResource(isSelected?resSelected:resUnselect);
        tv.setTextColor(Color.parseColor(isSelected?"#2962ff":"#ffffff"));
        typedArray.recycle();
    }

    boolean isSelected;
    public void setSelected(boolean isSelected){
        if (this.isSelected==isSelected) return;
        this.isSelected=isSelected;
        tv.setTextColor(Color.parseColor(isSelected?"#2962ff":"#ffffff"));
        img.setImageResource(isSelected?resSelected:resUnselect);
//        if(isSelected) img.startAnimation(AnimationUtils.loadAnimation(getContext(),R.anim.scale));
    }
}
