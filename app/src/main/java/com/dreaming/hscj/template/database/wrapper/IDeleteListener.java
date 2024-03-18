package com.dreaming.hscj.template.database.wrapper;

public interface IDeleteListener {
    void onSuccess(int count);
    void onFailure(String err);
}
