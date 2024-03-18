package com.dreaming.hscj.activity.community.search;

import com.dreaming.hscj.base.contract.BasePresenter;
import com.dreaming.hscj.base.contract.BaseView;

public interface ICommunitySearchContract {
    interface View extends BaseView{
    }

    abstract class Presenter extends BasePresenter<View>{
        abstract void updateQueryMode(int mode);
        abstract void updateQueryGroup(int group);
        abstract void updateQueryFields(String key,String value);
        abstract void query();
    }
}
