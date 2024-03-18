package com.dreaming.hscj.activity.community.manage;

import com.dreaming.hscj.base.contract.BasePresenter;
import com.dreaming.hscj.base.contract.BaseView;
import com.dreaming.hscj.template.database.wrapper.IDeleteListener;

import java.util.List;

import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

public interface ICommunityManageContract {
    interface View extends BaseView{
        void updateCount(int count);
        void updateData(int page, List<ESONObject> data);

        void showLoadingDialog();
        void hideLoadingDialog();
    }

    abstract class Presenter extends BasePresenter<View>{
        abstract void queryCount();
        abstract void queryPage(int page);
        abstract void delete(String idCardNo, int index, IDeleteListener listener);
    }
}
