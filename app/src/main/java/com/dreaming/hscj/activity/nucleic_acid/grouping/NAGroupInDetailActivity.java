package com.dreaming.hscj.activity.nucleic_acid.grouping;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.dreaming.hscj.App;
import com.dreaming.hscj.R;
import com.dreaming.hscj.activity.community.manage.CommunityMemberDetailActivity;
import com.dreaming.hscj.base.BaseActivity;
import com.dreaming.hscj.core.BundleBuilder;
import com.dreaming.hscj.core.ThreadPoolProvider;
import com.dreaming.hscj.core.ViewInjector;
import com.dreaming.hscj.dialog.DialogManager;
import com.dreaming.hscj.dialog.loading.LoadingDialog;
import com.dreaming.hscj.template.DataParser;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.template.api.wrapper.ApiParam;
import com.dreaming.hscj.template.database.wrapper.DatabaseConfig;
import com.dreaming.hscj.template.database.wrapper.IDeleteListener;
import com.dreaming.hscj.template.database.wrapper.ISetterListener;
import com.dreaming.hscj.utils.ToastUtils;
import com.dreaming.hscj.utils.ZxingUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import priv.songxusheng.easyjson.ESONObject;

public class NAGroupInDetailActivity extends BaseActivity{
    static ESONObject eData    = new ESONObject(); //数据库
    static ESONObject eShown   = new ESONObject(); //数据库转show
    static ESONObject eSaving  = new ESONObject();//展示的数据
    static ESONObject eConvert = new ESONObject();//展示转换数据库
    private static final int RESULT_SAVE = 1,RESULT_DELETE = 2;
    public interface IMemberStateChangeListener{
        void onDelete();
        void onSave(ESONObject data);
        void onCancel();
    }

    public static void shownMember(BaseActivity activity, ESONObject eDatabaseData){
        eData = eDatabaseData;
        activity.startActivity(NAGroupInDetailActivity.class, BundleBuilder.create("ReadOnly",true).build());
    }

    public static void shownMember(BaseActivity activity, ESONObject eDatabaseData,IMemberStateChangeListener listener){
        eData = eDatabaseData;
        activity.startActivityForResult(NAGroupInDetailActivity.class,4002, new OnActivityResultItemCallBack() {
            @Override
            public void OnActivityRequestResult(int resultCode, Intent data) {
                switch (resultCode){
                    case RESULT_SAVE:
                        listener.onSave(eConvert);
                        break;
                    case RESULT_DELETE:
                        listener.onDelete();
                        break;
                    default:
                        listener.onCancel();
                        break;
                }
            }
        });
    }

    @Override
    public int getContentViewResId() {
        return R.layout.activity_nucleic_acid_group_in_detail;
    }

    @Override
    protected void onBindView(View vContent) {
        ButterKnife.bind(this,vContent);
    }

    public String sIdCardNo = Template.getCurrentTemplate().getUserInputGuideDatabase().getIdFieldName();
    public String sName     = Template.getCurrentTemplate().getUserInputGuideDatabase().getNameFiledName();
    public String sGroupId  = Template.getCurrentTemplate().getNucleicAcidGroupingDatabase().getGroupIdFieldName();

    @BindView(R.id.ll_content)
    LinearLayout llContent;
    @BindView(R.id.tv_save)
    TextView tvSave;

    void showLoadingDialog(){
        LoadingDialog.showDialog("Build View Tree",this);
    }

    void hideLoadingDialog(){
        LoadingDialog.dismissDialog("Build View Tree");
    }

    boolean bIsReadOnly;

    @Override
    public void initView() {
        setCenterText("核酸分组成员详情");
        bIsReadOnly = getIntent().getBooleanExtra("ReadOnly",false);

        if(!bIsReadOnly) {
            setRightText("删除记录");
            tvTitleRight.setTextColor(getResources().getColor(R.color.material_red_500));
            tvTitleRight.setOnClickListener(v -> {
                DialogManager.showAlertDialog(this,"提示","确定要删除此成员记录？",null, v1 -> {
                    ThreadPoolProvider.getFixedThreadPool().execute(()->{
                        Template.getCurrentTemplate().getNucleicAcidGroupingDatabase().deletePeopleWithGroup(
                                eData.getJSONValue(sIdCardNo, ""),
                                eData.getJSONValue(Template.getCurrentTemplate().getNucleicAcidGroupingDatabase().getGroupIdFieldName(), ""), new IDeleteListener() {
                            @Override
                            public void onSuccess(int count) {
                                if(count>0){
                                    setResult(RESULT_DELETE);
                                    ToastUtils.show("删除成功！");
                                    finish();
                                    return;
                                }
                                ToastUtils.show("删除失败！");
                            }

                            @Override
                            public void onFailure(String err) {
                                ToastUtils.show("删除失败！");
                            }
                        });
                    });
                });
            });
            tvSave.setOnClickListener(v -> {
                ThreadPoolProvider.getFixedThreadPool().execute(()->{
                    final ESONObject data = DataParser.parseShownToDatabase(DatabaseConfig.TYPE_NA_GROUPING,eSaving);
                    Template.getCurrentTemplate().getNucleicAcidGroupingDatabase().addPeopleToGroup(data, new ISetterListener() {
                        @Override
                        public void onSuccess() {
                            setResult(RESULT_SAVE);
                            ToastUtils.show("保存成功");
                            eConvert = data;
                            finish();
                        }

                        @Override
                        public void onFailure(String err) {
                            ToastUtils.show("保存失败："+err);
                        }
                    });
                });
            });
        }
        else{
            tvSave.setVisibility(View.GONE);
        }

        buildViewTree();
    }

    List<EditText> lstAllEdits = new ArrayList<>();
    Map<String,EditText> mKeyMapper = new HashMap<>();
    List<EditText> lstNoneNull = new ArrayList<>();
    void buildViewTree(){

        showLoadingDialog();
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            eShown = DataParser.parseDatabaseToShown(DatabaseConfig.TYPE_NA_GROUPING,eData);

            final String idNo   = eShown.getJSONValue(Template.getCurrentTemplate().getIdCardNoFieldName(),"");
            App.Post(()->{
                Point p = new Point();
                getWindowManager().getDefaultDisplay().getSize(p);
                ImageView ivQR = new ImageView(this);
                ivQR.setImageBitmap(ZxingUtils.autoCreateCodeBitmap(idNo,p.x,p.x,"utf-8","","", Color.BLACK, Color.WHITE));
                llContent.addView(ivQR,new LinearLayout.LayoutParams(p.x,p.x));
            });

            List<ApiParam> lst = Template.getCurrentTemplate().getNucleicAcidGroupingDatabase().getConfig().getFields();

            for(int i=0,ni=lst.size();i<ni;++i){
                final ApiParam param = lst.get(i);
                App.Post(()->{
                    CardView vInput = (CardView) LayoutInflater.from(this).inflate(R.layout.view_input,null,false);
                    LinearLayout llRoot = (LinearLayout) vInput.getChildAt(0);
                    String name = param.getDescription();

                    TextView tvName  = (TextView) llRoot.getChildAt(0);
                    tvName.setText(name);
                    if("NOEMPTY".equals(param.getDefaultValue())){
                        tvName.setTextColor(getResources().getColor(R.color.material_red_A400));
                    }

                    EditText evValue = (EditText) llRoot.getChildAt(1);
                    if("NOEMPTY".equals(param.getDefaultValue())) {
                        lstNoneNull.add(evValue);
                    }
                    if(bIsReadOnly || param.getName().equals(sIdCardNo) || param.getName().equals(sName) ||param.getName().equals(sGroupId)){
                        evValue.setEnabled(false);
                    }
                    evValue.setTag(param);
                    lstAllEdits.add(evValue);
                    evValue.setText(eShown.getJSONValue(param.getName(),""));
                    eSaving.putValue(param.getName(),eShown.getJSONValue(param.getName(),""));
                    mKeyMapper.put(param.getName(),evValue);
                    evValue.setHint("请输入"+name);
                    evValue.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                        @Override
                        public void onFocusChange(View v, boolean hasFocus) {
                            if(hasFocus){
                                evValue.selectAll();
                            }
                        }
                    });
                    evValue.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {}
                        @Override
                        public void afterTextChanged(Editable s) {
                            eSaving.putValue(param.getName(),s.toString());
                            autoShowSaveBtn();
                        }
                    });

                    ViewInjector.inject(vInput);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp2px(40f));
                    params.topMargin = dp2px(2f);
                    llContent.addView(vInput,params);
                });
            }


            App.Post(()->{
                final View v = new View(this);
                final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp2px(80f));
                params.topMargin = dp2px(2f);
                llContent.addView(v,params);
                hideLoadingDialog();
            });

        });

    }

    void autoShowSaveBtn(){
        if(bIsReadOnly) return;
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            boolean shouldShown = false;
            for (Iterator<String> it = eShown.keys(); it.hasNext(); ) {
                String key = it.next();
                String v1 = eShown.getJSONValue(key,"");
                String v2 = eSaving.getJSONValue(key,"");
                if(v1 == null && v2 == null) continue;
                if(v1 == null) v1 = "";
                if(v2 == null) v2 = "";
                if(v1.equals(v2)) continue;
                shouldShown = true;
                break;
            }
            final boolean isShown = shouldShown;
            Log.e("SAVING",isShown+"");
            App.Post(()->tvSave.setVisibility(isShown?View.VISIBLE:View.INVISIBLE));
        });
    }

    @OnClick(R.id.tv_more)
    void onViewMoreClicked(){
        CommunityMemberDetailActivity.shownMember(this,eData.getJSONValue(sIdCardNo,""));
    }

}
