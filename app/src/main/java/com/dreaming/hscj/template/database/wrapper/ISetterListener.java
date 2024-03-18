package com.dreaming.hscj.template.database.wrapper;

public interface ISetterListener {
    void onSuccess();
    void onFailure(String err);
}
