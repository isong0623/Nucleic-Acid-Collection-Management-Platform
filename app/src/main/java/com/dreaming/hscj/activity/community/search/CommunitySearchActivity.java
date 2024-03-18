package com.dreaming.hscj.activity.community.search;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.dreaming.hscj.App;
import com.dreaming.hscj.Constants;
import com.dreaming.hscj.R;
import com.dreaming.hscj.activity.system.MemberQueryActivity;
import com.dreaming.hscj.base.BaseActivity;
import com.dreaming.hscj.base.contract.BaseMVPActivity;
import com.dreaming.hscj.core.BundleBuilder;
import com.dreaming.hscj.core.ThreadPoolProvider;
import com.dreaming.hscj.core.ocr.IDCardRecognizer;
import com.dreaming.hscj.dialog.DialogManager;
import com.dreaming.hscj.dialog.loading.LoadingDialog;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.template.api.wrapper.ApiParam;
import com.dreaming.hscj.utils.ToastUtils;
import com.dreaming.hscj.utils.ZxingUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

public class CommunitySearchActivity extends BaseMVPActivity<CommunitySearchPresenter> implements ICommunitySearchContract.View{
    @Override
    public int getContentViewResId() {
        return R.layout.activity_community_search;
    }

    @Override
    protected void onBindView(View vContent) {
        ButterKnife.bind(this,vContent);
    }

    public interface ISelectCallback{
        void onSelected(List<ESONObject> lstSelected);
    }
    public static void select(BaseActivity activity, int max, ISelectCallback callback){
        activity.startActivityForResult(CommunitySearchActivity.class, BundleBuilder.create("OnlyQuery", false).put("max",max).build(), new OnActivityResultItemCallBack() {
            @Override
            public void OnActivityRequestResult(int resultCode, Intent data) {
                if(data == null){
                    callback.onSelected(new ArrayList<>());
                    return;
                }
                String result = data.getStringExtra("data");
                if(result == null) result = "";
                ESONObject e = new ESONObject(result);
                ESONArray a = e.getJSONValue("data",new ESONArray());
                List<ESONObject> l = new ArrayList<>();

                for(int i=0,ni=a.length();i<ni;++i){
                    l.add(a.getArrayValue(i,new ESONObject()));
                }
                callback.onSelected(l);
            }
        });
    }

    @BindView(R.id.tv_name)
    TextView tvCurrentDatabaseName;
    @BindView(R.id.tv_value)
    TextView tvCurrentDatabaseValue;
    @BindView(R.id.ll_content)
    LinearLayout llContent;
    @BindView(R.id.tv_query)
    TextView tvQuery;
    @BindView(R.id.sv)
    ScrollView sv;

    @BindView(R.id.rb_accuracy)
    RadioButton rbAccuracy;
    @BindView(R.id.rb_vague)
    RadioButton rbVague;

    @BindView(R.id.rb_group_all)
    RadioButton rbGroupAll;
    @BindView(R.id.rb_group_in)
    RadioButton rbGroupIn;
    @BindView(R.id.rb_group_out)
    RadioButton rbGroupOut;

    void autoScroll(){
        int iHeightCounter = 0;
        for(int i=0,ni=llContent.getChildCount()-1;i<ni;++i){
            final EditText et = (EditText) ((LinearLayout)((CardView)llContent.getChildAt(i)).getChildAt(0)).getChildAt(2);
            if(et.hasFocus()) {
                sv.smoothScrollTo(0,iHeightCounter);
                postDelayed(()->et.requestFocus(),50);
                return;
            }
            iHeightCounter += llContent.getChildAt(i).getMeasuredHeight();
        }
    }

    CompoundButton.OnCheckedChangeListener onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(isChecked){
                switch (buttonView.getId()){
                    case R.id.rb_accuracy:
                        Constants.Config.setMemberQueryMode(0);
                        mPresenter.updateQueryMode(0);
                        break;
                    case R.id.rb_vague:
                        Constants.Config.setMemberQueryMode(1);
                        mPresenter.updateQueryMode(1);
                        break;
                    case R.id.rb_group_all:
                        Constants.Config.setMemberQueryGroup(0);
                        mPresenter.updateQueryGroup(0);
                        llContent.removeAllViews();
                        buildViewTree();
                        break;
                    case R.id.rb_group_in:
                        Constants.Config.setMemberQueryGroup(1);
                        mPresenter.updateQueryGroup(1);
                        llContent.removeAllViews();
                        buildViewTree();
                        break;
                    case R.id.rb_group_out:
                        Constants.Config.setMemberQueryGroup(2);
                        mPresenter.updateQueryGroup(2);
                        llContent.removeAllViews();
                        buildViewTree();
                        break;
                }
            }
        }
    };
    boolean bIsOnlyQuery = true;
    boolean bIsShownKeyboard = false;
    int iSelectMax = 0;
    @Override
    public void initView() {
        setCenterText("社区成员查询");
        setRightText("重置条件");

        iSelectMax   = getIntent().getIntExtra("max",0);
        bIsOnlyQuery = getIntent().getBooleanExtra("OnlyQuery",true);

        tvTitleRight.setOnClickListener(v -> {
            DialogManager.showAlertDialog(this,"提示","确定要重置搜索条件吗？",null,v1->{
                Constants.Config.setMemberQueryMode(0);
                Constants.Config.setMemberQueryGroup(0);
                Constants.Config.setSearchingFieldsConfig(new ESONObject());
                eConfig = new ESONObject();
                llContent.removeAllViews();
                rbAccuracy.setChecked(true);
                rbGroupAll.setChecked(true);
                buildViewTree();
            });
        });

        int queryMode = Constants.Config.getMemberQueryMode();
        (queryMode == 1? rbVague : rbAccuracy).setChecked(true);
        mPresenter.updateQueryMode(queryMode);

        int queryGroup= Constants.Config.getMemberQueryGroup();
        (queryGroup ==1?rbGroupIn :(queryGroup==2?rbGroupOut:rbGroupAll)).setChecked(true);
        mPresenter.updateQueryGroup(queryGroup);

        buildViewTree();

        rbAccuracy.setOnCheckedChangeListener(onCheckedChangeListener);
        rbVague   .setOnCheckedChangeListener(onCheckedChangeListener);
        rbGroupAll.setOnCheckedChangeListener(onCheckedChangeListener);
        rbGroupIn .setOnCheckedChangeListener(onCheckedChangeListener);
        rbGroupOut.setOnCheckedChangeListener(onCheckedChangeListener);

        tvCurrentDatabaseName.setText("当前数据库：");
        tvCurrentDatabaseName.setWidth(dp2px(150f));
        tvCurrentDatabaseName.invalidate();

        ESONObject object = Constants.DBConfig.getSelectedDatabase();
        String sTownName    = object.getJSONValue("townName"   ,"");
        String sVillageName = object.getJSONValue("villageName","");
        tvCurrentDatabaseValue.setText(String.format("%s/%s/%s", Template.getCurrentTemplate().getDatabaseSetting().getRegionName(),sTownName,sVillageName));

        setOnKeyBoardLayoutStateChangeListener(new OnKeyBoardLayoutStateChangeListener() {

            void adjustScrollViewHeight(int keyBoardHeight, int gapHeight){
                llContent.setPadding(0,0,0, keyBoardHeight - gapHeight);
                llContent.invalidate();
            }
            void resetScrollViewHeight(){
                llContent.setPadding(0,0,0,0);
                llContent.invalidate();
            }

            void adjustSaveButtonBottomMargin(int keyBoardHeight, int gapHeight){
                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) tvQuery.getLayoutParams();
                params.bottomMargin = keyBoardHeight - gapHeight + dp2px(35) ;
                tvQuery.setLayoutParams(params);
                tvQuery.invalidate();
            }

            void resetSaveButtonBottomMargin(){
                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) tvQuery.getLayoutParams();
                params.bottomMargin = dp2px(40f);
                tvQuery.setLayoutParams(params);
                tvQuery.invalidate();
            }

            @Override
            public void onKeyBoardShow(int keyBoardHeight, int gapHeight) {
                bIsShownKeyboard = true;
                adjustScrollViewHeight(keyBoardHeight, gapHeight);
                adjustSaveButtonBottomMargin(keyBoardHeight, gapHeight);
                autoScroll();
            }

            @Override
            public void onKeyBoardHide() {
                bIsShownKeyboard = false;
                resetScrollViewHeight();
                resetSaveButtonBottomMargin();
            }
        });
    }


    @Override
    protected boolean enableVolumeDownRecognizeIDCard() {
        return true;
    }

    @Override
    protected void onRecognizeCardSuccess(String idCard, String type) {
        ToastUtils.show("识别到"+type+":"+idCard);
        EditText etIdNo = mKeyMapper.get(Template.getCurrentTemplate().getIdCardNoFieldName());
        if(etIdNo == null) return;
        LinearLayout llRoot = (LinearLayout) etIdNo.getParent();
        CheckBox cbChoose = llRoot.findViewById(R.id.cb_choose);
        etIdNo.setText(idCard);
        cbChoose.setChecked(true);
        hideKeyboard();
    }

    @Override
    protected void onRecognizeCardFailure() {
        ToastUtils.show("识别失败！");
    }

    List<EditText> lstAllEdits = new ArrayList<>();
    Map<String,EditText> mKeyMapper = new HashMap<>();
    List<EditText> lstNoneNull = new ArrayList<>();
    Set<EditText> allQuery = new HashSet<>();
    ESONObject eConfig = Constants.Config.getSearchingFieldsConfig();
    void buildViewTree(){
        LoadingDialog.showDialog("buildViewTree",this);
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            allQuery.clear();
            mPresenter.resetQueryFields();
            List<ApiParam> lst = rbGroupAll.isChecked()?Template.getCurrentTemplate().getUserOverallDatabase().getConfig().getFields():
                    (rbGroupIn.isChecked()?Template.getCurrentTemplate().getNucleicAcidGroupingDatabase().getConfig().getFields():
                            Template.getCurrentTemplate().getNoneGroupingDatabase().getConfig().getFields());
            for(int i=0,ni=lst.size();i<ni;++i){
                final ApiParam param = lst.get(i);
                App.Post(()->{
                    CardView vInput = (CardView) LayoutInflater.from(this).inflate(R.layout.view_query_field_item,null,false);
                    LinearLayout llRoot = (LinearLayout) vInput.getChildAt(0);
                    String name = param.getDescription();
                    if(name==null||name.isEmpty()||name.equals("?")){
                        name = param.getName();
                    }

                    TextView tvName  = (TextView) llRoot.getChildAt(1);
                    tvName.setText(name);
                    if("NOEMPTY".equals(param.getDefaultValue())){
                        tvName.setTextColor(getResources().getColor(R.color.material_red_A400));
                    }

                    final EditText evValue = (EditText) llRoot.getChildAt(2);
                    if("NOEMPTY".equals(param.getDefaultValue())) {
                        lstNoneNull.add(evValue);
                    }
                    evValue.setTag(param);
                    lstAllEdits.add(evValue);
                    mKeyMapper.put(param.getName(),evValue);
                    evValue.setHint("点击包含项以输入"+name);
                    evValue.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) { }

                        @Override
                        public void afterTextChanged(Editable s) {
                            String text = s.toString();
                            mPresenter.updateQueryFields(param.getName(),text);
                        }
                    });

                    CheckBox cbChoose = llRoot.findViewById(R.id.cb_choose);
                    if(eConfig.getJSONValue(param.getName(),0)==1){
                        evValue.setEnabled(true);
                        allQuery.add(evValue);
                        evValue.setHint("请输入"+param.getDescription());
                        mPresenter.updateQueryFields(param.getName(),"");
                    }
                    else{
                        evValue.setEnabled(false);
                    }
                    cbChoose.setChecked(eConfig.getJSONValue(param.getName(),0)==1);
                    cbChoose.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            evValue.setEnabled(isChecked);
                            eConfig.putValue(param.getName(),isChecked?1:0);
                            if(isChecked){
                                allQuery.add(evValue);
                                evValue.setHint("请输入"+param.getDescription());
                                mPresenter.updateQueryFields(param.getName(),"");
                                showKeyboard(evValue);
                            }
                            else{
                                allQuery.remove(evValue);
                                evValue.setHint("点击包含项以输入"+param.getDescription());
                                mPresenter.updateQueryFields(param.getName(),null);
                            }
                        }
                    });

                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp2px(40f));
                    params.topMargin = dp2px(2f);
                    llContent.addView(vInput,params);
                });
            }

            App.Post(()->{
                final View v = new View(this);
                final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp2px(180f));
                params.topMargin = dp2px(2f);
                llContent.addView(v,params);
                LoadingDialog.dismissDialog("buildViewTree");
            });
        });
    }

    @Override
    protected boolean onKeyEnterDown() {
        for(int i=0,ni=lstAllEdits.size();i<ni;++i){
            EditText et = lstAllEdits.get(i);
            if(!et.isEnabled()) continue;
            if(!et.hasFocus()) continue;
            for(int j=i+1;j<ni;++j){
                EditText et1 = lstAllEdits.get(j);
                if(et1.isEnabled()){
                    et1.requestFocus();
                    return true;
                }
            }
            break;
        }
        return super.onKeyEnterDown();
    }

    @OnClick(R.id.tv_query)
    void onQueryClicked(){
        Constants.Config.setSearchingFieldsConfig(eConfig);
        if(bIsOnlyQuery){
            MemberQueryActivity.doQuery(this,mPresenter.mode,mPresenter.group,mPresenter.eQueryFields);
        }
        else{
            MemberQueryActivity.doSelect(this, mPresenter.mode, mPresenter.group, mPresenter.eQueryFields, iSelectMax, new ISelectCallback() {
                @Override
                public void onSelected(List<ESONObject> lstSelected) {
                    ESONArray a = new ESONArray();
                    for(int i=0,ni=lstSelected.size();i<ni;++i){
                        a.putValue(lstSelected.get(i));
                    }

                    setResult(RESULT_OK,new Intent(){{putExtras(BundleBuilder.create("data",new ESONObject().putValue("data",a).toString()).build());}});
                    finish();
                }
            });
        }
    }

    @OnClick(R.id.iv_qr_search)
    void onRecQrClicked(){
        IDCardRecognizer.recognize(this, new IDCardRecognizer.IDCardRecognizeListener() {
            @Override
            public void onSuccess(String id, String type) {
                Point p = new Point();
                getWindowManager().getDefaultDisplay().getSize(p);
                CommunitySearchShownQRActivity.shownQR(CommunitySearchActivity.this,type,id);
            }

            @Override
            public void onFailure() {
                ToastUtils.show("识别失败！");
            }
        });
    }
}
