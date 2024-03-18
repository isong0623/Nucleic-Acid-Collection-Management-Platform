package com.dreaming.hscj.activity.nucleic_acid.sampling;

import com.dreaming.hscj.base.contract.BasePresenter;
import com.dreaming.hscj.base.contract.BaseView;
import com.dreaming.hscj.template.database.wrapper.ISetterListener;

import java.util.List;

import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

public interface INASamplingContract {
    interface View extends BaseView{
        void updateView(List<ESONObject> data);
    }

    abstract class Presenter extends BasePresenter<View>{
        abstract void setChangedBarcode(String barcode);
        abstract void setSearchedBarcode(String barcode);

        abstract void requestTestTubeInfo();
        abstract void requestAddPeopleToTestTube(String idCardId, ISetterListener listener);
    }
}
