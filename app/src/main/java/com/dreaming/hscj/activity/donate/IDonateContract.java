package com.dreaming.hscj.activity.donate;

import com.dreaming.hscj.base.contract.BasePresenter;
import com.dreaming.hscj.base.contract.BaseView;

public interface IDonateContract {
    interface View extends BaseView {
        void jumpingToWX4Donate();
    }

    abstract class Presenter extends BasePresenter<View> {
        abstract void createDonateByWxPayImage();
    }
}
