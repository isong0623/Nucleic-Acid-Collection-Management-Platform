package com.dreaming.hscj.activity.nucleic_acid.searching;

import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dreaming.hscj.R;
import com.dreaming.hscj.base.BaseActivity;
import com.dreaming.hscj.core.BundleBuilder;
import com.dreaming.hscj.core.EasyAdapter.EasyAdapter;
import com.dreaming.hscj.core.EasyAdapter.EasyViewHolder;
import com.dreaming.hscj.core.ThreadPoolProvider;
import com.dreaming.hscj.dialog.DialogManager;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.template.api.ApiProvider;
import com.dreaming.hscj.template.api.ParamSetter;
import com.dreaming.hscj.template.api.impl.Api;
import com.dreaming.hscj.template.api.wrapper.ApiParam;
import com.dreaming.hscj.utils.JsonUtils;
import com.dreaming.hscj.utils.ToastUtils;
import com.scwang.smart.refresh.layout.api.RefreshLayout;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

public class NASearchingResultActivity extends BaseActivity {

    public static void doSearch(BaseActivity activity, ESONObject queries){
        activity.startActivity(NASearchingResultActivity.class, BundleBuilder.create("query",queries.toString()).build());
    }

    @Override
    public int getContentViewResId() {
        return R.layout.activity_nucleic_acid_searching_result;
    }

    @Override
    protected void onBindView(View vContent) {
        ButterKnife.bind(this,vContent);
    }

    @BindView(R.id.rv_result)
    RecyclerView rvResult;
    List<ESONObject> lstData = new ArrayList<>();

    ESONObject eSearch;
    @Override
    public void initView() {
        setCenterText("核酸采样记录查询结果");
        String query = getIntent().getStringExtra("query");
        if(query == null) query = "";
        eSearch = new ESONObject(query);

        rvResult.setLayoutManager(new LinearLayoutManager(this));
        rvResult.getRecycledViewPool().setMaxRecycledViews(0,0);
        rvResult.setAdapter(new EasyAdapter(this, R.layout.recy_result_item, lstData, new EasyAdapter.IEasyAdapter<ESONObject>() {
            List<ApiParam> lst = Template.getCurrentTemplate().apiOf(Api.TYPE_NC_HISTORY_SEARCH).getResponse().getFields();
            @Override
            public void convert(EasyViewHolder holder, ESONObject data, int position) {
                LinearLayout llContent = holder.getView(R.id.ll_content);
                holder.setText(R.id.tv_index,String.valueOf(position+1));
                llContent.removeAllViews();
                for(int i=0,ni=lst.size();i<ni;++i){
                    ApiParam p = lst.get(i);
                    View v = LayoutInflater.from(NASearchingResultActivity.this).inflate(R.layout.view_shown,null,false);
                    TextView tvN = v.findViewById(R.id.tv_name);
                    tvN.setText(p.getDescription());
                    TextView tvV = v.findViewById(R.id.tv_value);
                    String defaultValue = p.getDefaultValue();
                    String getter = p.getter(data.getJSONValue(p.getName(),""));
                    if(getter == null){
                        if(ApiParam.DEFAULT_NO_EMPTY.equals(defaultValue)){
                            getter = "";
                        }
                        else if(ApiParam.DEFAULT_NULL_STR.equals(defaultValue)){
                            getter = "null";
                        }
                        else{
                            getter = defaultValue;
                        }
                    }
                    tvV.setText(getter);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp2px(30f));
                    params.topMargin = dp2px(1);
                    llContent.addView(v,params);
                }

                TextView tvDelete = new TextView(NASearchingResultActivity.this);
                tvDelete.setText("删除");
                tvDelete.setGravity(Gravity.CENTER);
                tvDelete.setBackgroundResource(R.drawable.shape_btn_delete);
                tvDelete.setTextColor(getResources().getColor(R.color.white));
                tvDelete.getPaint().setFakeBoldText(true);
                tvDelete.setOnClickListener(v -> {
                    DialogManager.showAlertDialog(NASearchingResultActivity.this,"提示","确定要删除该人员的采样记录吗？",null,v1->{
                        ApiProvider.requestDeleteSamplingRecordWithTubeNo(new ParamSetter(Api.TYPE_NC_HISTORY_SEARCH, data), new ApiProvider.ISamplingRecordDeleteListener() {
                            @Override
                            public void onSuccess() {
                                lstData.remove(position);
                                rvResult.getAdapter().notifyDataSetChanged();
                                ToastUtils.show("删除成功！");
                            }

                            @Override
                            public void onFailure(String err) {
                                ToastUtils.show("删除失败："+err);
                            }
                        });
                    });
                });
                tvDelete.setTextSize(TypedValue.COMPLEX_UNIT_SP,16f);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp2px(36));
                layoutParams.topMargin = dp2px(6);
                layoutParams.bottomMargin = dp2px(6);
                llContent.addView(tvDelete,layoutParams);
            }
        },false));

        srlMain.setEnableLoadMore(false);
        srlMain.autoRefresh();
    }

    synchronized void buildViewTree(ESONArray array){
        if(array == null || array.length() == 0) {
            ToastUtils.show("没有查询到相关数据！");
            finish();
            return;
        }

        lstData.clear();
        lstData.addAll(JsonUtils.parseToList(array));
        rvResult.getAdapter().notifyDataSetChanged();
    }

    @Override
    protected boolean hasRefreshBar() {
        return true;
    }

    @Override
    public void onRefresh(@NonNull RefreshLayout refreshLayout) {
        ApiProvider.requestQuerySamplingHistoryRecord(eSearch, new ApiProvider.ISamplingRecordQueryListener() {
            @Override
            public void onSuccess(ESONArray data) {
                refreshLayout.finishRefresh(true);
                buildViewTree(data);
            }

            @Override
            public void onFailure(String err) {
                refreshLayout.finishRefresh(false);
                ToastUtils.show("查询失败："+err);
                finish();
            }
        });
    }

    @Override
    public void onLoadMore(@NonNull RefreshLayout refreshLayout) {
        super.onLoadMore(refreshLayout);
    }
}
