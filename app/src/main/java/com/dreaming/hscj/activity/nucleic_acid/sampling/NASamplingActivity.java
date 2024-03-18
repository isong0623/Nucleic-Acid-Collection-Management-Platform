package com.dreaming.hscj.activity.nucleic_acid.sampling;

import android.graphics.Point;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dreaming.hscj.App;
import com.dreaming.hscj.R;
import com.dreaming.hscj.activity.system.MemberQueryActivity;
import com.dreaming.hscj.activity.system.ZxingActivity;
import com.dreaming.hscj.activity.community.manage.CommunityMemberDetailActivity;
import com.dreaming.hscj.activity.community.search.CommunitySearchActivity;
import com.dreaming.hscj.activity.nucleic_acid.grouping.NAGroupingActivity;
import com.dreaming.hscj.activity.nucleic_acid.searching.LocalSamplingDialog;
import com.dreaming.hscj.base.contract.BaseMVPActivity;
import com.dreaming.hscj.core.EasyAdapter.EasyAdapter;
import com.dreaming.hscj.core.ocr.IDCardRecognizer;
import com.dreaming.hscj.core.TTSEngine;
import com.dreaming.hscj.core.ThreadPoolProvider;
import com.dreaming.hscj.dialog.DialogManager;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.template.api.ApiProvider;
import com.dreaming.hscj.template.api.ParamSetter;
import com.dreaming.hscj.template.api.impl.Api;
import com.dreaming.hscj.template.api.wrapper.ApiParam;
import com.dreaming.hscj.template.database.wrapper.ISetterListener;
import com.dreaming.hscj.utils.DensityUtils;
import com.dreaming.hscj.utils.JsonUtils;
import com.dreaming.hscj.utils.ToastUtils;
import com.dreaming.hscj.widget.ClearEditView;
import com.scwang.smart.refresh.layout.api.RefreshLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import priv.songxusheng.easydialog.EasyDialog;
import priv.songxusheng.easydialog.EasyDialogHolder;
import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

public class NASamplingActivity extends BaseMVPActivity<NASamplingPresenter> implements INASamplingContract.View{
    private static final String TAG = NASamplingActivity.class.getSimpleName();
    @Override
    public int getContentViewResId() {
        return R.layout.activity_nucleic_acid_sampling;
    }

    @Override
    protected void onBindView(View vContent) {
        ButterKnife.bind(this,vContent);
    }

    @BindView(R.id.rv_scan)
    RecyclerView rvScan;
    @BindView(R.id.tv_add_ocr)
    TextView tvAddOcr;
    @BindView(R.id.tv_add_searching)
    TextView tvAddSearching;
    @BindView(R.id.tv_add_grouping)
    TextView tvAddGrouping;
    @BindView(R.id.tv_add_community)
    TextView tvAddCommunity;
    @BindView(R.id.ev_barcode)
    ClearEditView evBarcode;

    EasyAdapter adapter;
    List<ESONObject> lstData = new ArrayList<>();

    @BindView(R.id.tv_data_empty)
    TextView tvEmpty;

    @BindView(R.id.part_search)
    LinearLayout llPartSearch;
    @BindView(R.id.ev_id_no)
    EditText etIdNo;
    @BindView(R.id.part_add_mini)
    ConstraintLayout clPartAddMini;
    @BindView(R.id.tv_plan_num_mini)
    TextView tvPlanNumMini;

    @BindView(R.id.tv_plan_num)
    TextView tvPlanNum;
    @BindView(R.id.v_point)
    View vPoint;
    @BindView(R.id.iv_sampling_plan)
    ImageView ivSamplingPlan;
    @BindView(R.id.tv_symbol_add)
    TextView tvSymbolAdd;

    @BindView(R.id.iv_expansion)
    ImageView ivExpansion;

    int iKeyboardHeight = 0;
    int iGapHeight      = 0;
    OnKeyBoardLayoutStateChangeListener onKeyBoardLayoutStateChangeListener = new OnKeyBoardLayoutStateChangeListener() {
        @Override
        public void onKeyBoardShow(int keyBoardHeight, int gapHeight) {
            iKeyboardHeight = keyBoardHeight;
            iGapHeight      = gapHeight;
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) llPartSearch.getLayoutParams();
            params.bottomToTop = -1;
            params.bottomToBottom = 0;
            params.bottomMargin = keyBoardHeight - gapHeight + dp2px(4);
            llPartSearch.setLayoutParams(params);
            if(lstSelected.size()>0){
                clPartAddMini.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onKeyBoardHide() {
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) llPartSearch.getLayoutParams();
            params.bottomToTop = R.id.part_add;
            params.bottomToBottom = -1;
            params.bottomMargin = dp2px(20f);
            llPartSearch.setLayoutParams(params);
            clPartAddMini.setVisibility(View.INVISIBLE);
        }
    };
    @Override
    public void initView() {
        setCenterText("核酸在线采样");
        setRightText("查询");
        tvTitleRight.setOnClickListener(v -> LocalSamplingDialog.showLocalSamplingDialog(this));
        evBarcode.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus) {
                    hideKeyboard();
                    mPresenter.setSearchedBarcode(evBarcode.getText().toString());
                    mPresenter.requestTestTubeInfo();
                }
            }
        });
        evBarcode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void afterTextChanged(Editable s) {
                mPresenter.setChangedBarcode(s.toString());
            }
        });
        srlMain.setEnableLoadMore(false);
        rvScan.setLayoutManager(new LinearLayoutManager(this){
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                try {
                    super.onLayoutChildren( recycler, state );
                } catch (IndexOutOfBoundsException e) {
                    e.printStackTrace();
                }
            }
        });
        rvScan.getRecycledViewPool().setMaxRecycledViews(0,0);
        rvScan.setAdapter(adapter = new EasyAdapter(this, R.layout.recy_sampling, lstData, (EasyAdapter.IEasyAdapter<ESONObject>) (holder, data, position) -> {
            LinearLayout llContent = holder.getView(R.id.ll_shown);
            if(llContent.getChildCount()==0){
                List<ApiParam> lst = Template.getCurrentTemplate().apiOf(2).getResponse().getFields();
                for(int i=0,ni=lst.size();i<ni;++i){
                    ApiParam p = lst.get(i);

                    String key = lst.get(i).getName();
                    String shown = "";
                    Object value = null;
                    try { value = data.get(key); } catch (Exception e) { }
                    if(value == null){
                        shown = p.getDefaultValue();
                        if(shown == null){
                            shown = "";
                        }
                    }
                    else{
                        shown = p.getter(value.toString());
                    }
                    CardView cvRoot = (CardView) LayoutInflater.from(this).inflate(R.layout.view_shown,null,false);
                    TextView tvName = cvRoot.findViewById(R.id.tv_name);
                    tvName.setText(p.getDescription());
                    TextView tvValue= cvRoot.findViewById(R.id.tv_value);
                    tvValue.setText(shown);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp2px(26f));
                    params.topMargin = dp2px(1);
                    llContent.addView(cvRoot,params);
                }
            }
            holder.setText(R.id.tv_index,String.valueOf(position+1));
            holder.setOnClickListener(R.id.tv_delete,v -> {
                DialogManager.showAlertDialog(this,"提示","确定删除该成员的采样记录吗？",null,v1 -> {
                    ApiProvider.requestDeleteSamplingRecordWithTubeNo(evBarcode.getText().toString(), new ParamSetter(Api.TYPE_NC_PEOPLE_INFO, data), new ApiProvider.ISamplingRecordDeleteListener() {
                        @Override
                        public void onSuccess() {
                            ToastUtils.show("删除成功！");
                            lstData.remove(position);
                            adapter.notifyDataSetChanged();
                            if(lstData.isEmpty()){
                                updateView(new ArrayList<>());
                            }
                        }

                        @Override
                        public void onFailure(String err) {
                            ToastUtils.show("删除失败："+err + "\n\n若本地没有该成员记录，请到网络采样查询页面进行删除。");
                        }
                    });
                });
            });
        },false));

        setOnKeyBoardLayoutStateChangeListener(onKeyBoardLayoutStateChangeListener);

        ToastUtils.show(this,"点击【扫描】按钮或【音量+】键来扫描条码！");
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if(event!=null&&event.getAction()==MotionEvent.ACTION_DOWN){
            if(event.getKeyCode()==KeyEvent.KEYCODE_VOLUME_UP) {
                onScanBarcodeClicked();
                return true;
            }
            if(event.getKeyCode()==KeyEvent.KEYCODE_ENTER){
                hideKeyboard();
                mPresenter.setChangedBarcode(evBarcode.getText().toString());
                mPresenter.requestTestTubeInfo();
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @OnClick(R.id.tv_scan_barcode)
    void onScanBarcodeClicked(){
        ZxingActivity.Scan(this, new ZxingActivity.IScanListener() {
            @Override
            public void onSuccess(String code) {
                evBarcode.setText(code);
                mPresenter.setChangedBarcode(code);
                mPresenter.requestTestTubeInfo();
            }

            @Override
            public void onFailure() {
                ToastUtils.show(NASamplingActivity.this,"条码扫描失败！");
            }
        });
    }

    @Override
    protected boolean hasRefreshBar() {
        return true;
    }

    @Override
    public void onRefresh(@NonNull RefreshLayout refreshLayout) {
        String text = evBarcode.getText().toString();
        if(text.trim().isEmpty()){
            refreshLayout.finishRefresh(false);
            ToastUtils.show("请输入条码！");
            return;
        }
        ApiProvider.requestSamplingResult(text, new ApiProvider.ISamplingResultListener() {
            @Override
            public void onSuccess(ESONArray data) {
                updateView(JsonUtils.parseToList(data));
                refreshLayout.finishRefresh(true);
            }

            @Override
            public void onFailure(String err) {
                ToastUtils.show(err);
                refreshLayout.finishRefresh(false);
            }
        });
    }

    @OnClick(R.id.iv_expansion)
    void onExpansionClicked(){
        Object tag = ivExpansion.getTag();
        int visibility = tag == null || ((tag instanceof Boolean || tag.getClass().equals(boolean.class)) && !((boolean) tag)) ? View.INVISIBLE : View.VISIBLE;
        ivExpansion.setTag(visibility == View.INVISIBLE);
        ivExpansion.setRotation(visibility == View.INVISIBLE?360:180);
        tvAddOcr      .setVisibility(visibility);
        tvAddSearching.setVisibility(visibility);
        tvAddCommunity.setVisibility(visibility);
        tvAddGrouping .setVisibility(visibility);
        ivSamplingPlan.setVisibility(visibility);
        llPartSearch  .setVisibility(visibility);

        tvPlanNum.setText(String.valueOf(lstSelected.size()));
        tvPlanNumMini.setText(tvPlanNum.getText().toString());
        if(visibility == View.VISIBLE && tvPlanNum.getText().toString().equals("0")){
            vPoint        .setVisibility(View.INVISIBLE);
            tvPlanNum     .setVisibility(View.INVISIBLE);
            tvSymbolAdd   .setVisibility(View.INVISIBLE);
        }
        else{
            vPoint        .setVisibility(visibility);
            tvPlanNum     .setVisibility(visibility);
            tvSymbolAdd   .setVisibility(visibility);
        }
    }

    List<ESONObject> lstSelected = new ArrayList<>();
    void setControlViewVisible(boolean visible){
        int visibility = visible?View.VISIBLE:View.INVISIBLE;
        if(lstSelected.size()+lstData.size() >= mPresenter.maxNum){
            tvAddOcr      .setVisibility(View.INVISIBLE);
            tvAddSearching.setVisibility(View.INVISIBLE);
            tvAddCommunity.setVisibility(View.INVISIBLE);
            tvAddGrouping .setVisibility(View.INVISIBLE);
        }
        else{
            tvAddOcr      .setVisibility(visibility);
            tvAddSearching.setVisibility(visibility);
            tvAddCommunity.setVisibility(visibility);
            tvAddGrouping .setVisibility(visibility);
        }

        llPartSearch.setVisibility(visibility);
        ivExpansion.setVisibility(visibility);
        ivSamplingPlan.setVisibility(visibility);

        ivExpansion.setRotation(visibility == View.INVISIBLE?360:180);

        tvPlanNum.setText(String.valueOf(lstSelected.size()));
        tvPlanNumMini.setText(tvPlanNum.getText().toString());
        if(visible && tvPlanNum.getText().toString().equals("0")){
            vPoint        .setVisibility(View.INVISIBLE);
            tvPlanNum     .setVisibility(View.INVISIBLE);
            tvSymbolAdd   .setVisibility(View.INVISIBLE);
        }
        else{
            vPoint        .setVisibility(visibility);
            tvPlanNum     .setVisibility(visibility);
            tvSymbolAdd   .setVisibility(visibility);
        }
    }

    @Override
    public void updateView(List<ESONObject> data) {
//        for (ESONObject item : data) {
//            Log.e(TAG,"updateView->"+item);
//        }

        tvEmpty.setVisibility(data.isEmpty()?View.VISIBLE:View.INVISIBLE);

        setControlViewVisible(data.size()< mPresenter.maxNum);

        lstData.clear();
        lstData.addAll(data);
        adapter.notifyDataSetChanged();
    }

    void addPeopleToPlanSampling(List<ESONObject> lst){
        Log.e(TAG,"onSelectResult->"+lst.size());
        int lastCount = lstSelected.size();
        for(int i=0,ni=lst.size();i<ni;++i){
            String idCardNo1 = lst.get(i).getJSONValue(mPresenter.sIdCardNoName,"");
            boolean contains = false;
            for(int j=0,nj=lstSelected.size();j<nj;++j){
                String idCardNo2 = lstSelected.get(j).getJSONValue(mPresenter.sIdCardNoName,"");
                if(idCardNo1.equals(idCardNo2)){
                    contains = true;
                    break;
                }
            }
            if(contains) {
                ToastUtils.show(this,idCardNo1+"重复选择！");
                continue;
            }
            lstSelected.add(lst.get(i));
        }
        int currCount = lstSelected.size();
        if(currCount - lastCount > 0){
            ToastUtils.show("成功添加"+(currCount - lastCount)+"条记录！");
        }
        setControlViewVisible(true);
    }

    @OnClick(R.id.tv_search)
    void onSearchIdNoClicked(){
        String searchText = etIdNo.getText().toString().trim().toUpperCase();
        if(searchText.isEmpty()){
            ToastUtils.show("查询身份号不能为空");
            return;
        }
        hideKeyboard();
        postDelayed(()->{
            MemberQueryActivity.doSelect(NASamplingActivity.this,1,0,new ESONObject().putValue(mPresenter.sIdCardNoName,searchText),1,new CommunitySearchActivity.ISelectCallback(){
                @Override
                public void onSelected(List<ESONObject> lstSelected) {
                    addPeopleToPlanSampling(lstSelected);
                    if(lstSelected.size()+lstData.size() < mPresenter.maxNum){
                        postDelayed(()->{
                            showKeyboard(etIdNo);
                            onKeyBoardLayoutStateChangeListener.onKeyBoardShow(iKeyboardHeight,iGapHeight);
                            etIdNo.setText("");
                        },100);
                    }
                }
            });
        },100);
    }

    @OnClick(R.id.tv_add_ocr)
    void onAddOcrClicked(){
        IDCardRecognizer.recognize(this, new IDCardRecognizer.IDCardRecognizeListener() {
            @Override
            public void onSuccess(String id, String type) {
                MemberQueryActivity.doSelect(NASamplingActivity.this,0,0,new ESONObject().putValue(mPresenter.sIdCardNoName,id),1,new CommunitySearchActivity.ISelectCallback(){
                    @Override
                    public void onSelected(List<ESONObject> lstSelected) {
                        addPeopleToPlanSampling(lstSelected);
                    }
                });
            }

            @Override
            public void onFailure() {
                ToastUtils.show("识别失败！");
            }
        });

    }

    @OnClick(R.id.tv_add_searching)
    void onAddSearchingClicked(){
        CommunitySearchActivity.select(this, mPresenter.maxNum - lstData.size(), new CommunitySearchActivity.ISelectCallback() {
            @Override
            public void onSelected(List<ESONObject> lstSelected) {
                addPeopleToPlanSampling(lstSelected);
            }
        });
    }

    @OnClick(R.id.tv_add_grouping)
    void onAddGroupingClicked(){
        NAGroupingActivity.doSelect(this, mPresenter.maxNum - lstData.size(), new NAGroupingActivity.ISelectCallback() {
            @Override
            public void onSelected(List<ESONObject> lstSelected) {
                addPeopleToPlanSampling(lstSelected);
            }
        });
    }

    @OnClick(R.id.tv_add_community)
    void onAddCommunityClicked(){
        MemberQueryActivity.doSelect(this, 0, 0, new ESONObject(), mPresenter.maxNum - lstData.size(), new CommunitySearchActivity.ISelectCallback() {
            @Override
            public void onSelected(List<ESONObject> lstSelected) {
                addPeopleToPlanSampling(lstSelected);
            }
        });
    }

    @OnClick({R.id.iv_sampling_plan,R.id.part_add_mini})
    void onShowSamplingPlanDialog(){
        if(lstSelected.size()==0){
            return;
        }
        hideKeyboard();
        ToastUtils.show("点击姓名播报语音！");
        Point p = DensityUtils.getScreenSize();
        new EasyDialog(R.layout.dialog_view_sampling_plan_and_continue,this)
                .setOnBindDialogListener(new EasyDialog.OnBindDialogListener() {

                    boolean bChangedResult = false;

                    @Override
                    public void onBindDialog(EasyDialogHolder holder) {
                        RecyclerView rv = holder.getView(R.id.rv_view_sampling_plan);
                        rv.setHasFixedSize(true);
                        rv.setLayoutManager(new LinearLayoutManager(NASamplingActivity.this));
                        rv.setAdapter(new EasyAdapter(NASamplingActivity.this, R.layout.recy_view_sampling_plan_item, lstSelected, (EasyAdapter.IEasyAdapter<ESONObject>) (holder1, data, position) -> {
                            holder1.setText(R.id.tv_index,String.valueOf(position+1));
                            String name = data.getJSONValue(mPresenter.sUserName,"");
                            holder1.setText(R.id.tv_name,name);
                            holder1.setOnClickListener(R.id.tv_name,v -> {
                                if(name == null || name.trim().isEmpty()) return;
                                TTSEngine.speakChinese(name);
                            });
                            holder1.setText(R.id.tv_id_card_no,data.getJSONValue(mPresenter.sIdCardNoName,""));
                            holder1.setOnClickListener(R.id.tv_detail,v -> CommunityMemberDetailActivity.shownMember(NASamplingActivity.this,data.getJSONValue(mPresenter.sIdCardNoName,"")));
                            holder1.setOnClickListener(R.id.tv_remove,v -> {
                                DialogManager.showAlertDialog(NASamplingActivity.this,"提示","确定要从待采样上传记录中移除吗？",null,v1 -> {
                                    lstSelected.remove(position);
                                    setControlViewVisible(true);
                                    rv.getAdapter().notifyItemRemoved(position);
                                    rv.getAdapter().notifyDataSetChanged();
                                });
                            });

                            holder1.setOnClickListener(R.id.tv_upload, new View.OnClickListener() {
                                long clickTime = 0L;
                                boolean isProcess = false;

                                void doUpload(){
                                    mPresenter.requestAddPeopleToTestTube(data.getJSONValue(mPresenter.sIdCardNoName, ""), new ISetterListener() {
                                        @Override
                                        public void onSuccess() {
                                            isProcess = false;
                                            ToastUtils.show("上传成功！");
                                            String name = lstSelected.get(position).getJSONValue(mPresenter.sUserName,"").trim();
                                            if(!name.isEmpty()){
                                                TTSEngine.speakChinese(name);
                                            }
                                            lstSelected.remove(position);
                                            rv.getAdapter().notifyDataSetChanged();

                                            setControlViewVisible(true);
                                            bChangedResult = true;
                                            if(lstSelected.isEmpty()){
                                                ToastUtils.show("全部记录上传成功！");
                                                holder.dismissDialog();
                                                srlMain.autoRefresh();
                                            }
                                        }

                                        @Override
                                        public void onFailure(String err) {
                                            isProcess = false;
                                            ToastUtils.show("上传失败："+err);
                                        }
                                    });
                                }

                                @Override
                                public void onClick(View v) {
                                    long now = System.currentTimeMillis();
                                    clickTime = now;

                                    if(isProcess) return;
                                    isProcess = true;
                                    App.PostDelayed(()->{
                                        if(System.currentTimeMillis() - clickTime < 240L){
                                            Log.e(TAG,"onDoubleClicked!");
                                            doUpload();
                                            return;
                                        }

                                        Log.e(TAG,"onSingleClicked!");
                                        ToastUtils.show("双击上传按钮立即上传记录！");
                                        DialogManager.showAlertDialog(NASamplingActivity.this,"提示","确定开始上传该成员记录吗？",v1 -> {isProcess = false;},v1 -> {
                                            doUpload();
                                        });
                                    },300);
                                }
                            });
                        }));
                        rv.invalidate();

                        holder.setOnClickListener(R.id.iv_close,v -> {
                            holder.dismissDialog();
                            if(bChangedResult) {
                                srlMain.autoRefresh();
                                setControlViewVisible(true);
                            }
                        });

                        holder.setOnClickListener(R.id.tv_start,v->{

                            DialogManager.showAlertDialog(NASamplingActivity.this,"提示","确定开始上传全部待采样记录？",null,v1 -> {

                                new EasyDialog(R.layout.dialog_sampling_plan_upload,NASamplingActivity.this)
                                        .setOnBindDialogListener(new EasyDialog.OnBindDialogListener() {
                                            AtomicBoolean bIsInterrupted = new AtomicBoolean(false);
                                            int iInitLength = lstSelected.size();
                                            EasyDialogHolder easyDialogHolder2;

                                            TextView tvProcess;
                                            ProgressBar pbProgress;

                                            void log(String s){
                                                post(()->tvProcess.setText(s));
                                            }

                                            void onProgress(int progress){
                                                post(()->pbProgress.setProgress(progress));
                                            }
                                            List<ESONObject> lstFailed = new ArrayList<>();
                                            Map<String,Integer> mRetryMapper = new HashMap<>();
                                            boolean onUploadFailure(String idCard){
                                                Integer integer = mRetryMapper.get(idCard);
                                                if(integer == null) integer = 0;
                                                if(integer<3){
                                                    mRetryMapper.put(idCard,integer+1);
                                                    return true;
                                                }
                                                lstFailed.add(lstSelected.get(0));
                                                lstSelected.remove(0);
                                                return false;
                                            }

                                            void onUploadSuccess(){
                                                lstSelected.remove(0);
                                                bChangedResult = true;
                                                post(()->rv.getAdapter().notifyItemRemoved(0));
                                                post(()->setControlViewVisible(true));
                                            }

                                            void upload(){
                                                ThreadPoolProvider.getFixedThreadPool().execute(()->{
                                                    if(lstSelected.isEmpty()){
                                                        onProgress(100);
                                                        if(lstFailed.isEmpty()){
                                                            post(()->holder.dismissDialog());
                                                            post(()->ToastUtils.show("记录上传成功！"));
                                                        }
                                                        else{
                                                            lstSelected.addAll(lstFailed);
                                                            if(lstFailed.size() != iInitLength){
                                                                post(()->ToastUtils.show("记录上传部分成功！"));
                                                            }
                                                            else{
                                                                post(()->ToastUtils.show("记录上传全部失败！"));
                                                            }
                                                        }
                                                        if(bChangedResult){
                                                            App.Post(()->setControlViewVisible(true));
                                                            App.Post(()->srlMain.autoRefresh());
                                                        }
                                                        postDelayed(()->easyDialogHolder2.dismissDialog(),500);
                                                        return;
                                                    }
                                                    if(bIsInterrupted.get()){
                                                        lstSelected.addAll(lstFailed);
                                                        post(()->ToastUtils.show("中断上传！"));
                                                        post(()->easyDialogHolder2.dismissDialog());
                                                        return;
                                                    }


                                                    ESONObject e = lstSelected.get(0);
                                                    String idCard = e.getJSONValue(mPresenter.sIdCardNoName,"");

                                                    log("开始准备上传"+idCard);
                                                    int progress = (iInitLength - lstSelected.size())*100/iInitLength;
                                                    onProgress(progress);

                                                    mPresenter.requestAddPeopleToTestTube(idCard, new ISetterListener() {
                                                        @Override
                                                        public void onSuccess() {
                                                            log(idCard+"处理完毕！");
                                                            onUploadSuccess();
                                                            postDelayed(()->upload(),500);
                                                        }

                                                        @Override
                                                        public void onFailure(String err) {
                                                            if(onUploadFailure(idCard)){
                                                                log(idCard+"处理失败，500ms后重试！");
                                                            }
                                                            else{
                                                                log(idCard+"处理失败，跳过该记录！");
                                                            }
                                                            postDelayed(()->upload(),500);
                                                        }
                                                    });

                                                });
                                            }

                                            @Override
                                            public void onBindDialog(EasyDialogHolder easyDialogHolder) {
                                                easyDialogHolder2 = easyDialogHolder;
                                                tvProcess = easyDialogHolder.getView(R.id.tv_process);
                                                pbProgress = easyDialogHolder.getView(R.id.pb_process);

                                                easyDialogHolder.setOnClickListener(R.id.tv_interrupt,v2 -> {
                                                    bIsInterrupted.set(true);
                                                });
                                                upload();
                                            }
                                        })
                                        .setForegroundResource(R.drawable.shape_common_dialog)
                                        .setDialogParams(p.x - (int)DensityUtils.dp2px(100f),(int)DensityUtils.dp2px(100f), Gravity.CENTER)
                                        .setAllowDismissWhenTouchOutside(false)
                                        .setAllowDismissWhenBackPressed(false)
                                        .showDialog();

                            });

                        });
                    }
                })
                .setForegroundResource(R.drawable.shape_dialog_corner_with_tl_tr)
                .setDialogParams(p.x,p.y- ((int)DensityUtils.dp2px(100f)), Gravity.BOTTOM)
                .showDialog();
    }

}
