package com.dreaming.hscj.activity.community.input;

import com.dreaming.hscj.base.contract.BasePresenter;
import com.dreaming.hscj.base.contract.BaseView;

public interface ICommunityInputContract {
    interface View extends BaseView{
        void resetView();
        void onDataChanged(String key, String value);
        void showRequestingDialog();
        void hideRequestingDialog();
        void onLoadedDatabase();
        void onLoadedNetwork();
        void saveConfig();
    }

    abstract class Presenter extends BasePresenter<View>{
        abstract void updateDataBlock(String key, String value);
        abstract void requestDatabaseData();
        abstract void requestNetData();
        abstract void saveToDatabase();

        abstract void setLocalDataFirst(boolean bIsFirst);
    }
}
