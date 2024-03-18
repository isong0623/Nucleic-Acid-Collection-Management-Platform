package com.dreaming.hscj.activity.nucleic_acid.exchange;

import android.view.View;

import com.dreaming.hscj.base.contract.BasePresenter;
import com.dreaming.hscj.base.contract.BaseView;

import java.util.List;

import priv.songxusheng.easyjson.ESONObject;

public interface INAExchangeContract {
    interface View extends BaseView{
        void onTransferSuccess(String path);
        void onTransferFailure();
        void showUnmatchedDialog(List<ESONObject> log, android.view.View.OnClickListener onAbort, android.view.View.OnClickListener onContinue);

        void onMessage(String message);
        void onProgress(int progress);

        void onTransferReadyUpdate(int count);
        void onTransferEndUpdate(int count);
    }

    abstract class Presenter extends BasePresenter<View>{
        abstract void transfer(String srcNo, String tarNo);
        abstract void transfer(String srcNo, String tarNo, String startTime, String endTime);

        abstract List<ESONObject> getTransferReadyRecodes();
        abstract List<ESONObject> getTransferEndRecodes();
    }
}
