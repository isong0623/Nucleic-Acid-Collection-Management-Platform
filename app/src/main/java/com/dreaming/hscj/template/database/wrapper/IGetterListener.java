package com.dreaming.hscj.template.database.wrapper;

import priv.songxusheng.easyjson.ESONArray;

public interface IGetterListener {
    void onSuccess(ESONArray data);
    void onFailure(String err);
}
