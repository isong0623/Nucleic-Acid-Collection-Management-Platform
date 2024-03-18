package com.dreaming.hscj.activity.login;

import com.dreaming.hscj.template.api.ApiProvider;

public class LoginPresenter extends ILoginContract.Presenter{

    private String loginName;
    @Override
    void setUserName(String name) {
        loginName = name;
    }
    @Override
    String getUserName() {
        return loginName;
    }

    private String loginPassword;
    @Override
    void setUserPassword(String pwd) {
        loginPassword = pwd;
    }
    @Override
    String getUserPassword() {
        return loginPassword;
    }
    @Override
    void requestLogin() {
        ApiProvider.requestLogin(loginName, loginPassword, new ApiProvider.ILoginListener() {
            @Override
            public void onLoginSuccess() {
                mView.onLoginSuccess();
            }

            @Override
            public void onLoginFailure() {
                mView.onLoginFailure();
            }
        });
    }
}
