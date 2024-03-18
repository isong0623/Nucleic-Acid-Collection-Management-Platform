package com.dreaming.hscj.activity.nucleic_acid.searching;

import com.dreaming.hscj.base.contract.BasePresenter;
import com.dreaming.hscj.base.contract.BaseView;

public interface INASearchingContract {
    interface View extends BaseView{
    }

    abstract class Presenter extends BasePresenter<View>{
        abstract void update(String key,String value);
    }
}
