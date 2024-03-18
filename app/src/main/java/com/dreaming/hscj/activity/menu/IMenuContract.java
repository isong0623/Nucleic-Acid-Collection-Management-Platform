package com.dreaming.hscj.activity.menu;

import com.dreaming.hscj.base.contract.BasePresenter;
import com.dreaming.hscj.base.contract.BaseView;

public interface IMenuContract {
    interface View extends BaseView{
    }

    abstract class Presenter extends BasePresenter<View>{
        abstract boolean shouldLogin(int type,int position);
        abstract boolean shouldCreateDB(int type,int position);
        abstract Class getJumpingClass(int type,int position);
        abstract void onInitSuccess();
        abstract boolean isInitializing();
    }
}
