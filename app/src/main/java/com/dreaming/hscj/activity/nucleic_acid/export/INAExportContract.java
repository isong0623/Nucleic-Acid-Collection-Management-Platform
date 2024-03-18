package com.dreaming.hscj.activity.nucleic_acid.export;

import com.dreaming.hscj.base.contract.BasePresenter;
import com.dreaming.hscj.base.contract.BaseView;

import java.util.List;

import priv.songxusheng.easyjson.ESONObject;

public interface INAExportContract {
    interface View extends BaseView {
        void onMessage(String message);
        void onProgress(int progress);

        void showEmptyDialog();
        void showSuccessDialog(List<String> paths);

        void onLocalCheckedUpdate(int count);
        void onLocalUncheckUpdate(int count);
        void onNetCheckedUpdate(int count);
    }

    abstract class Presenter extends BasePresenter<View> {
        abstract void export(String startTime,String endTime);
        abstract List<ESONObject> getLocalCheckedData();
        abstract List<ESONObject> getLocalUnCheckData();
        abstract List<ESONObject> getNetCheckedData();
    }
}
