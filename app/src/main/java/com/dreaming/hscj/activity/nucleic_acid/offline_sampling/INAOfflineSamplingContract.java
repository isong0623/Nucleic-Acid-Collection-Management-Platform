package com.dreaming.hscj.activity.nucleic_acid.offline_sampling;

import com.dreaming.hscj.base.contract.BasePresenter;
import com.dreaming.hscj.base.contract.BaseView;

import org.apache.xmlbeans.impl.piccolo.util.DuplicateKeyException;

import java.io.IOException;
import java.util.List;

import priv.songxusheng.easyjson.ESONObject;

public interface INAOfflineSamplingContract {
    interface View extends BaseView {
        void setOfflineFileName(String name);
        void showBarcodeInputViewAndEnableKeyAct();
        void autoShownMatchName(List<ESONObject> lstMatched, List<String> lstShown);
        void autoShownMatchId(List<ESONObject> lstMatched, List<String> lstShown);
    }

    abstract class Presenter extends BasePresenter<View> {
        abstract void write(String tubNo, String idNo, String name, String phone) throws IOException, DuplicateKeyException;
    }
}
