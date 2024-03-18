package com.dreaming.hscj.activity.system;

import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dreaming.hscj.App;
import com.dreaming.hscj.R;
import com.dreaming.hscj.activity.community.input.CommunityInputActivity;
import com.dreaming.hscj.activity.community.manage.CommunityMemberDetailActivity;
import com.dreaming.hscj.activity.community.search.CommunitySearchActivity;
import com.dreaming.hscj.activity.nucleic_acid.grouping.NAGroupInDetailActivity;
import com.dreaming.hscj.base.BaseActivity;
import com.dreaming.hscj.core.BundleBuilder;
import com.dreaming.hscj.core.EasyAdapter.EasyAdapter;
import com.dreaming.hscj.core.TTSEngine;
import com.dreaming.hscj.core.ThreadPoolProvider;
import com.dreaming.hscj.core.ViewInjector;
import com.dreaming.hscj.dialog.DialogManager;
import com.dreaming.hscj.template.DataParser;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.template.database.impl.NoneGroupingDatabase;
import com.dreaming.hscj.template.database.impl.NucleicAcidGroupingDatabase;
import com.dreaming.hscj.template.database.impl.UserOverallDatabase;
import com.dreaming.hscj.template.database.wrapper.DatabaseConfig;
import com.dreaming.hscj.template.database.wrapper.ICountListener;
import com.dreaming.hscj.template.database.wrapper.IDeleteListener;
import com.dreaming.hscj.template.database.wrapper.IGetterListener;
import com.dreaming.hscj.utils.ToastUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

/**
 * 社区人员查询
 *
 * 支持批量选择，设置最高选择数量
 * 支持批量删除
 * 支持修改记录
 *
 */
public class MemberQueryActivity extends BaseActivity {
    private static final String TAG = MemberQueryActivity.class.getSimpleName();
    //region 静态调用
    public static void doQuery(BaseActivity activity, int mode, int group, ESONObject eQuery){//查 改 删
        activity.startActivity(MemberQueryActivity.class, BundleBuilder.create().put("type",0).put("mode", mode).put("group", group).put("query", eQuery.toString()).build());
    }

    public static void doSelect(BaseActivity activity, int mode, int group, ESONObject eQuery, int maxSelection, CommunitySearchActivity.ISelectCallback callback){//查 选
        activity.startActivityForResult(
                MemberQueryActivity.class,
                BundleBuilder
                        .create()
                        .put("type",1)
                        .put("mode", mode)
                        .put("group", group)
                        .put("query", eQuery.toString())
                        .put("max",maxSelection)
                        .build(),
                4003,
                (resultCode, data) -> {

            String result = data==null?"":data.getStringExtra("data");
            if(result == null) result = "";
            ESONObject e = new ESONObject(result);
            ESONArray a = e.getJSONValue("data",new ESONArray());
            List<ESONObject> l = new ArrayList<>();

            for(int i=0,ni=a.length();i<ni;++i){
                l.add(a.getArrayValue(i,new ESONObject()));
            }
            callback.onSelected(l);
        });
    }
    //endregion



    //region Activity
    @Override
    public int getContentViewResId() {
        return R.layout.activity_member_query;
    }

    @Override
    protected void onBindView(View vContent) {
        ButterKnife.bind(this,vContent);
    }

    @BindView(R.id.part_title_query_all)
    CardView cvQueryAllTitle;
    @BindView(R.id.part_title_query_group)
    CardView cvQueryGroupTitle;
    @BindView(R.id.part_title_select_all)
    CardView cvSelectAllTitle;
    @BindView(R.id.part_title_select_group)
    CardView cvSelectGroupTitle;

    @BindView(R.id.tv_finish)
    TextView tvFinish;

    EasyAdapter adapterQueryAll;
    EasyAdapter adapterQueryGroup;
    EasyAdapter adapterSelectAll;
    EasyAdapter adapterSelectGroup;
    EasyAdapter pAdapter;

    final List<ESONObject> lstData = new ArrayList<>();

    private int startMode;//0:Query 1:Select
    private int queryMode;
    private int queryGroup;
    private ESONObject eQueryCondition = new ESONObject();
    private int maxSelection;//最大选择数量 <1表示不限制

    private List<ESONObject> lstSelections = new ArrayList<>();
    private Map<String,Integer> mSelections = new HashMap<>();

    @Override
    public void initView() {
        startMode       = getIntent().getIntExtra("type",0);
        queryMode       = getIntent().getIntExtra("mode",0);
        queryGroup      = getIntent().getIntExtra("group",0);
        String query    = getIntent().getStringExtra("query");
        eQueryCondition = new ESONObject(query == null ? "" : query);
        maxSelection    = getIntent().getIntExtra("max",0);

        if(startMode == 0){//查询
            if(queryGroup == 1){
                cvQueryGroupTitle.setVisibility(View.VISIBLE);
            }
            else{
                cvQueryAllTitle.setVisibility(View.VISIBLE);
            }
            setCenterText("成员查询");
        }
        else{//选择
            if(queryGroup == 1){
                cvSelectGroupTitle.setVisibility(View.VISIBLE);
            }
            else{
                cvSelectAllTitle.setVisibility(View.VISIBLE);
                if(queryGroup==2){
                    LinearLayout ll = (LinearLayout) cvSelectAllTitle.getChildAt(0);
                    ll.getChildAt(3).setVisibility(View.GONE);
                }
                else{//0
                    setRightText("新增成员");
                    tvTitleRight.setOnClickListener(v -> {
                        CommunityInputActivity.doInputOneAndReturn(this, new CommunityInputActivity.IInputCallback() {
                            @Override
                            public void onInputSuccess() {
                                count();
                                queryAll(0);
                            }
                        });
                    });
                }
            }
            setCenterText("成员选择");
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

        setOnKeyBoardLayoutStateChangeListener(new OnKeyBoardLayoutStateChangeListener() {
            @Override
            public void onKeyBoardShow(int keyBoardHeight, int gapHeight) {
                ((ConstraintLayout.LayoutParams)clJumping.getLayoutParams()).bottomMargin = keyBoardHeight-gapHeight;
                clJumping.setVisibility(View.GONE);
                clJumping.setVisibility(View.VISIBLE);
                etPage.requestFocus();
            }

            @Override
            public void onKeyBoardHide() {
                ((ConstraintLayout.LayoutParams)clJumping.getLayoutParams()).bottomMargin = 0;
                clJumping.setVisibility(View.GONE);
                clJumping.setVisibility(View.VISIBLE);
            }
        });

        if(startMode == 0){//查询
            if(queryGroup == 1){//序号 组号 姓名 身份号 详情 操作
                adapterQueryGroup = new EasyAdapter(this, R.layout.recy_query_part_title_query_group, lstData, (EasyAdapter.IEasyAdapter<ESONObject>) (holder, data, position) -> {
                    holder.setText(R.id.tv_index     ,String.valueOf(position+1+(currentPage*pageLength)));
                    holder.setText(R.id.tv_group_id  ,data.getJSONValue(db3.getGroupIdFieldName(),""));
                    holder.setText(R.id.tv_name      ,data.getJSONValue(db3.getNameFiledName()   ,""));
                    holder.setText(R.id.tv_id_card_no,data.getJSONValue(db3.getCardIdFieldName() ,""));
                    holder.setOnClickListener(R.id.tv_name, v-> TTSEngine.speakChinese(data.getJSONValue(db3.getNameFiledName()   ,"")));

                    holder.setOnClickListener(R.id.tv_detail,v -> NAGroupInDetailActivity.shownMember(MemberQueryActivity.this, data, new NAGroupInDetailActivity.IMemberStateChangeListener() {
                        @Override
                        public void onDelete() {
                            lstData.remove(position);
                            pAdapter.notifyDataSetChanged();
                        }

                        @Override
                        public void onSave(ESONObject data) {
                            lstData.remove(position);
                            lstData.add(position,data);
                            pAdapter.notifyDataSetChanged();
                        }

                        @Override
                        public void onCancel() {}
                    }));

                    holder.setOnClickListener(R.id.tv_delete,v -> {
                        DialogManager.showAlertDialog(MemberQueryActivity.this,"提示","确定要删除该成员吗？",null,v1 -> {
                            deleteGroupIn(data.getJSONValue(db3.getCardIdFieldName() ,""),data.getJSONValue(db3.getGroupIdFieldName(),""),new IDeleteListener() {
                                @Override
                                public void onSuccess(int count) {
                                    lstData.remove(position);
                                    pAdapter.notifyDataSetChanged();
                                }

                                @Override
                                public void onFailure(String err) {
                                    ToastUtils.show("删除失败："+err);
                                }
                            });
                        });
                    });

                });
                rvData.setAdapter(pAdapter=adapterQueryGroup);
            }
            else{//序号 姓名 身份号 详情 操作
                adapterQueryAll = new EasyAdapter(this,  R.layout.recy_query_part_title_query_all, lstData, (EasyAdapter.IEasyAdapter<ESONObject>) (holder, data, position) -> {
                    holder.setText(R.id.tv_index     ,String.valueOf(position+1+(currentPage*pageLength)));
                    holder.setText(R.id.tv_name      ,data.getJSONValue(db2.getNameFiledName(),""));
                    holder.setText(R.id.tv_id_card_no,data.getJSONValue(db2.getIdFieldName()  ,""));
                    holder.setOnClickListener(R.id.tv_name, v-> TTSEngine.speakChinese(data.getJSONValue(db3.getNameFiledName()   ,"")));

                    if(queryGroup == 2){
                        holder.setOnClickListener(R.id.tv_detail,v -> CommunityMemberDetailActivity.shownMember(MemberQueryActivity.this,data.getJSONValue(db2.getIdFieldName()  ,"")));
                    }
                    else{
                        holder.setOnClickListener(R.id.tv_detail,v -> CommunityMemberDetailActivity.shownMember(MemberQueryActivity.this, data, new CommunityMemberDetailActivity.IMemberStateChangeListener() {
                            @Override
                            public void onDelete() {
                                lstData.remove(position);
                                pAdapter.notifyDataSetChanged();
                            }

                            @Override
                            public void onSave(ESONObject data) {
                                lstData.remove(position);
                                lstData.add(position,data);
                                pAdapter.notifyDataSetChanged();
                            }

                            @Override
                            public void onCancel() {}
                        }));
                    }


                    holder.setOnClickListener(R.id.tv_delete,v -> {
                        DialogManager.showAlertDialog(MemberQueryActivity.this,"警告","此操作会删除该成员所有相关的数据库信息，确定要删除吗？",null,v1 -> {
                            deleteAll(data.getJSONValue(db2.getIdFieldName()  ,""),new IDeleteListener() {
                                @Override
                                public void onSuccess(int count) {
                                    lstData.remove(position);
                                    pAdapter.notifyDataSetChanged();
                                }

                                @Override
                                public void onFailure(String err) {
                                    ToastUtils.show("删除失败："+err);
                                }
                            });
                        });
                    });
                });
                rvData.setAdapter(pAdapter=adapterQueryAll);
            }
        }
        else {//选择
            if(queryGroup == 1){//勾选 序号 组号 姓名 身份号 详情
                adapterSelectGroup = new EasyAdapter(this,  R.layout.recy_query_part_title_select_group, lstData, (EasyAdapter.IEasyAdapter<ESONObject>) (holder, data, position) -> {
                    holder.setText(R.id.tv_index     ,String.valueOf(position+1+(currentPage*pageLength)));
                    holder.setText(R.id.tv_group_id  ,data.getJSONValue(db3.getGroupIdFieldName(),""));
                    holder.setText(R.id.tv_name      ,data.getJSONValue(db3.getNameFiledName()   ,""));
                    holder.setText(R.id.tv_id_card_no,data.getJSONValue(db3.getCardIdFieldName() ,""));

                    String idCardNo = data.getJSONValue(db3.getCardIdFieldName(),"");
                    holder.setOnClickListener(R.id.tv_name, v-> TTSEngine.speakChinese(data.getJSONValue(db3.getNameFiledName()   ,"")));

                    holder.setOnClickListener(R.id.tv_detail,v -> NAGroupInDetailActivity.shownMember(MemberQueryActivity.this, data));

                    CheckBox cbSelect = holder.getView(R.id.cb_select);
                    cbSelect.setChecked(mSelections.containsKey(idCardNo));
                    cbSelect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            Integer index = mSelections.get(idCardNo);
                            if(isChecked) {
                                if(index != null) return;
                                if(maxSelection>0 && lstSelections.size()>=maxSelection){
                                    ToastUtils.show("最多选择"+maxSelection+"人！");
                                    cbSelect.setChecked(false);
                                }
                                else{
                                    mSelections.put(idCardNo,lstSelections.size());
                                    lstSelections.add(data);
                                }
                            }
                            else {
                                if(index == null) return;
                                mSelections.remove(idCardNo);
                                lstSelections.remove(index);
                            }

                            boolean bShouldVisible = !lstSelections.isEmpty();
                            if(bShouldVisible ^ tvFinish.getVisibility()==View.VISIBLE){
                                tvFinish.setVisibility(bShouldVisible?View.VISIBLE:View.INVISIBLE);
                            }
                        }
                    });
                });
                rvData.setAdapter(pAdapter=adapterSelectGroup);
            }
            else{//勾选 序号 姓名 身份号 详情
                adapterSelectAll = new EasyAdapter(this,  R.layout.recy_query_part_title_select_all, lstData, (EasyAdapter.IEasyAdapter<ESONObject>) (holder, data, position) -> {
                    holder.setText(R.id.tv_index     ,String.valueOf(position+1+(currentPage*pageLength)));
                    holder.setText(R.id.tv_name      ,data.getJSONValue(db2.getNameFiledName(),""));
                    holder.setText(R.id.tv_id_card_no,data.getJSONValue(db2.getIdFieldName()  ,""));

                    holder.setOnClickListener(R.id.tv_name, v -> TTSEngine.speakChinese(data.getJSONValue(db3.getNameFiledName()   ,"")));

                    String idCardNo = data.getJSONValue(db2.getIdFieldName(),"");

                    if(queryGroup == 2){
                        holder.setOnClickListener(R.id.tv_detail,v -> CommunityMemberDetailActivity.shownMember(MemberQueryActivity.this,data.getJSONValue(db2.getIdFieldName()  ,"")));
                    }
                    else {
                        holder.setOnClickListener(R.id.tv_detail, v -> CommunityMemberDetailActivity.shownMember(MemberQueryActivity.this, data));
                    }

                    CheckBox cbSelect = holder.getView(R.id.cb_select);
                    cbSelect.setChecked(mSelections.containsKey(idCardNo));
                    cbSelect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            Integer index = mSelections.get(idCardNo);
                            if(isChecked) {
                                if(index != null) return;

                                if(maxSelection>0 && lstSelections.size()>=maxSelection){
                                    ToastUtils.show("最多选择"+maxSelection+"人！");
                                }
                                else{
                                    mSelections.put(idCardNo,lstSelections.size());
                                    lstSelections.add(data);
                                }
                            }
                            else {
                                if(index == null) return;
                                mSelections.remove(idCardNo);
                                lstSelections.remove(index);
                            }

                            boolean bShouldVisible = !lstSelections.isEmpty();
                            tvFinish.setVisibility(bShouldVisible?View.VISIBLE:View.INVISIBLE);
                        }
                    });
                });
                rvData.setAdapter(pAdapter=adapterSelectAll);
            }
        }

        count();

        etPage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    int page = Integer.parseInt(s.toString());
                    if(page<1 || page>pageNum){
                        ToastUtils.show(String.format("页数只能在%d~%d之间",1,pageNum));
                    }
                } catch (Exception e) {}
            }
        });
        tvJump.setOnClickListener(v->{
            try {
                int page = Integer.parseInt(etPage.getText().toString());
                if(page<0 || page>pageNum){
                    ToastUtils.show(String.format("页数只能在%d~%d之间",1,pageNum));
                    return;
                }
                queryPage(page-1);
            } catch (Exception e) {
                ToastUtils.show("请输入整数！");
            }
        });
        tvLastPage.setOnClickListener(v -> queryPage(currentPage - 1));
        tvNextPage.setOnClickListener(v -> queryPage(currentPage + 1));
    }

    @BindView(R.id.ev_page)
    EditText etPage;
    @BindView(R.id.tv_last_page)
    TextView tvLastPage;
    @BindView(R.id.tv_next_page)
    TextView tvNextPage;
    @BindView(R.id.tv_jump)
    TextView tvJump;
    @BindView(R.id.part_jumping)
    ConstraintLayout clJumping;
    @BindView(R.id.rv_data)
    RecyclerView rvData;
    @BindView(R.id.tv_data_empty)
    TextView tvDataEmpty;

    int currentPage = 0;
    int totalPage   = 0;
    int pageNum     = 0;
    void updateRvData(List<ESONObject> data, int page, int totalPage, int pageNum){
        currentPage = page;
        this.totalPage = totalPage;
        this.pageNum = pageNum;

        if(page!=0) etPage.setText(String.valueOf(page+1));

        tvDataEmpty.setVisibility(data==null||data.isEmpty()?View.VISIBLE:View.INVISIBLE);
        tvLastPage.setEnabled(page > 0);
        tvNextPage.setEnabled(currentPage<pageNum-1);
        lstData.clear();
        lstData.addAll(data);
        pAdapter.notifyDataSetChanged();
    }


    @OnClick(R.id.tv_finish)
    void onFinishClicked(){
        if(lstSelections.size() == 0) return;

        ESONArray a = new ESONArray();
        for(int i=0,ni=lstSelections.size();i<ni;++i){
            a.putValue(lstSelections.get(i));
        }

        setResult(RESULT_OK,new Intent(){{putExtras(BundleBuilder.create("data",new ESONObject().putValue("data",a).toString()).build());}});
        finish();
    }
    //endregion



    //region 数据库查询

    UserOverallDatabase         db2 = Template.getCurrentTemplate().getUserOverallDatabase();
    NucleicAcidGroupingDatabase db3 = Template.getCurrentTemplate().getNucleicAcidGroupingDatabase();
    NoneGroupingDatabase        db4 = Template.getCurrentTemplate().getNoneGroupingDatabase();


    int pageLength = 50;
    int countAll = 0;
    int pageSizeAll ;
    void countAll(){
        ICountListener listener = new ICountListener() {
            @Override
            public void onSuccess(int count) {
                countAll = count;
                pageSizeAll = countAll/pageLength + (countAll%pageLength==0?0:1);
                Log.e(TAG,"count->"+count);
                Log.e(TAG,"pageSizeAll->"+pageSizeAll);
                queryAll(0);
            }

            @Override
            public void onFailure(String err) {
                ToastUtils.show(err);
            }
        };
        if(queryMode == 0){
            db2.count(eQueryCondition,listener);
        }
        else{
            db2.countSearch(eQueryCondition,listener);
        }
    }
    void queryAll(int page){
        IGetterListener listener = new IGetterListener() {
            @Override
            public void onSuccess(ESONArray data) {
                ThreadPoolProvider.getFixedThreadPool().execute(()->{
                    List<ESONObject> lstData = new ArrayList<>();
                    for(int i=0,ni=data.length();i<ni;++i){
                        lstData.add(DataParser.parseDatabaseToShown(DatabaseConfig.TYPE_USER_OVERALL,data.getArrayValue(i,new ESONObject())));
                    }
                    App.Post(()->updateRvData(lstData,page,countAll,pageSizeAll));
                });
            }

            @Override
            public void onFailure(String err) {
                ToastUtils.show(err);
            }
        };

        if(queryMode == 0){
            db2.query(eQueryCondition,page,pageLength,listener);
        }
        else{
            db2.search(eQueryCondition,page,pageLength,listener);
        }
    }
    void deleteAll(String idCardNo,IDeleteListener listener){
        db2.deletePeople(new ESONObject().putValue(db2.getIdFieldName(), idCardNo), listener);
    }

    int countGroupIn;
    int pageSizeGroupIn;
    void countGroupIn(){
        ICountListener listener = new ICountListener() {
            @Override
            public void onSuccess(int count) {
                countGroupIn = count;
                pageSizeGroupIn = countGroupIn/pageLength + (countGroupIn%pageLength==0?0:1);
                queryGroupIn(0);
            }

            @Override
            public void onFailure(String err) {
                ToastUtils.show(err);
            }
        };
        if(queryMode == 0){
            db3.count(eQueryCondition,listener);
        }
        else{
            db3.countSearch(eQueryCondition,listener);
        }
    }

    void queryGroupIn(int page){
        IGetterListener listener = new IGetterListener() {
            @Override
            public void onSuccess(ESONArray data) {
                ThreadPoolProvider.getFixedThreadPool().execute(()->{
                    List<ESONObject> lstData = new ArrayList<>();
                    for(int i=0,ni=data.length();i<ni;++i){
                        lstData.add(DataParser.parseDatabaseToShown(DatabaseConfig.TYPE_NA_GROUPING,data.getArrayValue(i,new ESONObject())));
                    }
                    App.Post(()->updateRvData(lstData,page,countGroupIn,pageSizeGroupIn));
                });
            }

            @Override
            public void onFailure(String err) {
                ToastUtils.show(err);
            }
        };

        if(queryMode == 0){
            db3.query(eQueryCondition,page,pageLength,listener);
        }
        else{
            db3.search(eQueryCondition,page,pageLength,listener);
        }
    }
    void deleteGroupIn(String idCardNo,String gId, IDeleteListener listener){
        db3.deletePeopleWithGroup(idCardNo, gId, listener);
    }


    int countGroupOut;
    int pageSizeGroupOut;
    void countGroupOut(){
        ICountListener listener = new ICountListener() {
            @Override
            public void onSuccess(int count) {
                countGroupOut = count;
                pageSizeGroupOut = countGroupOut/pageLength + (countGroupOut%pageLength==0?0:1);
                queryGroupOut(0);
            }

            @Override
            public void onFailure(String err) {
                ToastUtils.show(err);
            }
        };
        if(queryMode == 0){
            db4.count(eQueryCondition,listener);
        }
        else{
            db4.countSearch(eQueryCondition,listener);
        }
    }
    void queryGroupOut(int page){
        IGetterListener listener = new IGetterListener() {
            @Override
            public void onSuccess(ESONArray data) {
                ThreadPoolProvider.getFixedThreadPool().execute(()->{
                    List<ESONObject> lstData = new ArrayList<>();
                    for(int i=0,ni=data.length();i<ni;++i){
                        lstData.add(DataParser.parseDatabaseToShown(DatabaseConfig.TYPE_NA_NONE_GROUP,data.getArrayValue(i,new ESONObject())));
                    }
                    App.Post(()->updateRvData(lstData,page,countGroupOut,pageSizeGroupOut));
                });
            }

            @Override
            public void onFailure(String err) {
                ToastUtils.show(err);
            }
        };

        if(queryMode == 0){
            db4.query(eQueryCondition,page,pageLength,listener);
        }
        else{
            db4.search(eQueryCondition,page,pageLength,listener);
        }
    }

    void count(){
        switch (queryGroup){
            case 0:
                countAll();
                break;
            case 1:
                countGroupIn();
                break;
            case 2:
                countGroupOut();
                break;
        }
    }

    void queryPage(int page){
        switch (queryGroup){
            case 0:
                queryAll(page);
                break;
            case 1:
                queryGroupIn(page);
                break;
            case 2:
                queryGroupOut(page);
                break;
        }
    }
    //endregion


}
