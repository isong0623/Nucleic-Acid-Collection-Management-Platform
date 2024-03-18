package com.dreaming.hscj.activity.community.search;

import priv.songxusheng.easyjson.ESONObject;

public class CommunitySearchPresenter extends ICommunitySearchContract.Presenter{


    int mode;
    @Override
    void updateQueryMode(int mode) {
        this.mode = mode;
    }

    int group;
    @Override
    void updateQueryGroup(int group) {
        this.group = group;
    }

    void resetQueryFields(){
        eQueryFields = new ESONObject();
    }
    ESONObject eQueryFields = new ESONObject();
    @Override
    void updateQueryFields(String key, String value) {
        if(value == null){
            eQueryFields.remove(key);
            return;
        }
        eQueryFields.putValue(key,value);
    }

    @Override
    void query() {

    }

}
