package com.dreaming.hscj.activity.login;

import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dreaming.hscj.Constants;
import com.dreaming.hscj.R;
import com.dreaming.hscj.activity.template.adapt.TemplateAdaptActivity;
import com.dreaming.hscj.base.BaseActivity;
import com.dreaming.hscj.base.ToastDialog;
import com.dreaming.hscj.base.contract.BaseMVPActivity;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.template.api.ApiTemplate;
import com.dreaming.hscj.template.api.impl.ApiConfig;
import com.dreaming.hscj.template.database.DatabaseTemplate;
import com.leon.lfilepickerlibrary.utils.Constant;

import org.json.JSONArray;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import priv.songxusheng.easyjson.ESONArray;

public class LoginActivity extends BaseMVPActivity<LoginPresenter> implements ILoginContract.View {
    private static final String TAG = LoginActivity.class.getSimpleName();
    @Override
    public int getContentViewResId() {
        return R.layout.activity_login;
    }

    public interface ILoginListener{
        void onSuccess();
        void onFailure();
    }

    public static void doLogin(BaseActivity activity, ILoginListener listener){
        activity.startActivityForResult(LoginActivity.class, 1024, (resultCode, data) -> {
            if(resultCode == activity.RESULT_OK){
                listener.onSuccess();
            }
            else{
                listener.onFailure();
            }
        });
    }

    public static void doLogout(){
        Constants.User.setAutoLogin(false);
        Constants.User.setToken("");
        Constants.User.setExpired(0L);
    }

    @BindView(R.id.cb_remember_password)
    CheckBox cbRememberPassword;

    @BindView(R.id.cb_auto_login)
    CheckBox cbAutoLogin;

    @BindView(R.id.tv_adapt_region)
    TextView tvAdaptRegion;

    @Override
    protected void onBindView(View vContent) {
        ButterKnife.bind(this,vContent);
    }

    @Override
    public boolean hasTitleBar() {
        return false;
    }

    @Override
    public void initView() {
        ApiConfig.Permission permission = Template.getCurrentTemplate().getApiConfig().getPermission();
        if(!permission.getBAllowAutoLogin()) cbAutoLogin.setVisibility(View.GONE);
        if(!permission.getBAllowRememberPassword()) {
            cbAutoLogin.setVisibility(View.GONE);
            cbRememberPassword.setVisibility(View.GONE);
        }
        if(Constants.User.isRememberPassword()){
            String account = Constants.User.getAccount();
            if(!account.isEmpty()){
                etAccount.setText(account);
            }
            String pwd = Constants.User.getPassword();
            if(!pwd.isEmpty()){
                etPassword.setText("记住密码");
            }
            cbRememberPassword.setChecked(true);
        }
        if(Constants.User.isAutoLogin()){
            cbAutoLogin.setChecked(true);
            onLoginClicked();
            Toast.makeText(this, "开始自动登录！", Toast.LENGTH_LONG).show();
        }
        cbRememberPassword.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                cbAutoLogin.setChecked(false);
            }
        });
        tvAdaptRegion.setText(Template.getCurrentTemplate().getDatabaseSetting().getRegionName());
    }

    @OnClick(R.id.tv_app_adapt)
    void onApiAdaptClicked(){
        startActivity(TemplateAdaptActivity.class);
    }

    @BindView(R.id.et_user_name)
    EditText etAccount;
    @BindView(R.id.edt_pwd)
    EditText etPassword;
    @BindView(R.id.img_pwd_visibility)
    ImageView ivPwdVisibility;

    private boolean bIsFirstLogin = true;

    @OnClick(R.id.img_pwd_visibility)
    void onChangePasswordVisibilityClicked(){
        if (EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD == etPassword.getInputType()) {
            //如果不可见就设置为可见
            ivPwdVisibility.setImageDrawable(getResources().getDrawable(R.drawable.ic_login_pwd_invisible));
            etPassword.setInputType(EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
            etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
        } else {
            //如果可见就设置为不可见
            ivPwdVisibility.setImageDrawable(getResources().getDrawable(R.drawable.ic_login_pwd_visible));
            etPassword.setInputType(EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
        }
        //执行上面的代码后光标会处于输入框的最前方,所以把光标位置挪到文字的最后面
        etPassword.setSelection(etPassword.getText().toString().length());
    }

    @OnClick(R.id.tv_login)
    void onLoginClicked(){
        ESONArray hist = Constants.User.getLoginHistory();
        int count = 0;
        long max = 0L;
        long now = System.currentTimeMillis();
        for(int i=0,ni=hist.length();i<ni;++i){
            long time = hist.getArrayValue(i,0L);
            max = Math.max(max,time);
            if(now - time < 3600000L){
                ++count;
            }
        }
        if(count>2 && now - max<60000L){
            ToastDialog.showBottom(this,String.format("登录过于频繁，请%d秒后再次重试！",(60-(now - max)/1000L)));
            return;
        }
        String userName = etAccount.getText().toString();
        if(userName.isEmpty()){
            ToastDialog.showCenter(this,"请填写登录账号！");
            etAccount.requestFocus();
            return;
        }
        mPresenter.setUserName(userName);

        if(bIsFirstLogin && Constants.User.isRememberPassword() && !Constants.User.getPassword().isEmpty()){
            mPresenter.setUserPassword(Constants.User.getPassword());
        }
        else{
            String userPassword = etPassword.getText().toString();
            if(userPassword.isEmpty()){
                ToastDialog.showCenter(this,"请填写登录密码！");
                etPassword.requestFocus();
                return;
            }
            mPresenter.setUserPassword(userPassword);
        }

        mPresenter.requestLogin();
    }

    @Override
    public void onLoginSuccess() {
        ApiConfig.Permission permission = Template.getCurrentTemplate().getApiConfig().getPermission();
        if(permission.getBAllowRememberPassword()){
            Constants.User.setAccount(mPresenter.getUserName());
            Constants.User.setPassword(mPresenter.getUserPassword());
        }
        Constants.User.setRememberPassword(cbRememberPassword.isChecked());
        Constants.User.setAutoLogin(cbAutoLogin.isChecked());
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void onLoginFailure() {
        Constants.User.addLoginHistory(System.currentTimeMillis());
        ToastDialog.showCenter(this,"登陆失败，请检查账号和密码，并保证网络通畅！");
    }
}
