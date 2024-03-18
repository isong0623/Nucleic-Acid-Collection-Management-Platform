package com.dreaming.hscj.activity.template.adapt;

import com.dreaming.hscj.base.contract.BasePresenter;
import com.dreaming.hscj.base.contract.BaseView;

public interface ITemplateAdaptContract {
    interface View extends BaseView{

    }

    abstract class Presenter extends BasePresenter<View>{

    }
}
