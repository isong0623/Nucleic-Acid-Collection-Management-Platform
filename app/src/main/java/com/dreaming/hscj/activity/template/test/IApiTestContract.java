package com.dreaming.hscj.activity.template.test;

import com.dreaming.hscj.base.contract.BasePresenter;
import com.dreaming.hscj.base.contract.BaseView;

public interface IApiTestContract {
    interface View extends BaseView{

    }

    abstract class Presenter extends BasePresenter<View>{

    }
}
