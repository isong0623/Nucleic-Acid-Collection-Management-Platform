package com.dreaming.hscj.template.api.impl;

public interface ApiListener {
    default void onPerform() {}
    void onSuccess(int code, Object msg, StringBuilder sbLog);
    default void onFailure(int code, Object msg, StringBuilder sbLog){ }
    default void onComplete(){}
}
