package com.dreaming.hscj.activity.community.manage;

import com.dreaming.hscj.App;
import com.dreaming.hscj.core.ThreadPoolProvider;
import com.dreaming.hscj.template.DataParser;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.template.database.DatabaseTemplate;
import com.dreaming.hscj.template.database.wrapper.DatabaseConfig;
import com.dreaming.hscj.template.database.wrapper.ICountListener;
import com.dreaming.hscj.template.database.wrapper.IDeleteListener;
import com.dreaming.hscj.utils.ToastUtils;

import java.util.ArrayList;
import java.util.List;

import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

public class CommunityManagePresenter extends ICommunityManageContract.Presenter{
    @Override
    void queryCount() {
        Template.getCurrentTemplate().getUserOverallDatabase().count(new ICountListener() {
            @Override
            public void onSuccess(int count) {
                mView.updateCount(count);
            }

            @Override
            public void onFailure(String err) {}
        });
    }

    @Override
    void queryPage(int page) {
        mView.showLoadingDialog();
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            try {
                ESONArray result = Template.getCurrentTemplate().getUserOverallDatabase().query(new ArrayList<>(),new ArrayList<>(),page,100);
                final List<ESONObject> lst = new ArrayList<>();
                for(int i=0,ni=result.length();i<ni;++i){
                    lst.add(DataParser.parseDatabaseToShown(DatabaseConfig.TYPE_USER_OVERALL,result.getArrayValue(i,new ESONObject())));
                }
                App.Post(()->mView.updateData(page,lst));
            } finally {
                App.Post(()->mView.hideLoadingDialog());
            }
        });
    }

    public String sIdCardNo = Template.getCurrentTemplate().getUserInputGuideDatabase().getIdFieldName();
    public String sName     = Template.getCurrentTemplate().getUserInputGuideDatabase().getNameFiledName();
    @Override
    void delete(String idCardNo, int index, IDeleteListener listener) {
        Template.getCurrentTemplate().getUserOverallDatabase().deletePeople(new ESONObject().putValue(sIdCardNo, idCardNo), listener);
    }
}
