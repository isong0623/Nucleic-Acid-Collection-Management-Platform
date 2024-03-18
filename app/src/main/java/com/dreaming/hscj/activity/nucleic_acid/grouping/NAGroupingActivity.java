package com.dreaming.hscj.activity.nucleic_acid.grouping;

import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dreaming.hscj.R;
import com.dreaming.hscj.activity.community.search.CommunitySearchActivity;
import com.dreaming.hscj.activity.nucleic_acid.batch_grouping.NABatchGroupingActivity;
import com.dreaming.hscj.base.BaseActivity;
import com.dreaming.hscj.base.contract.BaseMVPActivity;
import com.dreaming.hscj.core.BundleBuilder;
import com.dreaming.hscj.core.EasyAdapter.EasyAdapter;
import com.dreaming.hscj.core.EasyAdapter.EasyViewHolder;
import com.dreaming.hscj.core.ThreadPoolProvider;
import com.dreaming.hscj.dialog.DialogManager;
import com.dreaming.hscj.dialog.loading.LoadingDialog;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.template.database.DatabaseTemplate;
import com.dreaming.hscj.template.database.impl.NoneGroupingDatabase;
import com.dreaming.hscj.template.database.impl.NucleicAcidGroupingDatabase;
import com.dreaming.hscj.template.database.wrapper.IGetterListener;
import com.dreaming.hscj.template.database.wrapper.ISetterListener;
import com.dreaming.hscj.utils.JsonUtils;
import com.dreaming.hscj.utils.ToastUtils;
import com.scwang.smart.refresh.layout.api.RefreshLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

public class NAGroupingActivity extends BaseMVPActivity<NAGroupingPresenter> implements INAGroupingContract.View{
    private static final String TAG = NAGroupingActivity.class.getSimpleName();
    @Override
    public int getContentViewResId() {
        return R.layout.activity_nucleic_acid_grouping;
    }

    @Override
    protected void onBindView(View vContent) {
        ButterKnife.bind(this,vContent);
    }

    public interface ISelectCallback{
        void onSelected(List<ESONObject> lstSelected);
    }
    public static void doSelect(BaseActivity activity, int max, ISelectCallback callback){
        activity.startActivityForResult(
                NAGroupingActivity.class,
                BundleBuilder
                        .create("max", max)
                        .put("select",true)
                        .build(),
                1024,
                new OnActivityResultItemCallBack() {
            @Override
            public void OnActivityRequestResult(int resultCode, Intent data) {
                if(data==null){
                    callback.onSelected(new ArrayList<>());
                    return;
                }
                String result = data.getStringExtra("data");
                if(result == null) result = "";
                ESONObject e = new ESONObject(result);
                ESONArray a = e.getJSONValue("data",new ESONArray());
                List<ESONObject> l = new ArrayList<>();
                Log.e("ON",l.size()+":"+result);
                for(int i=0,ni=a.length();i<ni;++i){
                    l.add(a.getArrayValue(i,new ESONObject()));
                }
                callback.onSelected(l);
            }
        });
    }

    @BindView(R.id.part_title)
    CardView cvTitleShown;

    @BindView(R.id.part_title_select)
    CardView cvTitleSelect;

    @BindView(R.id.tv_total_num)
    TextView tvTotalNum;
    @BindView(R.id.tv_group_in_num)
    TextView tvGroupInNum;
    @BindView(R.id.tv_group_out_num)
    TextView tvGroupOutNum;

    @BindView(R.id.tv_data_empty)
    TextView tvDataEmpty;

    @BindView(R.id.rv_group)
    RecyclerView rvGroup;

    @BindView(R.id.tv_finish)
    TextView tvFinish;

    EasyAdapter adapter;

    final List<ESONObject> lstData = new ArrayList<>();
    boolean bIsSelect = false;
    int max = Template.getCurrentTemplate().getDatabaseSetting().getGroupMemberNum();
    Map<String,Integer> mSelectNum = new HashMap<>();
    Map<String,List<ESONObject>> mSelectedData = new HashMap<>();

    @Override
    public void initView() {

        bIsSelect = getIntent().getBooleanExtra("select",false);
        max       = getIntent().getIntExtra("max",max);

        if(bIsSelect){
            setCenterText("选择核酸分组");
            cvTitleShown.setVisibility(View.INVISIBLE);
            cvTitleSelect.setVisibility(View.VISIBLE);
            adapter = new EasyAdapter(this, R.layout.recy_group_list_select, lstData, (EasyAdapter.IEasyAdapter<ESONObject>) (holder, data, position) -> {
                Log.e(TAG,data.toString());

                String groupId = data.getJSONValue(db3.getGroupIdFieldName(),"");
                int peopleNum  = data.getJSONValue("peopleNum",0);
                Integer selectedNum = mSelectNum.get(groupId);
                if (selectedNum == null) selectedNum = 0;

                holder.setText(R.id.tv_group_id  ,groupId);
                holder.setText(R.id.tv_people_num,data.getJSONValue("peopleNum",""));
                holder.setText(R.id.tv_selected_num,String.valueOf(selectedNum));
                holder.setOnClickListener(R.id.tv_detail,v -> {
                    int count = 0;
                    for(Map.Entry<String,Integer> entry:mSelectNum.entrySet()){
                        if(entry.getValue() == null) continue;
                        count += entry.getValue();
                    }

                    NAGroupingAddActivity.Select(this, groupId, Math.max(max - count,0), new NAGroupingAddActivity.ISelectCallback() {
                        @Override
                        public void onSelected(List<ESONObject> lstSelected) {
                            List<ESONObject> lstGroup = mSelectedData.get(groupId);
                            if(lstGroup == null) lstGroup = new ArrayList<>();

                            for(int i=0,ni=lstSelected.size();i<ni;++i){
                                String idCardNo1 = lstSelected.get(i).getJSONValue(db3.getCardIdFieldName(),"");
                                boolean contains = false;
                                for(Map.Entry<String,List<ESONObject>> entry: mSelectedData.entrySet()){
                                    if(entry.getValue() == null) continue;
                                    for(ESONObject e:entry.getValue()){
                                        String idCardNo2 = e.getJSONValue(db3.getCardIdFieldName(),"");
                                        if(idCardNo1.equals(idCardNo2)){
                                            ToastUtils.show(idCardNo1+"重复添加！");
                                            contains = true;
                                            break;
                                        }
                                    }
                                    if(contains) break;
                                }
                                if(contains) continue;
                                lstGroup.add(lstSelected.get(i));
                            }
                            if(lstGroup.isEmpty()) return;
                            mSelectedData.put(groupId,lstGroup);
                            mSelectNum.put(groupId,lstGroup.size());
                            tvFinish.setVisibility(mSelectedData.isEmpty()?View.INVISIBLE:View.VISIBLE);
                            adapter.notifyItemChanged(position);
                        }

                        @Override
                        public void onGroupChanged(List<ESONObject> mAdded, List<ESONObject> mDeleted) {
                            if(mAdded!=null && !mAdded.isEmpty() || mDeleted!=null && !mDeleted.isEmpty()){
                                srlMain.autoRefresh();
                            }
                        }
                    });
                });

                CheckBox cbSelect = holder.getView(R.id.cb_select);
                cbSelect.setChecked(peopleNum == selectedNum);
                cbSelect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        List<ESONObject> selected = mSelectedData.get(groupId);
                        if (isChecked){
                            if(selected == null) selected = new ArrayList<>();
                            int count = 0;
                            for(Map.Entry<String,Integer> entry:mSelectNum.entrySet()){
                                if(entry.getValue() == null) continue;
                                count += entry.getValue();
                            }
                            if(max>0 && count - selected.size() + peopleNum > max){
                                ToastUtils.show("最多选择"+max+"人！");
                                cbSelect.setChecked(false);
                                return;
                            }
                            LoadingDialog.showDialog("Get Local Data",NAGroupingActivity.this);
                            db3.getGroupInfo(groupId, new IGetterListener() {
                                @Override
                                public void onSuccess(ESONArray data) {
                                    mSelectedData.put(groupId, JsonUtils.parseToList(data));
                                    mSelectNum.put(groupId,data.length());
                                    adapter.notifyItemChanged(position);
                                    tvFinish.setVisibility(mSelectedData.isEmpty()?View.INVISIBLE:View.VISIBLE);
                                    LoadingDialog.dismissDialog("Get Local Data");
                                }

                                @Override
                                public void onFailure(String err) {
                                    ToastUtils.show(err);
                                    cbSelect.setChecked(false);
                                    LoadingDialog.dismissDialog("Get Local Data");
                                }
                            });
                        }
                        else {
                            if(selected == null) return;
                            if(selected.size() != peopleNum) return;

                            mSelectNum.remove(groupId);
                            mSelectedData.remove(groupId);

                            adapter.notifyItemChanged(position);
                            tvFinish.setVisibility(mSelectedData.isEmpty()?View.INVISIBLE:View.VISIBLE);
                        }
                    }
                });
            },false);
            rvGroup.getRecycledViewPool().setMaxRecycledViews(0,0);
        }
        else{
            setCenterText("社区成员核酸分组");
            setRightText("批量导入");
            tvTitleRight.setOnClickListener(v -> startActivityForResult(NABatchGroupingActivity.class, 1000, new OnActivityResultItemCallBack() {
                @Override
                public void OnActivityRequestResult(int resultCode, Intent data) {
                    if(resultCode == RESULT_OK){
                        srlMain.autoRefresh();
                    }
                }
            }));
            cvTitleShown.setVisibility(View.VISIBLE);
            cvTitleSelect.setVisibility(View.INVISIBLE);
            adapter = new EasyAdapter(this, R.layout.recy_group_list, lstData, (EasyAdapter.IEasyAdapter<ESONObject>) (holder, data, position) -> {
                Log.e(TAG,data.toString());
                holder.setText(R.id.tv_group_id  ,data.getJSONValue(db3.getGroupIdFieldName(),""));
                holder.setText(R.id.tv_people_num,data.getJSONValue("peopleNum",""));
                holder.setOnClickListener(R.id.tv_detail,v -> {
                    NAGroupingAddActivity.Preview(this, data.getJSONValue(db3.getGroupIdFieldName(), ""), new NAGroupingAddActivity.IPreviewCallback() {
                        @Override
                        public void onGroupChanged(List<ESONObject> mAdded, List<ESONObject> mDeleted) {
                            if(mAdded!=null && !mAdded.isEmpty() || mDeleted!=null && !mDeleted.isEmpty()){
                                srlMain.autoRefresh();
                            }
                        }
                    });
                });
                holder.setOnClickListener(R.id.tv_delete,v -> {
                    DialogManager.showAlertDialog(this,"提示","确定要删除该组及其所有成员吗？",null,v1->{
                        db3.deleteGroup(data.getJSONValue(db3.getGroupIdFieldName(), ""), new ISetterListener() {
                            @Override
                            public void onSuccess() {
                                lstData.remove(position);
                                adapter.notifyDataSetChanged();
                                updateTitle();
                            }

                            @Override
                            public void onFailure(String err) {
                                ToastUtils.show(err);
                            }
                        });
                    });
                });
            });
        }

        rvGroup.setLayoutManager(new LinearLayoutManager(this){
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                try {
                    super.onLayoutChildren( recycler, state );
                } catch (IndexOutOfBoundsException e) {
                    e.printStackTrace();
                }
            }
        });

        rvGroup.setAdapter(adapter);
        srlMain.autoRefresh();
    }

    @OnClick(R.id.tv_create)
    void onCreateGroupClicked(){
        NAGroupingAddActivity.Create(this, new NAGroupingAddActivity.ICreateCallback() {
            @Override
            public void onCreate(String groupId, List<ESONObject> members) {
                srlMain.autoRefresh();
            }

            @Override
            public void onCancel() {}
        });
    }

    void updateTitle(){
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            final int iCountGroup    = db3.countGroup();
            final int iCountGroupIn  = db3.countSync();
            final int iCountGroupOut = db4.count();

            post(()->tvTotalNum   .setText(iCountGroup   +"组"));
            post(()->tvGroupInNum .setText(iCountGroupIn +"人"));
            post(()->tvGroupOutNum.setText(iCountGroupOut+"人"));
        });
    }

    void update(ESONArray data){
        if(data==null || data.length()==0){
            if(lstData.isEmpty()){
                tvDataEmpty.setVisibility(View.VISIBLE);
            }
            return;
        }
        tvDataEmpty.setVisibility(View.GONE);
        int startIndex = lstData.size();
        List<ESONObject> lst = new ArrayList<>();
        for(int i=0,ni=data.length();i<ni;++i){
            lst.add(data.getArrayValue(i,new ESONObject()));
        }
        lstData.addAll(lst);
        adapter.notifyItemRangeInserted(startIndex,lstData.size()-startIndex);
    }

    @Override
    protected boolean hasRefreshBar() {
        return true;
    }

    NucleicAcidGroupingDatabase db3 = Template.getCurrentTemplate().getNucleicAcidGroupingDatabase();
    NoneGroupingDatabase        db4 = Template.getCurrentTemplate().getNoneGroupingDatabase();
    int currentPage = 0;
    int pageSize = 30;
    @Override
    public void onRefresh(@NonNull RefreshLayout refreshLayout) {
        lstData.clear();
        adapter.notifyDataSetChanged();
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            updateTitle();

            db3.getGroupingList(currentPage = 0,pageSize,new IGetterListener() {
                @Override
                public void onSuccess(ESONArray data) {
                    update(data);
                    refreshLayout.finishRefresh(true);
                }

                @Override
                public void onFailure(String err) {
                    ToastUtils.show(err);
                    refreshLayout.finishRefresh(false);
                }
            });
        });
    }

    @Override
    public void onLoadMore(@NonNull RefreshLayout refreshLayout) {
        db3.getGroupingList(++currentPage,pageSize,new IGetterListener() {
            @Override
            public void onSuccess(ESONArray data) {
                if(data == null || data.length() == 0){
                    refreshLayout.finishLoadMore(0,true,true);
                }
                else{
                    refreshLayout.finishLoadMore(true);
                }
                update(data);
            }

            @Override
            public void onFailure(String err) {
                ToastUtils.show(err);
                refreshLayout.finishLoadMore(false);
            }
        });
    }

    @Override
    public void finish() {
        autoSetResult();
        super.finish();
    }

    @Override
    public void onBackPressed() {
        autoSetResult();
        super.onBackPressed();
    }

    @OnClick(R.id.tv_finish)
    void onFinishClicked(){
        finish();
    }

    boolean bIsSetResult = false;
    void autoSetResult(){
        if(!bIsSelect) return;
        if(bIsSetResult) return;
        bIsSetResult = true;

        Intent intent = new Intent();
        List<ESONObject> lstSelected = new ArrayList<>();
        for(Map.Entry<String,List<ESONObject>> entry : mSelectedData.entrySet()){
            if(entry.getValue()==null) continue;
            lstSelected.addAll(entry.getValue());
        }
        Log.e("RETURN",lstSelected.size()+":"+JsonUtils.parse2Array(lstSelected).toString());
        intent.putExtras(BundleBuilder.create().put("data",new ESONObject().putValue("data",JsonUtils.parse2Array(lstSelected)).toString()).build());
        setResult(RESULT_OK,intent);
    }
}
