package com.dreaming.hscj.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;

import com.dreaming.hscj.R;
import com.dreaming.hscj.utils.DensityUtils;


/**
 * author : HJQ
 * github : https://github.com/getActivity/AndroidProject
 * time   : 2018/10/18
 * desc   : 带清除按钮的EditText
 */
@SuppressLint("AppCompatCustomView")
public class ClearEditView extends EditText
        implements View.OnTouchListener, View.OnFocusChangeListener, TextWatcher {

    private Drawable mClearIcon;

    private OnTouchListener mOnTouchListener;
    private OnFocusChangeListener mOnFocusChangeListener;

    public ClearEditView(Context context) {
        super(context);
        initialize(context);
    }

    public ClearEditView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public ClearEditView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    boolean bIsInit = false;
    private void initialize(final Context context) {

        if(bIsInit) return;
        bIsInit = true;

        final Drawable drawable = ContextCompat.getDrawable(context, R.drawable.widget_input_del);

        final Drawable wrappedDrawable = DrawableCompat.wrap(drawable); //Wrap the drawable so that it can be tinted pre Lollipop
        //DrawableCompat.setTint(wrappedDrawable, getCurrentHintTextColor());
        mClearIcon = wrappedDrawable;
        mClearIcon.setBounds(0, 0, (int) DensityUtils.dp2px(22),  (int) DensityUtils.dp2px(22));
        setClearIconVisible(false);
        super.setOnTouchListener(this);
        super.setOnFocusChangeListener(this);
        super.addTextChangedListener(this);
        ViewCompat.setBackgroundTintList(this, ContextCompat.getColorStateList(context, R.color.light_black));
    }

    private void setClearIconVisible(final boolean visible) {
        if (mClearIcon.isVisible() == visible) return;

        mClearIcon.setVisible(visible, false);

        final Drawable[] compoundDrawables = getCompoundDrawables();
        setCompoundDrawables(
                compoundDrawables[0],
                compoundDrawables[1],
                visible ? mClearIcon : null,
                compoundDrawables[3]);
    }

    @Override
    public void setOnFocusChangeListener(final OnFocusChangeListener onFocusChangeListener) {
        mOnFocusChangeListener = onFocusChangeListener;
    }

    /**
     * {@link OnFocusChangeListener}
     */

    @Override
    public void onFocusChange(final View view, final boolean hasFocus) {
        Log.e("EditText","onFocusChange");
        if (hasFocus && getText() != null) {
            setClearIconVisible(getText().length() > 0);
        } else {
            setClearIconVisible(false);
        }
        if (mOnFocusChangeListener != null) {
            mOnFocusChangeListener.onFocusChange(view, hasFocus);
        }
    }

    /**
     * {@link OnTouchListener}
     */

    @Override
    public boolean onTouch(final View view, final MotionEvent motionEvent) {
        Log.e("EditText","onTouch"+mOnTouchListener);
        final int x = (int) motionEvent.getX();
        if (mClearIcon.isVisible() && x > getWidth() - getPaddingRight() - mClearIcon.getIntrinsicWidth()) {
            if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                setText("");
            }
            return true;
        }
        return mOnTouchListener != null && mOnTouchListener.onTouch(view, motionEvent);
    }

    /**
     * {@link TextWatcher}
     */

    @Override
    public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
        if (isFocused()) {
            setClearIconVisible(s.length() > 0);
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void afterTextChanged(Editable s) {
    }
}