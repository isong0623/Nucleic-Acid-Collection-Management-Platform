package com.dreaming.hscj.activity.community.batch_input;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dreaming.hscj.App;
import com.dreaming.hscj.Constants;
import com.dreaming.hscj.R;
import com.dreaming.hscj.activity.nucleic_acid.batch_grouping.NABatchGroupingConfigActivity;
import com.dreaming.hscj.base.BaseActivity;
import com.dreaming.hscj.core.EasyAdapter.EasyAdapter;
import com.dreaming.hscj.dialog.DialogManager;
import com.dreaming.hscj.dialog.loading.LoadingDialog;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.template.api.wrapper.ApiParam;
import com.dreaming.hscj.template.database.DatabaseTemplate;
import com.tencent.bugly.proguard.A;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

public class CommunityBatchInputConfigActivity extends BaseActivity {
    @Override
    public int getContentViewResId() {
        return R.layout.activity_community_batch_input_config;
    }

    @Override
    protected void onBindView(View vContent) {
        ButterKnife.bind(this,vContent);
    }

    @BindView(R.id.rv_config_add)
    RecyclerView rvConfigAdd;
    @BindView(R.id.rv_config_del)
    RecyclerView rvConfigDel;

    private static final ArrayList<ApiParam> lstAdd = new ArrayList<>();
    private static final ArrayList<ApiParam> lstDel = new ArrayList<>();
    private static String sIdCardNoFieldName = Template.getCurrentTemplate().getUserOverallDatabase().getIdFieldName();
    private static String sNameFieldName     = Template.getCurrentTemplate().getUserOverallDatabase().getNameFiledName();
    private static final ESONObject currentDatabase = Constants.DBConfig.getSelectedDatabase();
    private static String unique = Template.getCurrentTemplate().getDatabaseSetting().getRegionCode()+"_"+Template.getCurrentTemplate().getDatabaseSetting().getUnifySocialCreditCodes();

    private static void resetFields(){
        lstAdd.clear();
        lstAdd.addAll(Template.getCurrentTemplate().getUserInputGuideDatabase().getConfig().getFields());
        lstDel.clear();
        lstDel.addAll( Template.getCurrentTemplate().getUserOverallDatabase().getConfig().getFields());
        sIdCardNoFieldName = Template.getCurrentTemplate().getUserOverallDatabase().getIdFieldName();
        sNameFieldName     = Template.getCurrentTemplate().getUserOverallDatabase().getNameFiledName();
        unique = Template.getCurrentTemplate().getDatabaseSetting().getRegionCode()+"_"+Template.getCurrentTemplate().getDatabaseSetting().getUnifySocialCreditCodes();
        for(int i=0,ni=lstDel.size();i<ni;++i){
            ApiParam p1 = lstDel.get(i);
            for(int j=0,nj=lstAdd.size();j<nj;++j){
                ApiParam p2 = lstAdd.get(j);
                if(p1.getName().equals(p2.getName())){
                    lstDel.remove(i);
                    --i;
                    --ni;
                    break;
                }
            }
        }
    }

    private static void autoLoad(){
        lstAdd.clear();
        lstDel.clear();
        ESONObject config = Constants.Config.getBatchInputExcelFieldConfig();
        if(config.length() == 0) {
            resetFields();
            return;
        }
        ESONArray eAdd = config.getJSONValue("add",new ESONArray());
        if(eAdd.length() == 0) {
            resetFields();
            return;
        }
        ESONArray eDel = config.getJSONValue("del",new ESONArray());

        for(int i=0,ni=eAdd.length();i<ni;++i){
            ESONObject e = eAdd.getArrayValue(i,new ESONObject());
            String name = e.getJSONValue("name","");
            String type = e.getJSONValue("type","");
            String description = e.getJSONValue("description","");
            String defaultValue = e.getJSONValue("defaultValue","");
            ApiParam p = new ApiParam(name,type,description,defaultValue);
            lstAdd.add(p);
        }

        for(int i=0,ni=eDel.length();i<ni;++i){
            ESONObject e = eDel.getArrayValue(i,new ESONObject());
            String name = e.getJSONValue("name","");
            String type = e.getJSONValue("type","");
            String description = e.getJSONValue("description","");
            String defaultValue = e.getJSONValue("defaultValue","");
            ApiParam p = new ApiParam(name,type,description,defaultValue);
            lstDel.add(p);
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

        ESONArray eDel = new ESONArray();
        for(int i=0,ni=lstDel.size();i<ni;++i){
            ApiParam p = lstDel.get(i);
            ESONObject e = new ESONObject();
            e.putValue("name"        ,p.getName());
            e.putValue("description" ,p.getDescription());
            e.putValue("type"        ,p.getType());
            e.putValue("defaultValue",p.getDefaultValue());
            eDel.putValue(e);
        }

        ESONObject eData = new ESONObject().putValue("add",eAdd).putValue("del",eDel);
        Constants.Config.setBatchInputExcelFieldConfig(eData);
    }

    private EasyAdapter addAdapter;
    private EasyAdapter delAdapter;
    private LinearLayoutManager addMgr;
    private LinearLayoutManager delMgr;

    @Override
    public void initView() {
        String unique = Template.getCurrentTemplate().getDatabaseSetting().getRegionCode()+"_"+Template.getCurrentTemplate().getDatabaseSetting().getUnifySocialCreditCodes();
        ESONObject db = Constants.DBConfig.getSelectedDatabase();
        if(db.getJSONValue("id",-1)!=currentDatabase.getJSONValue("id",0) || !unique.equals(CommunityBatchInputConfigActivity.unique)){
            autoLoad();
        }
        setCenterText("Excel导入字段配置");
        setRightText("重置");

        Toast.makeText(this, "长按字段进行排序！\n\n红色名字段为Excel保存必须字段，若Excel导入字段全部包含红色字段则在本地和网络为空的情况下可将Excel数据保存到本地！", Toast.LENGTH_LONG).show();

        addAdapter = new EasyAdapter(this, R.layout.recy_batch_input_config, lstAdd, (EasyAdapter.IEasyAdapter<ApiParam>) (holder, data, position) -> {
            holder.setText(R.id.tv_index,String.valueOf(position+1));
            holder.setText(R.id.tv_field_name,data.getDescription()+"@"+data.getName());
            holder.setBackgroundRes(R.id.tv_option,R.drawable.shape_input_config_del);
            holder.setText(R.id.tv_option,"移除");
            if(!data.canEmpty()){
                holder.setTextColor(R.id.tv_field_name,getResources().getColor(R.color.material_red_500));
            }
            else{
                holder.setTextColor(R.id.tv_field_name,getResources().getColor(R.color.material_grey_700));
            }

            holder.setOnClickListener(R.id.tv_option,v->{
                if(sIdCardNoFieldName.equals(data.getName())) {
                    Toast.makeText(CommunityBatchInputConfigActivity.this, "【身份号】字段不允许删除！", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(sNameFieldName.equals(data.getName())){
                    Toast.makeText(CommunityBatchInputConfigActivity.this,"【姓名】字段不允许删除！",Toast.LENGTH_SHORT).show();
                    return;
                }
                boolean bContains = false;
                for(int i=0,ni=lstDel.size();i<ni;++i){
                    if(lstDel.get(i).getName().equals(data.getName())){
                        bContains = true;
                        break;
                    }
                }
                if(!bContains){
                    lstDel.add(data);
                    delAdapter.notifyItemInserted(lstDel.size()-1);
                    delAdapter.notifyDataSetChanged();
                }

                lstAdd.remove(position);
                addAdapter.notifyItemRemoved(position);
                addAdapter.notifyDataSetChanged();
            });
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
        delAdapter = new EasyAdapter(this, R.layout.recy_batch_input_config, lstDel, (EasyAdapter.IEasyAdapter<ApiParam>) (holder, data, position) -> {
            holder.setText(R.id.tv_index,String.valueOf(position+1));
            holder.setText(R.id.tv_field_name,data.getDescription()+"@"+data.getName());
            holder.setBackgroundRes(R.id.tv_option,R.drawable.shape_input_config_add);
            holder.setText(R.id.tv_option,"增加");
            if(!data.canEmpty()){
                holder.setTextColor(R.id.tv_field_name,getResources().getColor(R.color.material_red_500));
            }
            holder.setOnClickListener(R.id.tv_option,v->{
                boolean bContains = false;
                for(int i=0,ni=lstAdd.size();i<ni;++i){
                    if(lstAdd.get(i).getName().equals(data.getName())){
                        bContains = true;
                        break;
                    }
                }
                if(!bContains){
                    lstAdd.add(data);
                    addAdapter.notifyItemInserted(lstAdd.size()-1);
                    addAdapter.notifyDataSetChanged();
                }

                lstDel.remove(position);
                delAdapter.notifyItemRemoved(position);
                delAdapter.notifyItemRangeChanged(position,lstDel.size());
                delAdapter.notifyDataSetChanged();
            });
        })
                .setDragEnabled(true)
                .setEnableDragDelete(false)
                .setCacheEnabled(false);
        rvConfigDel.setLayoutManager(delMgr = new LinearLayoutManager(this){
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                try {
                    super.onLayoutChildren( recycler, state );
                } catch (IndexOutOfBoundsException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onScrollStateChanged(int state) {
                delMgr.findFirstVisibleItemPosition();
                super.onScrollStateChanged(state);
            }

        });
        rvConfigDel.setAdapter(delAdapter);
        rvConfigDel.getRecycledViewPool().setMaxRecycledViews(0,0);

        tvTitleRight.setOnClickListener(v->{
            DialogManager.showAlertDialog(this,"提示","确定要重置吗？",null, v1 -> {
                resetFields();
                addAdapter.notifyDataSetChanged();
                delAdapter.notifyDataSetChanged();
            });
        });
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
                Log.e("onTouchEvent","MotionEvent.ACTION_UP");
                int visibleAdd = addMgr.findLastCompletelyVisibleItemPosition();
                int visibleDel = delMgr.findLastCompletelyVisibleItemPosition();

                Log.e("add",visibleAdd+"");
                Log.e("del",visibleDel+"");

                App.PostDelayed(()->{
                    List<ApiParam> lst0 = new ArrayList<>();
                    lst0.addAll(lstAdd);
                    lstAdd.clear();
                    addAdapter.notifyDataSetChanged();
                    App.PostDelayed(()->{
                        lstAdd.addAll(lst0);
                        addAdapter.notifyDataSetChanged();
                        App.PostDelayed(()->addMgr.scrollToPosition(visibleAdd),100);
                    },100);

                    List<ApiParam> lst1 = new ArrayList<>();
                    lst1.addAll(lstDel);
                    lstDel.clear();
                    delAdapter.notifyDataSetChanged();
                    App.PostDelayed(()->{
                        lstDel.addAll(lst1);
                        delAdapter.notifyDataSetChanged();
                        App.PostDelayed(()->{
                            delMgr.scrollToPosition(visibleDel);
                            hideLoadingDialog();
                        },100);
                    },100);
                },100);
            }
        }
        return super.dispatchTouchEvent(event);
    }
}
