package com.dreaming.hscj.activity.system;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.dreaming.hscj.Constants;
import com.dreaming.hscj.R;
import com.dreaming.hscj.base.BaseActivity;
import com.dreaming.hscj.dialog.DialogManager;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.utils.ToastUtils;
import com.tencent.bugly.crashreport.CrashReport;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class FeedbackActivity extends BaseActivity {
    @Override
    public int getContentViewResId() {
        return R.layout.activity_feedback;
    }

    @Override
    protected void onBindView(View vContent) {
        ButterKnife.bind(this,vContent);
    }

    @BindView(R.id.et_feedback)
    EditText etFeedback;

    @BindView(R.id.tv_submit)
    TextView tvSubmit;

    @Override
    public void initView() {
        setCenterText("反馈建议");
        etFeedback.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString();
                if(text.length()>1000){
                    etFeedback.setText(text.substring(0,1000));
                    ToastUtils.show("反馈内容不能超过1000字！");
                }
            }
        });
    }

    @OnClick(R.id.tv_submit)
    void onFeedbackClicked(){
        String feedback = etFeedback.getText().toString();
        if(feedback.trim().isEmpty()){
            ToastUtils.show("请输入反馈内容！");
            showKeyboard(etFeedback);
            return;
        }
        long last = Constants.User.getLastFeedbackTimestamp();
        long now  = System.currentTimeMillis();

        if(now - last < 24*3600*1000L){
            ToastUtils.show("距离上次反馈时间不足24小时，不能反馈！");
            return;
        }
        DialogManager.showAlertDialog(this,"提示","请保持网络通畅，24小时内只能反馈一次，确定要反馈吗？",null, v -> {
            Constants.User.setLastFeedbackTimestamp(now);
            CrashReport.postCatchedException(new Throwable(Template.getCurrentTemplate().getDatabaseSetting().getSPUnify()+"\n\n"+feedback));
            ToastUtils.show("开始反馈，请不要立即关闭程序，并保持网络通畅10s以上。");
        });

    }
}
