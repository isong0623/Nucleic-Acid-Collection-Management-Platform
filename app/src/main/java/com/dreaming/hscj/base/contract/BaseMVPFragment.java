package com.dreaming.hscj.base.contract;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;

import com.dreaming.hscj.base.BaseFragment;
import com.dreaming.hscj.utils.ReflectUtils;

public abstract class BaseMVPFragment<P extends BasePresenter> extends BaseFragment {

    protected P mPresenter;
    protected boolean shouldInitPresenter(){
        return true;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(shouldInitPresenter()){
            mPresenter = ReflectUtils.getParameterizeTypeInstance(this, 0);
            if (this instanceof BaseView)
                mPresenter.setView(this);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mPresenter != null) {
            mPresenter.onDestroy();
            //mPresenter = null;
        }
    }

    protected int getLayoutId(){return View.NO_ID;}
}
