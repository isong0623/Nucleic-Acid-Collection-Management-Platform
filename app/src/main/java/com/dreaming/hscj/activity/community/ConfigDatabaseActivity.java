package com.dreaming.hscj.activity.community;

import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dreaming.hscj.Constants;
import com.dreaming.hscj.R;
import com.dreaming.hscj.base.BaseActivity;
import com.dreaming.hscj.core.EasyAdapter.EasyAdapter;
import com.dreaming.hscj.dialog.DialogManager;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.template.database.DatabaseTemplate;
import com.dreaming.hscj.template.database.wrapper.DatabaseConfig;
import com.dreaming.hscj.utils.ToastUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

public class ConfigDatabaseActivity extends BaseActivity {
    @Override
    public int getContentViewResId() {
        return R.layout.activity_config_database;
    }

    @Override
    protected void onBindView(View vContent) {
        ButterKnife.bind(this,vContent);
    }

    @BindView(R.id.rv_db)
    RecyclerView rvDb;
    EasyAdapter adapter;
    List<ESONObject> lstData = new ArrayList<>();

    void loadData(){
        lstData.clear();
        ESONArray a = Constants.DBConfig.getAllDatabase();
        for(int i=0,ni=a.length();i<ni;++i){
            ESONObject o = a.getArrayValue(i,new ESONObject());
            if(o.length()==0) continue;

            lstData.add(o);
        }
    }

    @Override
    public void initView() {
        setCenterText("数据库配置");
        setRightText("新建数据库");
        tvTitleRight.setOnClickListener(v -> DatabaseConfig.showCreateDatabaseDialog(this, new DatabaseConfig.IDBCreateListener() {
            @Override
            public void onSuccess(String townName, String villageName) {
                loadData();
                adapter.notifyDataSetChanged();
            }
            @Override
            public void onFailure() { }
        }));

        loadData();

        if(lstData.isEmpty()){
            tvTitleRight.callOnClick();
        }

        rvDb.setLayoutManager(new LinearLayoutManager(this){
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                try {
                    super.onLayoutChildren( recycler, state );
                } catch (IndexOutOfBoundsException e) {
                    e.printStackTrace();
                }
            }
        });
        rvDb.setAdapter(adapter = new EasyAdapter(this, R.layout.recy_database_choose, lstData, (EasyAdapter.IEasyAdapter<ESONObject>) (holder, data, position) -> {
            holder.setText(R.id.tv_id_value,String.valueOf(data.getJSONValue("id",-1)));
            holder.setText(R.id.tv_db_value, Template.getCurrentTemplate().getDatabaseSetting().getRegionName()+"/"+data.getJSONValue("townName","")+"/"+data.getJSONValue("villageName",""));
            CheckBox cb = holder.getView(R.id.cb_choose);
            if(Constants.DBConfig.getSelectedDatabase().getJSONValue("id",-1) == data.getJSONValue("id",-2)){
                cb.setChecked(true);
                holder.getRootView().setBackground(getResources().getDrawable(R.drawable.shape_recy_choose_database_2));
            }
            else{
                holder.getRootView().setBackground(getResources().getDrawable(R.drawable.shape_recy_choose_database_1));
                cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if(isChecked){
                            DialogManager.showAlertDialog(ConfigDatabaseActivity.this,"提示","确定要切换数据库吗？",v -> cb.setChecked(false),v -> {
                                Constants.DBConfig.setSelectedDatabase(data.getJSONValue("id",-2));
                                if(Constants.DBConfig.getSelectedDatabase().getJSONValue("id",-1) == data.getJSONValue("id",-2)){
                                    ToastUtils.show("切换成功！");
                                    adapter.notifyDataSetChanged();
                                    if(Constants.User.isLogin()){
                                        Template.getCurrentTemplate().getUserOverallDatabase().sync();
                                    }
                                    Template.getCurrentTemplate().getNoneGroupingDatabase().sync(null);
                                    return;
                                }
                                ToastUtils.show("切换失败！");
                            });
                        }
                    }
                });
            }

        },false));
    }
}
