package com.dreaming.hscj.activity.community.info;

import com.dreaming.hscj.base.contract.BasePresenter;
import com.dreaming.hscj.base.contract.BaseView;
import com.dreaming.hscj.template.api.ApiProvider;

public interface ICommunityInfoContract {
    interface View extends BaseView{
    }

    abstract class Presenter extends BasePresenter<View>{

    }
}
