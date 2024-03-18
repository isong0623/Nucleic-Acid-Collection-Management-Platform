package com.dreaming.hscj.activity.nucleic_acid.batch_grouping;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dreaming.hscj.App;
import com.dreaming.hscj.Constants;
import com.dreaming.hscj.R;
import com.dreaming.hscj.base.BaseActivity;
import com.dreaming.hscj.core.EasyAdapter.EasyAdapter;
import com.dreaming.hscj.dialog.DialogManager;
import com.dreaming.hscj.dialog.loading.LoadingDialog;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.template.api.wrapper.ApiParam;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

public class NABatchGroupingConfigActivity extends BaseActivity {
    @Override
    public int getContentViewResId() {
        return R.layout.activity_nucleic_acid_batch_grouping_config;
    }

    @Override
    protected void onBindView(View vContent) {
        ButterKnife.bind(this,vContent);
    }

    @BindView(R.id.rv_config_add)
    RecyclerView rvConfigAdd;
    private LinearLayoutManager addMgr;

    private static final ArrayList<ApiParam> lstAdd = new ArrayList<>();
    private static final ESONObject currentDatabase = Constants.DBConfig.getSelectedDatabase();
    private static String unique = Template.getCurrentTemplate().getDatabaseSetting().getRegionCode()+"_"+Template.getCurrentTemplate().getDatabaseSetting().getUnifySocialCreditCodes();

    private static void resetFields(){
        lstAdd.clear();
        List<ApiParam> lstFields = Template.getCurrentTemplate().getNucleicAcidGroupingDatabase().getConfig().getFields();
        lstAdd.add(lstFields.get(0));
        lstAdd.add(lstFields.get(1));
        unique = Template.getCurrentTemplate().getDatabaseSetting().getRegionCode()+"_"+Template.getCurrentTemplate().getDatabaseSetting().getUnifySocialCreditCodes();
        savingConfig();
    }

    private static void autoLoad(){
        lstAdd.clear();
        ESONObject config = Constants.Config.getBatchGroupingExcelFieldConfig();
        if(config.length() == 0) {
            resetFields();
            return;
        }
        ESONArray eAdd = config.getJSONValue("add",new ESONArray());
        if(eAdd.length() == 0) {
            resetFields();
            return;
        }
        
        for(int i=0,ni=eAdd.length();i<ni;++i){
            ESONObject e = eAdd.getArrayValue(i,new ESONObject());
            String name = e.getJSONValue("name","");
            String type = e.getJSONValue("type","");
            String description = e.getJSONValue("description","");
            String defaultValue = e.getJSONValue("defaultValue","");
            ApiParam p = new ApiParam(name,type,description,defaultValue);
            lstAdd.add(p);
        }
    }

    public static ArrayList<ApiParam> getExcelConfig(){
        ESONObject db = Constants.DBConfig.getSelectedDatabase();
        if(db.getJSONValue("id",-1)!=currentDatabase.getJSONValue("id",0)){
            resetFields();
        }
        return lstAdd;
    }

    static {
        autoLoad();
    }

    public static void savingConfig(){
        ESONArray eAdd = new ESONArray();
        for(int i=0,ni=lstAdd.size();i<ni;++i){
            ApiParam p = lstAdd.get(i);
            ESONObject e = new ESONObject();
            e.putValue("name"        ,p.getName());
            e.putValue("description" ,p.getDescription());
            e.putValue("type"        ,p.getType());
            e.putValue("defaultValue",p.getDefaultValue());
            eAdd.putValue(e);
        }

        ESONObject eData = new ESONObject().putValue("add",eAdd);
        Constants.Config.setBatchGroupingExcelFieldConfig(eData);
    }

    private EasyAdapter addAdapter;

    @Override
    public void initView() {
        String unique = Template.getCurrentTemplate().getDatabaseSetting().getRegionCode()+"_"+Template.getCurrentTemplate().getDatabaseSetting().getUnifySocialCreditCodes();
        ESONObject db = Constants.DBConfig.getSelectedDatabase();
        if(db.getJSONValue("id",-1)!=currentDatabase.getJSONValue("id",0) || !unique.equals(NABatchGroupingConfigActivity.unique)){
            autoLoad();
        }
        setCenterText("Excel导入字段配置");
        setRightText("重置");

        Toast.makeText(this, "长按字段进行排序！", Toast.LENGTH_LONG).show();

        addAdapter = new EasyAdapter(this, R.layout.recy_batch_input_config, lstAdd, (EasyAdapter.IEasyAdapter<ApiParam>) (holder, data, position) -> {
            holder.setText(R.id.tv_index,String.valueOf(position+1));
            holder.setText(R.id.tv_field_name,data.getDescription()+"@"+data.getName());
            holder.setVisibility(R.id.tv_option,View.INVISIBLE);
            if(!data.canEmpty()){
                holder.setTextColor(R.id.tv_field_name,getResources().getColor(R.color.material_red_500));
            }
            else{
                holder.setTextColor(R.id.tv_field_name,getResources().getColor(R.color.material_grey_700));
            }
        })
            .setDragEnabled(true)
            .setEnableDragDelete(false)
            .setCacheEnabled(false);

        rvConfigAdd.setLayoutManager(addMgr = new LinearLayoutManager(this){
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                try {
                    super.onLayoutChildren( recycler, state );
                } catch (IndexOutOfBoundsException e) {
                    e.printStackTrace();
                }
            }
        });
        rvConfigAdd.setAdapter(addAdapter);
        rvConfigAdd.getRecycledViewPool().setMaxRecycledViews(0,0);

        tvTitleRight.setOnClickListener(v->{
            DialogManager.showAlertDialog(this,"提示","确定要重置吗？",null, v1 -> {
                resetFields();
                addAdapter.notifyDataSetChanged();
            });
        });
    }

    @Override
    protected void onDestroy() {
        savingConfig();
        super.onDestroy();
    }

    void showLoadingDialog(){
        LoadingDialog.showDialog("Refresh List",this);
    }

    void hideLoadingDialog(){
        LoadingDialog.dismissDialog("Refresh List");
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if(event!=null){
            if(event.getAction() == MotionEvent.ACTION_UP){
                showLoadingDialog();
                int pos = addMgr.findLastCompletelyVisibleItemPosition();
                Log.e("onTouchEvent","MotionEvent.ACTION_UP");
                App.PostDelayed(()->{
                    List<ApiParam> lst0 = new ArrayList<>();
                    lst0.addAll(lstAdd);
                    lstAdd.clear();
                    addAdapter.notifyDataSetChanged();
                    App.PostDelayed(()->{
                        lstAdd.addAll(lst0);
                        addAdapter.notifyDataSetChanged();
                        App.PostDelayed(()->{
                            addMgr.scrollToPosition(pos);
                            hideLoadingDialog();
                        },100);
                    },100);
                },100);
            }
        }
        return super.dispatchTouchEvent(event);
    }
}
