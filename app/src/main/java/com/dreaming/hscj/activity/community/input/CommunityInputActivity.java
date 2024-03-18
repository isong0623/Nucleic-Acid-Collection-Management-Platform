package com.dreaming.hscj.activity.community.input;

import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
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
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.dreaming.hscj.Constants;
import com.dreaming.hscj.R;
import com.dreaming.hscj.activity.community.batch_input.CommunityBatchInputActivity;
import com.dreaming.hscj.base.BaseActivity;
import com.dreaming.hscj.base.ToastDialog;
import com.dreaming.hscj.base.contract.BaseMVPActivity;
import com.dreaming.hscj.core.BundleBuilder;
import com.dreaming.hscj.core.TTSEngine;
import com.dreaming.hscj.core.ViewInjector;
import com.dreaming.hscj.dialog.loading.LoadingDialog;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.template.api.wrapper.ApiParam;
import com.dreaming.hscj.utils.CheckUtils;
import com.dreaming.hscj.utils.ToastUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import priv.songxusheng.easyjson.ESONObject;

public class CommunityInputActivity extends BaseMVPActivity<CommunityInputPresenter> implements ICommunityInputContract.View, View.OnClickListener {

    public interface IInputCallback{
        void onInputSuccess();
    }
    public static void doInputOneAndReturn(BaseActivity activity, IInputCallback callback){
        activity.startActivityForResult(CommunityInputActivity.class, BundleBuilder.create("isInputOne",true).build(),1001, new OnActivityResultItemCallBack() {
            @Override
            public void OnActivityRequestResult(int resultCode, Intent data) {
                if(resultCode == RESULT_OK){
                    callback.onInputSuccess();
                }
            }
        });
    }

    public static void doInputOneWithId(BaseActivity activity, String id){
        activity.startActivityForResult(CommunityInputActivity.class, BundleBuilder.create().put("id",id).put("isInputOne",true).build(), 1002, new OnActivityResultItemCallBack() {
            @Override
            public void OnActivityRequestResult(int resultCode, Intent data) {

            }
        });
    }

    @Override
    public int getContentViewResId() {
        return R.layout.activity_community_input;
    }

    @Override
    protected void onBindView(View vContent) {
        ButterKnife.bind(this,vContent);
    }

    boolean isInputOne = false;
    String inputId;
    @Override
    public void initView() {
        setCenterText("社区人员添加");
        isInputOne = getIntent().getBooleanExtra("isInputOne",false);
        inputId    = getIntent().getStringExtra("id");
        if(inputId == null) inputId = "";
        if(!isInputOne){
            setRightText("批量导入");
            tvTitleRight.setOnClickListener(this);
        }
        post(this::setupView);
        post(this::setupDatabaseInfo);
        post(this::buildViewTree);
        post(()->{
            if(inputId.trim().isEmpty()) return;
            etRecognizeIdCard.setText(inputId);
        });
    }

    @BindView(R.id.part_database_info)
    CardView cvPartDatabaseInfo;
    @BindView(R.id.part_input_id_card)
    CardView cvPartInputIdCard;
    @BindView(R.id.part_input_name)
    CardView cvPartInputName;
    @BindView(R.id.sv_input_design)
    ScrollView svInputDesign;
    @BindView(R.id.ll_input_design)
    LinearLayout llInputDesign;
    @BindView(R.id.tv_input_id_card)
    TextView tvInputIdCard;
    @BindView(R.id.ev_recognize_id_card)
    EditText etRecognizeIdCard;
    @BindView(R.id.iv_recognize_id_card)
    ImageView ivRecognizeIdCard;
    @BindView(R.id.tv_save)
    TextView tvSave;
    @BindView(R.id.tv_input_name)
    TextView tvInputName;
    @BindView(R.id.ev_input_name)
    EditText evInputName;
    @BindView(R.id.iv_read)
    ImageView ivRead;
    @BindView(R.id.cb_remember_read)
    CheckBox cbRememberRead;
    @BindView(R.id.rb_local_fist)
    RadioButton rbLocalFirst;
    @BindView(R.id.rb_net_first)
    RadioButton rbNetFirst;


    void setupDatabaseInfo(){
        LinearLayout llRoot = (LinearLayout) cvPartDatabaseInfo.getChildAt(0);
        TextView tvName  = (TextView) llRoot.getChildAt(0);
        tvName.setText("当前数据库");
        tvName.setTextColor(getResources().getColor(R.color.material_grey_600));

        TextView tvValue = (TextView) llRoot.getChildAt(1);
        ESONObject object = Constants.DBConfig.getSelectedDatabase();
        String sTownName    = object.getJSONValue("townName"   ,"");
        String sVillageName = object.getJSONValue("villageName","");
        tvValue.setText(String.format("%s/%s/%s", Template.getCurrentTemplate().getDatabaseSetting().getRegionName(),sTownName,sVillageName));
    }

    void autoScroll(){
        if(etRecognizeIdCard.hasFocus()) return;
        if(evInputName.hasFocus()) return;
        int iHeightCounter = 0;
        for(int i=0,ni=llInputDesign.getChildCount()-1;i<ni;++i){
            final EditText et = (EditText) ((LinearLayout)((CardView)llInputDesign.getChildAt(i)).getChildAt(0)).getChildAt(1);
            if(et.hasFocus()) {
                svInputDesign.smoothScrollTo(0,iHeightCounter);
                postDelayed(()->et.requestFocus(),50);
                return;
            }
            iHeightCounter += llInputDesign.getChildAt(i).getMeasuredHeight();
        }
    }

    public void saveConfig(){
        ESONObject config = new ESONObject();
        config.putValue("auto speak",cbRememberRead.isChecked()?1:0);
        config.putValue("priority",rbLocalFirst.isChecked()?0:1);
        Constants.Config.setInputConfig(config);
    }

    void setupView(){
        ViewInjector.inject(cvPartInputIdCard);
        ViewInjector.inject(cvPartInputName);

        ESONObject config = Constants.Config.getInputConfig();
        int read = config.getJSONValue("auto speak",0);
        cbRememberRead.setChecked(read == 1);
        int priority = config.getJSONValue("priority",0);
        (priority == 0 ? rbLocalFirst : rbNetFirst).setChecked(true);
        mPresenter.setLocalDataFirst(priority == 0);

        rbLocalFirst.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) mPresenter.setLocalDataFirst(true);
            }
        });
        rbNetFirst.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) mPresenter.setLocalDataFirst(false);
            }
        });

        etRecognizeIdCard.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            long reqTime = 0L;

            @Override
            public void afterTextChanged(Editable s) {
                String idCard = s.toString().trim();
                StringBuilder sb = new StringBuilder(idCard);
                reqTime = System.currentTimeMillis();
                String type = CheckUtils.isValidCard(Template.getCurrentTemplate().getApiConfig().getCard(), sb);
                if(type == null) return;
                if(!idCard.equals(sb.toString())){
                    idCard = sb.toString();
                    etRecognizeIdCard.removeTextChangedListener(this);
                    etRecognizeIdCard.setText(idCard);
                    etRecognizeIdCard.addTextChangedListener(this);
                }
                mPresenter.updateDataBlock(mPresenter.sIdCardNoName,idCard);
                postDelayed(()->{
                    long now = System.currentTimeMillis();
                    if(now - reqTime < 1500) return;
                    hideKeyboard();
                    mPresenter.requestDatabaseData();
                },1500);
            }
        });
        ivRecognizeIdCard.setOnClickListener(v -> recognizeIDCard());

        evInputName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus){
                    mPresenter.updateDataBlock(mPresenter.sUserName,evInputName.getText().toString());
                }
            }
        });

        ivRead.setOnClickListener(v->{
            if(evInputName.getText().toString().trim().isEmpty()){
                return;
            }
            TTSEngine.speakChinese(evInputName.getText().toString().trim());
        });

        setOnKeyBoardLayoutStateChangeListener(new OnKeyBoardLayoutStateChangeListener() {

            void adjustScrollViewHeight(int keyBoardHeight, int gapHeight){
                svInputDesign.setPadding(0,0,0, keyBoardHeight - gapHeight);
                svInputDesign.invalidate();
            }
            void resetScrollViewHeight(){
                svInputDesign.setPadding(0,0,0,0);
                svInputDesign.invalidate();
            }

            void adjustSaveButtonBottomMargin(int keyBoardHeight, int gapHeight){
                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) tvSave.getLayoutParams();
                params.bottomMargin = keyBoardHeight - gapHeight + dp2px(5);
                tvSave.setLayoutParams(params);
                tvSave.invalidate();
            }

            void resetSaveButtonBottomMargin(){
                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) tvSave.getLayoutParams();
                params.bottomMargin = dp2px(40f);
                tvSave.setLayoutParams(params);
                tvSave.invalidate();
            }

            @Override
            public void onKeyBoardShow(int keyBoardHeight, int gapHeight) {
                adjustScrollViewHeight(keyBoardHeight, gapHeight);
                adjustSaveButtonBottomMargin(keyBoardHeight, gapHeight);
                autoScroll();
            }

            @Override
            public void onKeyBoardHide() {
                resetScrollViewHeight();
                resetSaveButtonBottomMargin();
            }
        });
    }

    void setClickedToCopy(View v, TextView t){
        v.setOnClickListener(v1 -> {
            String text = t.toString();
            if(text.isEmpty()){
                ToastUtils.show("值为空，复制失败！");
                return;
            }
            ViewInjector.copyClipboard(text);
        });
    }

    private Map<String,EditText> mKeyMapper = new HashMap<>();
    private List<EditText> lstAllEdits      = new ArrayList<>();
    private List<EditText> lstNoneNull      = new ArrayList<>();
    void buildViewTree(){
        List<ApiParam> lstParams = Template.getCurrentTemplate().getUserOverallDatabase().getConfig().getFields();

        etRecognizeIdCard.setTag(lstParams.get(0));
        setClickedToCopy(tvInputIdCard,etRecognizeIdCard);
        mPresenter.updateDataBlock(mPresenter.sIdCardNoName,"");


        evInputName.setTag(lstParams.get(1));
        setClickedToCopy(tvInputName,evInputName);
        mPresenter.updateDataBlock(mPresenter.sUserName,"");

        mKeyMapper.put(lstParams.get(0).getName(),etRecognizeIdCard);
        mKeyMapper.put(lstParams.get(1).getName(),evInputName);


        for(int i=2,ni=lstParams.size();i<ni;++i){
            final ApiParam param = lstParams.get(i);
            mPresenter.updateDataBlock(param.getName(),"");
            post(()->{
                CardView vInput = (CardView) LayoutInflater.from(this).inflate(R.layout.view_input,null,false);
                LinearLayout llRoot = (LinearLayout) vInput.getChildAt(0);
                String name = param.getDescription();
                if(name==null||name.isEmpty()||name.equals("?")){
                    name = param.getName();
                }
                TextView tvName  = (TextView) llRoot.getChildAt(0);
                tvName.setText(name);
                if("NOEMPTY".equals(param.getDefaultValue())){
                    tvName.setTextColor(getResources().getColor(R.color.material_red_A400));
                }

                EditText evValue = (EditText) llRoot.getChildAt(1);
                if("NOEMPTY".equals(param.getDefaultValue())) {
                    lstNoneNull.add(evValue);
                }
                evValue.setTag(param);
                lstAllEdits.add(evValue);
                mKeyMapper.put(param.getName(),evValue);
                evValue.setHint("请输入"+name);
                evValue.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        if(hasFocus){
                            evValue.selectAll();
                        }
                        else{
                            evValue.setText(param.getter(param.setter(evValue.getText().toString())));
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
                        mPresenter.updateDataBlock(param.getName(),s.toString());
                    }
                });

                ViewInjector.inject(vInput);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp2px(40f));
                params.topMargin = dp2px(2f);
                llInputDesign.addView(vInput,params);
            });
        }

        post(()->{
            final View v = new View(this);
            final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp2px(80f));
            params.topMargin = dp2px(2f);
            llInputDesign.addView(v,params);
        });
    }

    @Override
    public void onDataChanged(String key, String value) {
        EditText et = mKeyMapper.get(key);
        if(key==null) return;
        if(key.equals(mPresenter.sIdCardNoName)) return;
        if(et==null) return;
        et.setText(value);
        if(mPresenter.sUserName.equals(key) && cbRememberRead.isChecked()){
            ivRead.callOnClick();
        }
    }

    @Override
    public void showRequestingDialog() {
        LoadingDialog.showDialog("requesting_dialog",this);
    }

    @Override
    public void hideRequestingDialog() {
        LoadingDialog.dismissDialog("requesting_dialog");
    }

    @OnClick(R.id.tv_save)
    void onSaveClicked(){
        String name = evInputName.getText().toString();
        if(name.isEmpty()){
            Toast.makeText(this, "请输入【姓名】！", Toast.LENGTH_SHORT).show();
            evInputName.requestFocus();
            return;
        }
        for(int i=0,ni=lstNoneNull.size();i<ni;++i){
            if(lstNoneNull.get(i).getText().toString().isEmpty()){
                ApiParam p = (ApiParam) lstNoneNull.get(i).getTag();
                Toast.makeText(this, "请输入【"+p.getDescription()+"】！", Toast.LENGTH_SHORT).show();
            }
        }
        tvSave.requestFocus();
        mPresenter.setLocalDataFirst(rbLocalFirst.isChecked());
        mPresenter.saveToDatabase();
    }

    @Override
    public void resetView() {
        ToastUtils.show("保存成功！");
        if(isInputOne){
            setResult(RESULT_OK);
            finish();
            return;
        }
        etRecognizeIdCard.setText("");
        evInputName.setText("");
        for(Map.Entry<String,EditText> entry:mKeyMapper.entrySet()){
            post(()->entry.getValue().setText(""));
        }
        etRecognizeIdCard.requestFocus();
        showKeyboard(etRecognizeIdCard);
    }

    @Override
    public void onLoadedDatabase() {
        Toast.makeText(this,"已加载本地记录",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLoadedNetwork() {
        Toast.makeText(this,"已加载网络记录",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.tv_right:
                startActivity(CommunityBatchInputActivity.class);
                break;
        }
    }

    @Override
    protected boolean enableVolumeDownRecognizeIDCard() {
        return true;
    }

    @Override
    protected void onRecognizeCardSuccess(String idCard, String type) {
        idCard = idCard.toUpperCase();
        etRecognizeIdCard.setText(idCard);
        mPresenter.updateDataBlock(mPresenter.sIdCardNoName,idCard);
        mPresenter.requestDatabaseData();
        ToastUtils.show("识别到"+type+":"+idCard);
    }

    @Override
    protected void onRecognizeCardFailure() {
        ToastDialog.showCenter(this,"身份信息识别失败！");
    }

    @Override
    protected boolean onKeyEnterDown() {
        if(etRecognizeIdCard.hasFocus()){
            if (rbLocalFirst.isChecked()){
                mPresenter.requestDatabaseData();
            }
            if(rbNetFirst.isChecked()){
                mPresenter.requestNetData();
            }
            return true;
        }
        if(lstAllEdits.isEmpty()) return false;
        if(evInputName.hasFocus()){
            lstAllEdits.get(0).requestFocus();
            return true;
        }
        for(int i=0,ni=lstAllEdits.size();i<ni;++i){
            if(lstAllEdits.get(i).hasFocus()){
                if(i+1<ni){
                    lstAllEdits.get(i+1).requestFocus();
                    autoScroll();
                    return true;
                }
            }
        }
        return false;
    }
}
