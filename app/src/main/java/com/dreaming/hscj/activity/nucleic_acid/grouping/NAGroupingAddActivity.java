package com.dreaming.hscj.activity.nucleic_acid.grouping;

import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dreaming.hscj.App;
import com.dreaming.hscj.R;
import com.dreaming.hscj.activity.system.MemberQueryActivity;
import com.dreaming.hscj.activity.community.search.CommunitySearchActivity;
import com.dreaming.hscj.base.BaseActivity;
import com.dreaming.hscj.core.BundleBuilder;
import com.dreaming.hscj.core.EasyAdapter.EasyAdapter;
import com.dreaming.hscj.dialog.DialogManager;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.template.database.impl.NucleicAcidGroupingDatabase;
import com.dreaming.hscj.template.database.wrapper.IDeleteListener;
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

public class NAGroupingAddActivity extends BaseActivity {
    private static final String TAG = NAGroupingAddActivity.class.getSimpleName();
    @Override
    public int getContentViewResId() {
        return R.layout.activity_nucleic_acid_grouping_add;
    }

    @Override
    protected void onBindView(View vContent) {
        ButterKnife.bind(this,vContent);
    }


    public interface ICreateCallback{
        void onCreate(String groupId, List<ESONObject> members);
        void onCancel();
    }
    public static void Create(BaseActivity activity,ICreateCallback callback){
        activity.startActivityForResult(NAGroupingAddActivity.class, 6001, new OnActivityResultItemCallBack() {
            @Override
            public void OnActivityRequestResult(int resultCode, Intent data) {
                if(resultCode == activity.RESULT_OK){
                    String group = data.getStringExtra("group");
                    String member= data.getStringExtra("member");

                    callback.onCreate(group,JsonUtils.parseToList(new ESONObject(member).getJSONValue("data",new ESONArray())));
                }
                else{
                    callback.onCancel();
                }
            }
        });
    }

    public interface IPreviewCallback{
        void onGroupChanged(List<ESONObject> mAdded,List<ESONObject> mDeleted);
    }
    public static void Preview(BaseActivity activity, String groupId, IPreviewCallback callback){
        activity.startActivityForResult(NAGroupingAddActivity.class, BundleBuilder.create("group", groupId).put("mode",1).build(), 6002, new OnActivityResultItemCallBack() {
            @Override
            public void OnActivityRequestResult(int resultCode, Intent data) {
                String sAdd = data.getStringExtra("add");
                if(sAdd==null) sAdd = "";
                ESONArray aAdd = new ESONObject(sAdd).getJSONValue("data",new ESONArray());

                String sDel = data.getStringExtra("del");
                if(sDel==null) sDel = "";
                ESONArray aDel = new ESONObject(sDel).getJSONValue("data",new ESONArray());

                callback.onGroupChanged(JsonUtils.parseToList(aAdd),JsonUtils.parseToList(aDel));
            }
        });
    }

    public interface ISelectCallback{
        void onSelected(List<ESONObject> lstSelected);
        void onGroupChanged(List<ESONObject> mAdded,List<ESONObject> mDeleted);
    }
    public static void Select(BaseActivity activity,String groupId, int max, ISelectCallback callback){
        activity.startActivityForResult(NAGroupingAddActivity.class, BundleBuilder.create("group", groupId).put("mode",2).put("max",max).build(), 6002, new OnActivityResultItemCallBack() {
            @Override
            public void OnActivityRequestResult(int resultCode, Intent data) {
                String sAdd = data.getStringExtra("add");
                if(sAdd==null) sAdd = "";
                ESONArray aAdd = new ESONObject(sAdd).getJSONValue("data",new ESONArray());

                String sDel = data.getStringExtra("del");
                if(sDel==null) sDel = "";
                ESONArray aDel = new ESONObject(sDel).getJSONValue("data",new ESONArray());

                String sSel = data.getStringExtra("select");
                if(sSel==null) sSel = "";
                ESONArray aSel = new ESONObject(sSel).getJSONValue("data",new ESONArray());

                try { callback.onSelected(JsonUtils.parseToList(aSel)); } catch (Exception e) { e.printStackTrace(); }

                try { callback.onGroupChanged(JsonUtils.parseToList(aAdd),JsonUtils.parseToList(aDel)); } catch (Exception e) { e.printStackTrace(); }
            }
        });
    }

    @BindView(R.id.tv_name)
    TextView tvName;
    @BindView(R.id.ev_value)
    EditText etValue;
    @BindView(R.id.rv_data)
    RecyclerView rvData;

    @BindView(R.id.part_title)
    CardView cvTitleShown;

    @BindView(R.id.part_title_select)
    CardView cvTitleSelect;

    @BindView(R.id.tv_add_searching)
    TextView tvAddSearching;
    @BindView(R.id.tv_add)
    TextView tvAdd;

    List<ESONObject> lstData = new ArrayList<>();
    EasyAdapter adapter;
    String sGroupName = null;
    List<ESONObject> lstAdd = new ArrayList<>();
    List<ESONObject> lstDel = new ArrayList<>();

    Map<String,Integer> mSelectedIndex = new HashMap<>();
    List<ESONObject> lstSelectedData = new ArrayList<>();

    int mode = 0;//0 创建 1详情 2选择
    int max  = Template.getCurrentTemplate().getDatabaseSetting().getGroupMemberNum();

    @Override
    public void initView() {

        sGroupName = getIntent().getStringExtra("group");
        mode       = getIntent().getIntExtra("mode",0);
        max        = getIntent().getIntExtra("max",max);

        tvName.setText("组名");
        etValue.setHint("请输入组名");

        setOnKeyBoardLayoutStateChangeListener(new OnKeyBoardLayoutStateChangeListener() {
            @Override
            public void onKeyBoardShow(int keyBoardHeight, int gapHeight) { }

            @Override
            public void onKeyBoardHide() {
                etValue.clearFocus();
                if(sGroupName!=null && !sGroupName.trim().isEmpty()){
                    srlMain.autoRefresh();
                }
            }
        });

        srlMain.setEnableLoadMore(false);
        cvTitleShown.setVisibility(mode == 2?View.INVISIBLE:View.VISIBLE);
        cvTitleSelect.setVisibility(mode == 2? View.VISIBLE:View.INVISIBLE);

        switch (mode){
            case 0:
                setCenterText("添加核酸分组");
                tvAdd.setVisibility(View.GONE);
                tvAddSearching.setVisibility(View.GONE);
                etValue.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) { }
                    @Override
                    public void afterTextChanged(Editable s) {
                        sGroupName = s.toString();
                    }
                });
                break;
            case 1:
                etValue.setText(sGroupName);
                etValue.setEnabled(false);
                setCenterText("核酸分组详情");
                break;
            case 2:
                setCenterText("选择分组成员");
                etValue.setText(sGroupName);
                etValue.setEnabled(false);
                break;
        }

        rvData.setLayoutManager(new LinearLayoutManager(this){
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                try {
                    super.onLayoutChildren( recycler, state );
                } catch (IndexOutOfBoundsException e) {
                    e.printStackTrace();
                }
            }
        });
        if(mode == 1 || mode == 0){
            adapter = new EasyAdapter(this, R.layout.recy_group_detail, lstData, (EasyAdapter.IEasyAdapter<ESONObject>) (holder, data, position) -> {
                holder.setText(R.id.tv_name,data.getJSONValue(db.getNameFiledName(),""));
                holder.setText(R.id.tv_id_card_no,data.getJSONValue(db.getCardIdFieldName(),""));
                holder.setOnClickListener(R.id.tv_detail,v -> NAGroupInDetailActivity.shownMember(NAGroupingAddActivity.this,data));
                holder.setOnClickListener(R.id.tv_delete,v -> {
                    DialogManager.showAlertDialog(NAGroupingAddActivity.this,"提示","确定要在该组中移除此成员吗？",null,v1 -> {
                        db.deletePeopleWithGroup(data.getJSONValue(db.getCardIdFieldName(), ""), sGroupName, new IDeleteListener() {
                            @Override
                            public void onSuccess(int count) {
                                if(count>0){
                                    Log.e(TAG,"DELETE SUCCESS");
                                    lstData.remove(position);
                                    adapter.notifyItemRemoved(position);
                                    adapter.notifyDataSetChanged();
                                    App.Post(()->adapter.notifyDataSetChanged());

                                    String idCardNo = data.getJSONValue(db.getCardIdFieldName(),"");
                                    for(int i=0,ni=lstAdd.size();i<ni;++i){
                                        ESONObject eDel = lstAdd.get(i);
                                        String s = eDel.getJSONValue(db.getCardIdFieldName(),"");
                                        if(s.equals(idCardNo)) {
                                            lstDel.remove(i);
                                            --i;
                                            --ni;
                                        }
                                    }

                                    for(int i=0,ni=lstDel.size();i<ni;++i){
                                        ESONObject eDel = lstDel.get(i);
                                        String s = eDel.getJSONValue(db.getCardIdFieldName(),"");
                                        if(s.equals(idCardNo)) {
                                            lstDel.remove(i);
                                            --i;
                                            --ni;
                                        }
                                    }

                                    lstDel.add(data);
                                }
                                updateAddBtnVisibility();
                            }

                            @Override
                            public void onFailure(String err) {}
                        });
                    });
                });
            });
        }
        if(mode == 2){
            adapter = new EasyAdapter(this, R.layout.recy_group_detail_select, lstData, (EasyAdapter.IEasyAdapter<ESONObject>) (holder, data, position) -> {
                holder.setText(R.id.tv_name,data.getJSONValue(db.getNameFiledName(),""));
                String idCardNo = data.getJSONValue(db.getCardIdFieldName(),"");
                holder.setText(R.id.tv_id_card_no,idCardNo);
                holder.setOnClickListener(R.id.tv_detail,v -> NAGroupInDetailActivity.shownMember(NAGroupingAddActivity.this,data));
                holder.setOnClickListener(R.id.tv_delete,v -> {
                    DialogManager.showAlertDialog(NAGroupingAddActivity.this,"提示","确定要在该组中移除此成员吗？",null, v1 -> {
                        db.deletePeopleWithGroup(data.getJSONValue(db.getCardIdFieldName(), ""), sGroupName, new IDeleteListener() {
                            @Override
                            public void onSuccess(int count) {
                                if(count>0){
                                    Log.e(TAG,"DELETE SUCCESS");
                                    lstData.remove(position);
                                    adapter.notifyItemRemoved(position);
                                    adapter.notifyDataSetChanged();
                                    App.Post(()->adapter.notifyDataSetChanged());

                                    String idCardNo = data.getJSONValue(db.getCardIdFieldName(),"");

                                    Integer index = mSelectedIndex.get(idCardNo);
                                    if(index != null){
                                        lstSelectedData.remove(index);
                                        mSelectedIndex.remove(idCardNo);
                                    }

                                    for(int i=0,ni=lstAdd.size();i<ni;++i){
                                        ESONObject eDel = lstAdd.get(i);
                                        String s = eDel.getJSONValue(db.getCardIdFieldName(),"");
                                        if(s.equals(idCardNo)) {
                                            lstAdd.remove(i);
                                            --i;
                                            --ni;
                                        }
                                    }

                                    for(int i=0,ni=lstDel.size();i<ni;++i){
                                        ESONObject eDel = lstDel.get(i);
                                        String s = eDel.getJSONValue(db.getCardIdFieldName(),"");
                                        if(s.equals(idCardNo)) {
                                            lstDel.remove(i);
                                            --i;
                                            --ni;
                                        }
                                    }

                                    lstDel.add(data);
                                }
                                updateAddBtnVisibility();
                            }

                            @Override
                            public void onFailure(String err) {}
                        });
                    });
                });

                CheckBox cbSelect = holder.getView(R.id.cb_select);
                cbSelect.setChecked(mSelectedIndex.containsKey(idCardNo));

                cbSelect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        Integer index = mSelectedIndex.get(idCardNo);
                        if(isChecked){
                            if(index!=null) return;
                            if(max>0 && lstSelectedData.size()+1>=max){
                                ToastUtils.show("最多选择"+max+"人！");
                                return;
                            }

                            mSelectedIndex.put(idCardNo, lstSelectedData.size());
                            lstSelectedData.add(data);
                        }
                        else{
                            if(!mSelectedIndex.containsKey(idCardNo)) return;
                            lstSelectedData.remove(index);
                            mSelectedIndex.remove(idCardNo);
                        }
                    }
                });
            },false);
        }
        rvData.setAdapter(adapter);

        if(mode == 0) showKeyboard(etValue);
        if(mode == 1) srlMain.autoRefresh();
        if(mode == 2) srlMain.autoRefresh();
    }

    boolean bIsSetResult = false;

    void autoSetResult(){
        if(!bIsSetResult){
            bIsSetResult = true;
            if(mode == 0){
                if(sGroupName==null || sGroupName.trim().isEmpty() || lstData.size() == 0){

                }
                else{
                    Intent intent = new Intent();
                    intent.putExtras(BundleBuilder.create("group",sGroupName).put("member",new ESONObject().putValue("data",JsonUtils.parse2Array(lstData)).toString()).build());
                    setResult(RESULT_OK,intent);
                }
            }
            else{
                Intent intent = new Intent();
                BundleBuilder builder = BundleBuilder.create().put("add",new ESONObject().putValue("data",JsonUtils.parse2Array(lstAdd)).toString()).put("del",new ESONObject().putValue("data",JsonUtils.parse2Array(lstDel)).toString());
                if(mode == 2){
                    builder.put("select", new ESONObject().putValue("data",JsonUtils.parse2Array(lstSelectedData)).toString());
                }
                intent.putExtras(builder.build());
                setResult(RESULT_OK,intent);
            }
        }
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

    @OnClick(R.id.tv_add_searching)
    void onAddSearchingClicked(){
        CommunitySearchActivity.select(this, 1, new CommunitySearchActivity.ISelectCallback() {
            @Override
            public void onSelected(List<ESONObject> lstSelected) {
                addPeople(lstSelected);
            }
        });
    }

    @OnClick(R.id.tv_add)
    void onAddClicked(){
        MemberQueryActivity.doSelect(this, 0, 2, new ESONObject(), max - lstData.size(), new CommunitySearchActivity.ISelectCallback() {
            @Override
            public void onSelected(List<ESONObject> lstSelected) {
                Log.e(TAG,"SELECTED:"+lstSelected.size());
                addPeople(lstSelected);
            }
        });
    }

    void addPeople(List<ESONObject> lstSelected){
        if(lstSelected.isEmpty()) return;
        ESONObject e = lstSelected.get(0);
        e.putValue(db.getGroupIdFieldName(),sGroupName);

        db.addPeopleToGroup(e, new ISetterListener() {
            @Override
            public void onSuccess() {
                if(mode == 0){
                    etValue.setEnabled(false);
                }

                String idCardId = e.getJSONValue(db.getCardIdFieldName(),"");
                for(int i=0,ni=lstDel.size();i<ni;++i){
                    ESONObject eDel = lstDel.get(i);
                    String s = eDel.getJSONValue(db.getCardIdFieldName(),"");
                    if(s.equals(idCardId)) {
                        lstDel.remove(i);
                        --i;
                        --ni;
                    }
                }

                for(int i=0,ni=lstAdd.size();i<ni;++i){
                    ESONObject eDel = lstAdd.get(i);
                    String s = eDel.getJSONValue(db.getCardIdFieldName(),"");
                    if(s.equals(idCardId)) {
                        lstAdd.remove(i);
                        --i;
                        --ni;
                    }
                }

                lstAdd.add(e);
                lstData.add(e);
                adapter.notifyItemInserted(lstData.size()-1);
                lstSelected.remove(0);
                addPeople(lstSelected);
                updateAddBtnVisibility();
            }

            @Override
            public void onFailure(String err) {
                ToastUtils.show("成员添加失败："+err);
                int retryTime = e.getJSONValue("__retryTime_1",0);
                e.putValue("__retryTime_1",retryTime+1);
                if(retryTime<3){
                    App.PostDelayed(()->addPeople(lstSelected),300L);
                }
                else{
                    lstSelected.remove(0);
                    addPeople(lstSelected);
                }
            }
        });
    }

    @Override
    protected boolean hasRefreshBar() {
        return true;
    }

    void updateAddBtnVisibility(){
        boolean bShouldVisible = mode == 1 || sGroupName!=null && lstData.size()<Template.getCurrentTemplate().getDatabaseSetting().getGroupMemberNum();
        tvAdd.setVisibility(bShouldVisible?View.VISIBLE:View.INVISIBLE);
        tvAddSearching.setVisibility(bShouldVisible?View.VISIBLE:View.INVISIBLE);
    }

    NucleicAcidGroupingDatabase db = Template.getCurrentTemplate().getNucleicAcidGroupingDatabase();
    @Override
    public void onRefresh(@NonNull RefreshLayout refreshLayout) {
        db.getGroupInfo(sGroupName, new IGetterListener() {
            @Override
            public void onSuccess(ESONArray data) {
                Log.e(TAG,""+data.toString());
                if(data.length() == 0 && mode == 0){
                    ToastUtils.show("当前分组名称可用！");
                }
                lstData.clear();
                lstData.addAll(JsonUtils.parseToList(data));
                updateAddBtnVisibility();
                adapter.notifyDataSetChanged();
                refreshLayout.finishRefresh(true);
            }

            @Override
            public void onFailure(String err) {
                ToastUtils.show(err);
                refreshLayout.finishRefresh(false);
            }
        });
    }

}
