package com.dreaming.hscj.template.database.wrapper;

public interface ICountListener {
    void onSuccess(int count);
    void onFailure(String err);
}
