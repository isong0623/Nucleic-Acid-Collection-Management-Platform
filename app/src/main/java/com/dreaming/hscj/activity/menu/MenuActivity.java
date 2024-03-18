package com.dreaming.hscj.activity.menu;

import android.graphics.Point;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.dreaming.hscj.App;
import com.dreaming.hscj.Constants;
import com.dreaming.hscj.R;
import com.dreaming.hscj.activity.community.ConfigDatabaseActivity;
import com.dreaming.hscj.activity.donate.DonateActivity;
import com.dreaming.hscj.activity.login.LoginActivity;
import com.dreaming.hscj.activity.nucleic_acid.searching.LocalSamplingDialog;
import com.dreaming.hscj.activity.template.adapt.TemplateAdaptActivity;
import com.dreaming.hscj.base.BaseFragment;
import com.dreaming.hscj.base.ToastDialog;
import com.dreaming.hscj.base.contract.BaseMVPActivity;
import com.dreaming.hscj.core.EasyAdapter.EasyAdapter;
import com.dreaming.hscj.core.ThreadPoolProvider;
import com.dreaming.hscj.core.ocr.IDCardRecognizer;
import com.dreaming.hscj.dialog.loading.LoadingDialog;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.template.database.impl.NoneGroupingDatabase;
import com.dreaming.hscj.template.database.wrapper.DatabaseConfig;
import com.dreaming.hscj.template.database.wrapper.DatabaseSetting;
import com.dreaming.hscj.utils.DensityUtils;
import com.dreaming.hscj.utils.JsonUtils;
import com.dreaming.hscj.utils.ToastUtils;
import com.dreaming.hscj.widget.AppBarView;
import com.dreaming.hscj.widget.FixedViewPager;
import com.dreaming.security.Defender;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.File;
import java.security.Security;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import priv.songxusheng.easydialog.EasyDialog;
import priv.songxusheng.easydialog.EasyDialogHolder;
import priv.songxusheng.easyjson.ESONObject;

public class MenuActivity extends BaseMVPActivity<MenuPresenter> implements IMenuContract.View, View.OnClickListener {

    @Override
    public int getContentViewResId() {
        return R.layout.activity_menu;
    }

    private static final String TAG = MenuActivity.class.getSimpleName();

    @Override
    protected void onBindView(View vContent) {
        ButterKnife.bind(this,vContent);
    }

    void setupTitleBar(){
        setTitleBarColor(R.color.app_theme_color);
        setStatusBarColor((Object) R.color.app_theme_color);
        setLeftImageVisibility(View.GONE);
        setCenterText("全民核酸采集管理平台");
        setCenterTextColor("#ffffff");
        setRightImageDrawable(R.drawable.ic_menu_sponsor);
        setLeftImageDrawable(R.drawable.ic_menu_config_db);
        imgTitleRight.setOnClickListener(this);
        imgTitleLeft.setOnClickListener(this);
        tvTitleCenter.setOnClickListener(this);
    }

    @BindView(R.id.view_pager)
    ViewPager viewPager;

    @BindView(R.id.appbar_na)
    AppBarView appBarViewNA;
    @BindView(R.id.appbar_community)
    AppBarView appBarViewCommunity;
    @BindView(R.id.appbar_other)
    AppBarView appBarViewOther;

    FragmentPagerAdapter adapter;
    List<BaseFragment> lstFragments = null;
    AppBarView appBarViews[];
    int iAppBarLastIndex = 0;
    Class clzsFragments[] = new Class[]{
        NAFragment.class,
        CommunityFragment.class,
        OtherFragment.class
    };

    void setupContent(){
        appBarViews = new AppBarView[3];
        appBarViews[0] = appBarViewNA;
        appBarViews[1] = appBarViewCommunity;
        appBarViews[2] = appBarViewOther;

        if (lstFragments != null&&lstFragments.size()>0) {
            for(int i=0;i<5;++i){
                BaseFragment.releaseInstance(clzsFragments[i]);
            }
            lstFragments.clear();
            if(adapter!=null) adapter.notifyDataSetChanged();
        }

        lstFragments = new ArrayList<BaseFragment>(3) {{
            for(int i=0;i<3;++i){
                add(BaseFragment.getInstance(clzsFragments[i]));
            }
        }};

        viewPager.setAdapter(adapter = new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int i) {
                return lstFragments.get(i);
            }
            @Override
            public int getCount() {
                return lstFragments.size();
            }
        });
        viewPager.setOffscreenPageLimit(lstFragments.size());
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }
            @Override
            public void onPageSelected(int position) {
                lstFragments.get(position).onFragmentResume();
                lstFragments.get(position).notifyInitFragment();
                onAppBarClicked(appBarViews[position]);
            }
            @Override
            public void onPageScrollStateChanged(int state) {}
        });
    }

    @OnClick({R.id.appbar_na,R.id.appbar_community,R.id.appbar_other})
    void onAppBarClicked(final View v){
        int targetIndex = 0;
        switch (v.getId()){
            case R.id.appbar_na:
                targetIndex = 0;
                break;
            case R.id.appbar_community:
                targetIndex = 1;
                break;
            case R.id.appbar_other:
                targetIndex = 2;
                break;
        }
        final int fTargetIndex = targetIndex;
        appBarViews[iAppBarLastIndex].setSelected(false);
        appBarViews[fTargetIndex].setSelected(true);
        iAppBarLastIndex = fTargetIndex;
        App.PostDelayed(()-> setCurrentItem(fTargetIndex),100L);
    }

    void setCurrentItem(int position){
//        showFragment(instance.getSupportFragmentManager(),lstFragments.get(position),R.id.root_frame);
        viewPager.setCurrentItem(position,true);
    }

    @Override
    public void initView() {
        LoadingDialog.showDialog("DumpTemplateInfo",this);
        setupTitleBar();
        setupContent();
    }

    @Override
    public void initData() {
        eInitTemplate = Constants.TemplateConfig.getCurrentTemplate();

        File fDumpDir = new File(getFilesDir().getParentFile(),"/dump/");
        if(!fDumpDir.exists()) fDumpDir.mkdirs();
        if(!fDumpDir.exists()){
            ToastUtils.show("创建初始化文件夹失败！");
        }
        Defender.workup(App.sInstance);

        initialize();
    }

    ESONObject eInitTemplate = Constants.TemplateConfig.getCurrentTemplate();
    void initialize(){
        App.PostDelayed(()->{
            Log.e(TAG,"Starting work up!");
            final File fDump = new File(getFilesDir().getParentFile(),"/dump/template.dump");
            if(!fDump.exists()){
                initialize();
                return;
            }

            Log.e(TAG,"work up success!");
            ThreadPoolProvider.getFixedThreadPool().execute(()->{
                try {
                    Template.init(fDump.getAbsolutePath());
                    if(eInitTemplate.length()!=0){
                        String path = eInitTemplate.getJSONValue("path","");
                        try { Template.read(path).setCurrentTemplate(); } catch (Exception e) { }
                    }

                    mPresenter.onInitSuccess();
                    post(()->LoadingDialog.dismissDialog("DumpTemplateInfo"));
                    post(()->showVerifyTemplatePasswordDialog());
                    if(Constants.User.isLogin()){
                        Template.getCurrentTemplate().getUserOverallDatabase().sync();
                    }
                    Template.getCurrentTemplate().getNoneGroupingDatabase().sync(new NoneGroupingDatabase.ISyncListener() {
                        @Override
                        public void onStart() { }

                        @Override
                        public void onProgress(int progress) { }

                        @Override
                        public void onError(String err) {
                            ToastUtils.show("未分组数据库同步失败!");
                        }

                        @Override
                        public void onFinish() {
                            post(()->LoadingDialog.dismissDialog("DumpTemplateInfo"));
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    App.Post(()->ToastUtils.show("此版本可能为盗版！"));
                }
            });
        },1000L);
    }

    @Override
    public void onClick(View v) {
        if(v==null) return;
        switch (v.getId()){
            case R.id.tv_right:
            case R.id.iv_right:
                startActivity(DonateActivity.class);
                break;
            case R.id.tv_center:
                IDCardRecognizer.recognize(this, new IDCardRecognizer.IDCardRecognizeListener() {
                    @Override
                    public void onSuccess(String id, String type) {

                    }

                    @Override
                    public void onFailure() {

                    }
                });
//                startActivity(CommunityBatchInputConfigActivity.class);
//                startActivity(ZxingActivity.class);
                break;
            case R.id.iv_left:
                if(Constants.DBConfig.getAllDatabase().length() == 0){
                    DatabaseConfig.showCreateDatabaseDialog(this, new DatabaseConfig.IDBCreateListener() {
                        @Override
                        public void onSuccess(String townName, String villageName) {
                            Constants.DBConfig.setSelectedDatabase(1);
                            ToastUtils.show("数据库创建成功！");
                        }

                        @Override
                        public void onFailure() {
                            ToastUtils.show("数据库创建失败！");
                        }
                    });
                    return;
                }
                startActivity(ConfigDatabaseActivity.class);
                break;
        }
    }

    long lLastBackPressedTimestamp = 0L;
    @Override
    public void onBackPressed() {
        long now = System.currentTimeMillis();
        if(now - lLastBackPressedTimestamp>2000L){
            ToastUtils.show("再按一次退出程序！");
            lLastBackPressedTimestamp = now;
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        try { lstFragments.get(iAppBarLastIndex).onFragmentResume(); } catch (Exception e) { }
        try {
            if(mPresenter.isInitializing()) return;
            showVerifyTemplatePasswordDialog();
        } finally {
            super.onResume();
        }
    }

    boolean bIsShownVerifyDialog = false;
    public void showVerifyTemplatePasswordDialog(){
        if(true) return;
        if(bIsShownVerifyDialog) return;
        if(Constants.TemplateConfig.isTemplateVerify()) return;
        if(!Template.getCurrentTemplate().getDatabaseSetting().isEnableEnterPassword()) return;
        bIsShownVerifyDialog = true;
        Point p = DensityUtils.getScreenSize();
        new EasyDialog(R.layout.dialog_password_verify_or_create_template,this)
                .setOnBindDialogListener(new EasyDialog.OnBindDialogListener() {
                    TextView tvVerify;
                    TextView tvTemplateUnique;
                    TextView tvTemplateInfo;

                    private void setBtnEnabled(boolean enabled, long time){
                        Log.e(TAG,"setBtnEnabled->"+enabled+"  "+time);
                        if(enabled){
                            tvVerify.setText("验证口令");
                            tvVerify.setEnabled(true);
                            tvVerify.getPaint().setFakeBoldText(true);
                            bIsInLoop = false;
                            return;
                        }
                        tvVerify.setEnabled(false);
                        tvVerify.getPaint().setFakeBoldText(false);
                        tvVerify.setText(String.format("%d秒后可验证",time));
                    }

                    boolean bIsInLoop = false;
                    private void autoLoop(){
                        if(bIsInLoop) {
                            Log.e(TAG,"autoLoop->break1");
                            return;
                        }
                        bIsInLoop = true;
                        if(tvVerify == null) {
                            bIsInLoop = false;
                            Log.e(TAG,"autoLoop->break2");
                            return;
                        }

                        List<Long> arr = JsonUtils.parse2List(Constants.TemplateConfig.getVerifyHistory(),0L);
                        Collections.sort(arr);

                        Log.e(TAG,"autoLoop->arr:"+arr);
                        if(arr == null || arr.size() < 3) {
                            Log.e(TAG,"autoLoop->break3");
                            setBtnEnabled(true,0);
                            return;
                        }
                        long last = arr.get(arr.size() - 3);
                        long now  = System.currentTimeMillis();
                        if(now - last > 3600000L){
                            Log.e(TAG,"autoLoop->break4");
                            setBtnEnabled(true,0);
                            return;
                        }
                        last = arr.get(arr.size()-1);
                        last = now - last;
                        if(last>300000){
                            Log.e(TAG,"autoLoop->break5");
                            setBtnEnabled(true,0);
                            return;
                        }
                        setBtnEnabled(false,(300000L - last)/1000L);
                        postDelayed(()->{
                            bIsInLoop = false;
                            autoLoop();
                        },1000L);
                    }

                    private void reloadTemplateInfo(){
                        DatabaseSetting setting = Template.getCurrentTemplate().getDatabaseSetting();
                        String unique = setting.getUnifySocialCreditCodes();
                        if(unique.replaceAll("0","").length() == 0){
                            unique = "******************";
                        }
                        else{
                            unique = unique.substring(0,4)+"**********"+unique.substring(14);
                        }

                        unique = setting.getRegionCode()+"/"+unique ;

                        tvTemplateUnique.setText(unique);
                        tvTemplateInfo  .setText(setting.getRegionName());
                        autoLoop();
                    }

                    @Override
                    public void onBindDialog(EasyDialogHolder holder) {
                        ImageView ivPwdVisibility    = holder.getView(R.id.img_pwd_visibility);
                        EditText etPassword          = holder.getView(R.id.edt_pwd);
                        tvTemplateUnique    = holder.getView(R.id.tv_template_unique);
                        tvTemplateInfo      = holder.getView(R.id.tv_template_info);

                        ivPwdVisibility.setOnClickListener(v -> {
                            int selected = etPassword.getSelectionStart();
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

                            try {
                                etPassword.setSelection(selected);
                            } catch (Exception e) {
                                //执行上面的代码后光标会处于输入框的最前方,所以把光标位置挪到文字的最后面
                                etPassword.setSelection(etPassword.getText().toString().length());
                            }
                        });

                        TextView tvAddTemplate = holder.getView(R.id.tv_add_template);
                        tvAddTemplate.setOnClickListener(v -> TemplateAdaptActivity.doAdapt(MenuActivity.this, new TemplateAdaptActivity.IAdaptCallback() {
                            @Override
                            public void onAdaptSuccess() {
                                reloadTemplateInfo();
                            }

                            @Override
                            public void onAdaptCancel() {
                                reloadTemplateInfo();
                            }
                        }));

                        tvVerify = holder.getView(R.id.tv_verify);
                        tvVerify.setOnClickListener(v -> {
                            String pwd = etPassword.getText().toString();
                            if(pwd.isEmpty()){
                                showKeyboard(etPassword);
                                ToastUtils.show("请输入口令！");
                                return;
                            }
                            if(Template.getCurrentTemplate().getDatabaseSetting().getPassword().equals(pwd)){
                                Constants.TemplateConfig.setTemplateVerify(true);
                                ToastUtils.show("口令正确！");
                                bIsShownVerifyDialog = false;
                                holder.dismissDialog();
                                return;
                            }
                            ToastUtils.show("口令错误！");
                            Constants.TemplateConfig.addVerifyHistory(System.currentTimeMillis());
                            autoLoop();
                        });
                        reloadTemplateInfo();
                    }
                })
                .setDialogWidth(p.x/6*5)
                .setDialogHeight(dp2px(240f))
                .setAllowDismissWhenBackPressed(false)
                .setAllowDismissWhenTouchOutside(false)
                .setForegroundResource(R.drawable.shape_template_dialog)
                .showDialog();
    }
}
