package com.dreaming.hscj.activity.nucleic_acid.searching;

import com.dreaming.hscj.template.api.ApiProvider;
import com.dreaming.hscj.utils.ToastUtils;

import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

public class NASearchingPresenter extends INASearchingContract.Presenter{

    ESONObject eSearch = new ESONObject();
    @Override
    void update(String key, String value) {
        eSearch.putValue(key,value);
    }
}
