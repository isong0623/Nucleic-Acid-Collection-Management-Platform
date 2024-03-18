package com.dreaming.hscj.activity.community.manage;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dreaming.hscj.App;
import com.dreaming.hscj.Constants;
import com.dreaming.hscj.R;
import com.dreaming.hscj.base.contract.BaseMVPActivity;
import com.dreaming.hscj.core.EasyAdapter.EasyAdapter;
import com.dreaming.hscj.dialog.DialogManager;
import com.dreaming.hscj.dialog.loading.LoadingDialog;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.template.database.DatabaseTemplate;
import com.dreaming.hscj.template.database.wrapper.IDeleteListener;
import com.dreaming.hscj.utils.ToastUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import priv.songxusheng.easyjson.ESONObject;

public class CommunityManageActivity extends BaseMVPActivity<CommunityManagePresenter> implements ICommunityManageContract.View{

    @Override
    public int getContentViewResId() {
        return R.layout.activity_community_manage;
    }

    @Override
    protected void onBindView(View vContent) {
        ButterKnife.bind(this,vContent);
    }

    @BindView(R.id.part_database_info)
    CardView cvDatabaseInfo;
    @BindView(R.id.part_database_total)
    CardView cvDatabaseTotal;

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

    TextView tvDatabaseTotal;

    @BindView(R.id.rv_data)
    RecyclerView rvData;
    List<ESONObject> lstData = new ArrayList<>();

    @BindView(R.id.tv_data_empty)
    TextView tvDataEmpty;

    EasyAdapter adapter;
    @Override
    public void initView() {
        setCenterText("社区成员管理");

        TextView tvCurrentDatabaseName = cvDatabaseInfo.findViewById(R.id.tv_name);
        tvCurrentDatabaseName.setText("当前数据库：");
        tvCurrentDatabaseName.setWidth(dp2px(150f));
        tvCurrentDatabaseName.invalidate();
        TextView tvCurrentDatabaseValue= cvDatabaseInfo.findViewById(R.id.tv_value);
        ESONObject object = Constants.DBConfig.getSelectedDatabase();
        String sTownName    = object.getJSONValue("townName"   ,"");
        String sVillageName = object.getJSONValue("villageName","");
        tvCurrentDatabaseValue.setText(String.format("%s/%s/%s", Template.getCurrentTemplate().getDatabaseSetting().getRegionName(),sTownName,sVillageName));

        TextView tvDatabaseTotalName = cvDatabaseTotal.findViewById(R.id.tv_name);
        tvDatabaseTotalName.setText("总人数：");
        tvDatabaseTotalName.setWidth(dp2px(150f));
        tvDatabaseTotalName.invalidate();
        tvDatabaseTotal = cvDatabaseTotal.findViewById(R.id.tv_value);
        tvDatabaseTotal.setText("?");
        mPresenter.queryCount();

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
        rvData.setAdapter(adapter = new EasyAdapter(this, R.layout.recy_community_member_manage, lstData, (EasyAdapter.IEasyAdapter<ESONObject>) (holder, data, position) -> {
            holder.setText(R.id.tv_index,String.valueOf(currentPage*100 + position + 1));
            holder.setText(R.id.tv_name,data.getJSONValue(mPresenter.sName,""));
            holder.setText(R.id.tv_id_card_no,data.getJSONValue(mPresenter.sIdCardNo,""));
            holder.setVisibility(R.id.tv_detail,View.VISIBLE);
            holder.setVisibility(R.id.tv_delete,View.VISIBLE);
            holder.setOnClickListener(R.id.tv_detail,v->{
                CommunityMemberDetailActivity.shownMember(this, data, new CommunityMemberDetailActivity.IMemberStateChangeListener() {
                    @Override
                    public void onDelete() {
                        mPresenter.queryCount();
                        lstData.remove(position);
                        adapter.notifyItemRemoved(position);
                    }

                    @Override
                    public void onSave(ESONObject data) {
                        lstData.remove(position);
                        lstData.add(position,data);
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancel() {}
                });
            });
            holder.setOnClickListener(R.id.tv_delete,v->{
                DialogManager.showAlertDialog(CommunityManageActivity.this,"提示","确定要删除此条记录？",null,v1->{
                    mPresenter.delete(data.getJSONValue(mPresenter.sIdCardNo, ""), position, new IDeleteListener() {
                        @Override
                        public void onSuccess(int count) {
                            if(count>0){
                                App.Post(()->{
                                    mPresenter.queryCount();
                                    lstData.remove(position);
                                    adapter.notifyDataSetChanged();
                                    ToastUtils.show("删除成功！");
                                });
                            }
                            else{
                                App.Post(()-> ToastUtils.show("删除失败！"));
                            }
                        }
                        @Override
                        public void onFailure(String err) {
                            App.Post(()-> ToastUtils.show("删除失败！"));
                        }
                    });
                });
            });
        },false));
        mPresenter.queryPage(0);

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
                mPresenter.queryPage(page-1);
            } catch (Exception e) {
                ToastUtils.show("请输入整数！");
            }
        });
        tvLastPage.setOnClickListener(v -> mPresenter.queryPage(currentPage - 1));
        tvNextPage.setOnClickListener(v -> mPresenter.queryPage(currentPage + 1));
    }

    int total = 0;
    int pageNum = 0;
    @Override
    public void updateCount(int count) {
        total = count;
        pageNum = (total/100) + (total%100==0 ?0 :1);
        tvDatabaseTotal.setText(String.format("共%d条记录",count));
    }

    int currentPage;
    @Override
    public void updateData(int page, List<ESONObject> data) {
        currentPage = page;
        if(data!=null && !data.isEmpty()){
            etPage.setText(String.valueOf(page+1));
        }
        tvDataEmpty.setVisibility(data==null||data.isEmpty()?View.VISIBLE:View.INVISIBLE);
        tvLastPage.setEnabled(page > 0);
        tvNextPage.setEnabled(currentPage<pageNum-1);
        lstData.clear();
        lstData.addAll(data);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void showLoadingDialog() {
        LoadingDialog.showDialog("Query Data",this);
    }

    @Override
    public void hideLoadingDialog() {
        LoadingDialog.dismissDialog("Query Data");
    }
}
