package com.dreaming.hscj.activity.login;

import com.dreaming.hscj.base.contract.BasePresenter;
import com.dreaming.hscj.base.contract.BaseView;

public interface ILoginContract {
    interface View extends BaseView{
        void onLoginSuccess();
        void onLoginFailure();
    }

    abstract class Presenter extends BasePresenter<View>{
        abstract void setUserName(String name);
        abstract String getUserName();
        abstract void setUserPassword(String pwd);
        abstract String getUserPassword();
        abstract void requestLogin();
    }
}
