package com.dreaming.hscj.activity.template.test;

import android.graphics.Color;
import android.graphics.Point;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dreaming.hscj.R;
import com.dreaming.hscj.activity.template.encrypt.TemplateEncryptActivity;
import com.dreaming.hscj.base.contract.BaseMVPActivity;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.template.api.impl.Api;
import com.dreaming.hscj.template.api.impl.ApiListener;
import com.dreaming.hscj.template.api.wrapper.ApiParam;
import com.dreaming.hscj.utils.DensityUtils;
import com.dreaming.hscj.utils.ToastUtils;
import com.dreaming.hscj.widget.InputView;

import java.security.InvalidParameterException;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import priv.songxusheng.easydialog.EasyDialog;
import priv.songxusheng.easydialog.EasyDialogHolder;
import priv.songxusheng.easyjson.ESONObject;

public class ApiTestActivity extends BaseMVPActivity<ApiTestPresenter> implements IApiTestContract.View{
    @Override
    public int getContentViewResId() {
        return R.layout.activity_api_test;
    }

    @Override
    protected void onBindView(View vContent) {
        ButterKnife.bind(this,vContent);
    }

    @BindView(R.id.iv_type)
    InputView ivApiType;

    @BindView(R.id.ll_content)
    LinearLayout llContent;

    @BindView(R.id.tv_do_request)
    TextView tvDoRequest;

    @Override
    public void initView() {
        setCenterText("Api请求测试");
        setRightText("字段转换测试");

        String pwd = getIntent().getStringExtra("password");
        if(!Template.getCurrentTemplate().getUserInputGuideDatabase().isValidLoginPassword(pwd)){
            finish();
        }

        tvTitleRight.setOnClickListener(v -> startActivity(TemplateEncryptActivity.class));

        tvDoRequest.setVisibility(View.GONE);

        ivApiType.tvValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            long lClickTime = 0L;
            @Override
            public void afterTextChanged(Editable s) {
               String s1 = s.toString();
                try {
                    Integer type = Integer.valueOf(s1);
                    if(type<0 || type>6) throw new InvalidParameterException();
                    mPresenter.setType(type);
                    lClickTime = System.currentTimeMillis();

                    postDelayed(()->{
                        long now = System.currentTimeMillis();
                        if(now - lClickTime <1700L) return;
                        buildViewTree(type);
                    },1800);
                } catch (Exception e) {
                    ToastUtils.show("只能填写0~6之内的请求类型！");
                }
            }
        });


    }

    ESONObject eHeaders = new ESONObject();
    ESONObject eParams  = new ESONObject();

    void buildTitle(String text){
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#000000"));
        tv.getPaint().setFakeBoldText(true);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP,15f);
        tv.setGravity(Gravity.CENTER_VERTICAL|Gravity.LEFT);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp2px(30));

        llContent.addView(tv,params);
    }

    void buildItem(ApiParam param, boolean bIsHeader, boolean bIsAuth){
        InputView iv = new InputView(this);
        iv.tvName.setText(param.getDescription());
        if(ApiParam.DEFAULT_NO_EMPTY.equals(param.getDefaultValue())){
            iv.tvName.setTextColor(Color.parseColor("#b71c1c"));
        }
        if(param.getDefaultValue()!=null && !param.getDefaultValue().equals(ApiParam.DEFAULT_NO_EMPTY)){
            iv.tvValue.setText(param.getDefaultValue());
            if(bIsHeader){
                eHeaders.putValue(param.getName(),param.getDefaultValue());
                iv.tvValue.setEnabled(bIsAuth);
            }
            else{
                eParams.putValue(param.getName(),param.getDefaultValue());
            }
        }
        iv.tvValue.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus){
                    iv.tvValue.setText(param.getter(param.setter(iv.tvValue.getText().toString())));
                }
            }
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp2px(30));
        params.topMargin = dp2px(1);
        llContent.addView(iv,params);
    }

    void buildViewTree(int type){
        llContent.removeAllViews();

        Api api = Template.getCurrentTemplate().apiOf(type);
        String auth = api.getRequest().getAuthorization();


        buildTitle("请求类型："+api.getRequest().getType());

        buildTitle("请求方式："+api.getRequest().getUpload());

        buildTitle("请求头部信息");
        eHeaders = new ESONObject();

        List<ApiParam> lstHeaders= Template.getCurrentTemplate().apiOf(type).getRequest().getHeaders();
        for(int i=0,ni=lstHeaders.size();i<ni;++i){
            ApiParam p = lstHeaders.get(i);
            buildItem(p,true, auth!=null && auth.equals(p.getName()));
        }

        buildTitle("请求参数信息");
        eParams = new ESONObject();

        List<ApiParam> lstParams = Template.getCurrentTemplate().apiOf(type).getRequest().getParams();
        for(int i=0,ni=lstParams.size();i<ni;++i){
            ApiParam p = lstParams.get(i);
            buildItem(p,false,false);
        }

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp2px(100));
        llContent.addView(new View(this),params);

        tvDoRequest.setVisibility(View.VISIBLE);
    }

    void showApiRequestResult(String text, StringBuilder sb){
        Point p = DensityUtils.getScreenSize();
        new EasyDialog(R.layout.dialog_api_request_result,this)
            .setOnBindDialogListener(new EasyDialog.OnBindDialogListener() {
                @Override
                public void onBindDialog(EasyDialogHolder holder) {
                    holder.setOnClickListener(R.id.iv_close, v -> holder.dismissDialog());
                    holder.setText(R.id.tv_log,text+"\n\n"+sb.toString());
                }
            })
            .setDialogParams(p.x,p.y - dp2px(80),Gravity.BOTTOM)
            .setForegroundResource(R.drawable.shape_dialog_corner_with_tl_tr)
            .setAllowDismissWhenTouchOutside(false)
            .showDialog();
    }


    @OnClick(R.id.tv_do_request)
    void onDoRequestClicked(){
        tvDoRequest.requestFocus();
        mPresenter.doRequest(eHeaders, eParams, new ApiListener() {
            @Override
            public void onSuccess(int code, Object msg, StringBuilder sbLog) {
                ToastUtils.show("请求成功！");
                showApiRequestResult("请求成功！",sbLog);
            }

            @Override
            public void onFailure(int code, Object msg, StringBuilder sbLog) {
                ToastUtils.show("请求失败！");
                showApiRequestResult("请求失败！",sbLog);
            }
        });
    }
}
