package com.dreaming.hscj.template.api.impl;

import com.dreaming.hscj.App;
import com.dreaming.hscj.base.ToastDialog;
import com.dreaming.hscj.utils.ToastUtils;

import priv.songxusheng.easyjson.ESONObject;

public interface ApiListener {
    default void onPerform() {}
    void onSuccess(int code, Object msg, StringBuilder sbLog);
    default void onFailure(int code, Object msg, StringBuilder sbLog){ ToastUtils.show(App.sInstance.getCurrentActivity(),new ESONObject(msg).getJSONValue("message","数据请求失败~")); }
    default void onComplete(){}
}
