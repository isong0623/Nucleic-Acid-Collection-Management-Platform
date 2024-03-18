package com.dreaming.hscj.activity.community.backup;

import com.dreaming.hscj.base.contract.BasePresenter;
import com.dreaming.hscj.base.contract.BaseView;

public interface ICommunityBackupContract {
    interface View extends BaseView{
        void setDb1Num(int num);
        void setDb2Num(int num);
        void onBackUpFailure(String err);
        void onBackUpSuccess(String path);
    }

    abstract class Presenter extends BasePresenter<View>{
        abstract void backup();
        abstract void queryDb1Num();
        abstract void queryDb2Num();
    }
}
