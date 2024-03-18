package com.dreaming.hscj.activity.template.encrypt;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

import com.dreaming.hscj.R;
import com.dreaming.hscj.base.contract.BaseMVPActivity;
import com.dreaming.hscj.core.ViewInjector;
import com.dreaming.hscj.utils.ToastUtils;
import com.dreaming.hscj.utils.algorithm.DecryptUtils;
import com.dreaming.hscj.utils.algorithm.EncryptUtils;
import com.dreaming.hscj.utils.algorithm.SignUtils;
import com.dreaming.hscj.widget.InputView;
import com.dreaming.hscj.widget.ShownView;

import org.apache.poi.ss.formula.functions.T;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class TemplateEncryptActivity extends BaseMVPActivity<TemplateEncryptPresenter> implements ITemplateEncryptContract.View{
    @Override
    public int getContentViewResId() {
        return R.layout.activity_template_encrypt;
    }

    @Override
    protected void onBindView(View vContent) {
        ButterKnife.bind(this,vContent);
    }

    @BindView(R.id.iv_convert_type)
    InputView ivConvertType;

    @BindView(R.id.iv_convert_secret)
    InputView ivConvertSecret;

    @BindView(R.id.iv_convert_method)
    InputView ivConvertMethod;

    @BindView(R.id.iv_convert_src)
    InputView ivConvertSrc;

    @BindView(R.id.sv_convert_dst)
    ShownView ivConvertDst;

    @BindView(R.id.tv_log)
    TextView tvLog;

    String sConvertType;
    String sConvertMethod;
    String sConvertSecret;
    String sConvertSource;
    String sConvertTarget;
    @Override
    public void initView() {
        setCenterText("字段转换调试");

        setRightText("新开窗口");

        tvTitleLeft.setOnClickListener(v->startActivity(TemplateEncryptActivity.class));

        ivConvertType.tvValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                sConvertType = s.toString().trim().toUpperCase();
                if("ENCRYPT".equals(sConvertType)) return;
                if(sConvertType.startsWith("E")){
                    sConvertType = "ENCRYPT";
                    ivConvertType.tvValue.setText(sConvertType);
                    return;
                }
                if("SIGN".equals(sConvertType)) return;
                if(sConvertType.startsWith("S")){
                    sConvertType = "SIGN";
                    ivConvertType.tvValue.setText(sConvertType);
                    return;
                }
                if("DECRYPT".equals(sConvertType)) return;
                if(sConvertType.startsWith("D")){
                    sConvertType = "DECRYPT";
                    ivConvertType.tvValue.setText(sConvertType);
                    return;
                }
                sConvertType = "";
                ivConvertType.tvValue.setText(sConvertType);
            }
        });

        ivConvertMethod.tvValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                sConvertMethod = s.toString();
            }
        });
        ivConvertSecret.tvValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                sConvertSecret = s.toString();
            }
        });
        ivConvertSrc.tvValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                sConvertSource = s.toString();
            }
        });

        ivConvertDst.tvValue.setOnClickListener(v -> ViewInjector.copyClipboard(ivConvertDst.tvValue.getText().toString()));
    }

    @OnClick(R.id.tv_convert)
    void onConvertClicked(){
        StringBuilder sbLog = new StringBuilder();

        if(sConvertSource == null || sConvertSource.isEmpty()){
            ToastUtils.show("请输入待加密原串！");
            showKeyboard(ivConvertSrc.tvValue);
            return;
        }

        if(sConvertSecret == null ) sConvertSecret = "";

        if(sConvertMethod == null ) sConvertMethod = "";

        sConvertTarget = "";

        try {
            if("ENCRYPT".equals(sConvertType)){
                sbLog.append("开始加密\n");
                sConvertTarget = EncryptUtils.encrypt(sConvertSource,sConvertSecret,sConvertMethod);
            }
            else if("DECRYPT".equals(sConvertType)){
                sbLog.append("开始解密\n");
                sConvertTarget = DecryptUtils.decrypt(sConvertSource,sConvertSecret,sConvertMethod);
            }
            else if("SIGN".equals(sConvertType)){
                sbLog.append("开始签名\n");
                sConvertTarget = SignUtils.sign(sConvertSource,sConvertSecret,sConvertMethod);
            }
            else {
                ToastUtils.show("不支持的转换类型！");
                showKeyboard(ivConvertType.tvValue);
                return;
            }
            sbLog.append("转换成功!\n");
            ToastUtils.show("转换完成！");
        } catch (Exception e) {
            sbLog.append(e.getMessage()).append("\n");
        }
        finally {
            tvLog.setText(sbLog.toString());
            ivConvertDst.tvValue.setText(sConvertTarget);
        }
    }
}
