package com.dreaming.hscj.base;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.dreaming.hscj.App;
import com.dreaming.hscj.BuildConfig;
import com.dreaming.hscj.R;
import com.dreaming.hscj.utils.EasyPermission;
import com.dreaming.hscj.utils.InternetUtils;
import com.dreaming.hscj.utils.StatusBarUtil;
import com.dreaming.hscj.utils.SystemBarTintManager;
import com.google.android.material.appbar.AppBarLayout;
import com.scwang.smart.refresh.footer.BallPulseFooter;
import com.scwang.smart.refresh.header.MaterialHeader;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnLoadMoreListener;
import com.scwang.smart.refresh.layout.listener.OnRefreshListener;

import org.apache.poi.ss.formula.functions.T;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BaseActivity extends AppCompatActivity implements OnRefreshListener, OnLoadMoreListener {

    private String TAG;

    /**
     * SmartRefreshLayout预加载
     */
    static {
        SmartRefreshLayout.setDefaultRefreshHeaderCreator((context, layout) -> {
            layout.setPrimaryColorsId(R.color.app_theme_color, android.R.color.white);
            MaterialHeader  header = new MaterialHeader(context);
            header.setProgressBackgroundColorSchemeResource(android.R.color.white);
            header.setColorSchemeResources(R.color.app_theme_color);
            return header;
        });
        SmartRefreshLayout.setDefaultRefreshFooterCreator((context, layout) ->{
            layout.setPrimaryColorsId(R.color.app_theme_color, android.R.color.white);
            BallPulseFooter footer = new BallPulseFooter(context);
            footer.setBackgroundColor(Color.TRANSPARENT);
            footer.setAnimatingColor(context.getResources().getColor(R.color.app_theme_color));
            footer.setNormalColor(Color.TRANSPARENT);
            return footer;
        });
    }

    final private static Map<Long, Object> paramsManager = new HashMap<>();
    private static long lParamsId = 0;
    protected static long putParams(Object param){
        long id = ++lParamsId;
        paramsManager.put(id,param);
        return id;
    }
    protected static Object getParams(long id){
        return paramsManager.get(id);
    }
    protected static void releasePamrams(long id){
        paramsManager.remove(id);
    }

    protected boolean isPreLoadMode(){
        return false;
    }
    //region swipeBackFinish
//    @Override
//    public boolean dispatchTouchEvent(MotionEvent ev) {
//        boolean processTouchEvent = SwipeHelper.instance().processTouchEvent(ev);
//        if (processTouchEvent) {
//            return true;
//        }
//        return super.dispatchTouchEvent(ev);
//    }
    //endregion

    //region 生命周期
    protected Context context;
    protected View vStatusBar;
    public static int SCREEN_HEIGHT=0,SCREEN_WIDTH=0,STATUS_BAR_HEIGHT=0;

    public  @LayoutRes int getContentViewResId(){ return View.NO_ID;}
    public void initView(){}
    public void initData(){}
    public void initLazyView(){}
    public void initLazyData(){}

    public boolean hasTitleBar(){
        return true;
    }
    public ConstraintLayout clTitleBar;
    public TextView tvTitleLeft,tvTitleCenter,tvTitleRight;
    public ImageView imgTitleLeft,imgTitleCenter,imgTitleRight;
    public View vTitleLine,vTitleLeft;
    protected ConstraintLayout vParent = null;
    protected View vContentView;
    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        TAG = this.getClass().getSimpleName();

        if(getContentViewResId()!= View.NO_ID){
            if(SCREEN_HEIGHT==0){
                WindowManager manager = this.getWindowManager();
                DisplayMetrics outMetrics = new DisplayMetrics();
                manager.getDefaultDisplay().getMetrics(outMetrics);
                SCREEN_WIDTH = outMetrics.widthPixels;
                SCREEN_HEIGHT = outMetrics.heightPixels;
                STATUS_BAR_HEIGHT = StatusBarUtil.getStatusBarHeight(context);
            }
            initSystemBarTint();
            StatusBarUtil.setLightStatusBar(this,getForegroundStatusBarColor(),true);
            supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
            if(isPreLoadMode()) return;
            vContentView = LayoutInflater.from(this).inflate(getContentViewResId(),null);
            if(hasTitleBar()){
                vParent = (ConstraintLayout) LayoutInflater.from(this).inflate(R.layout.base_title,null).findViewById(R.id.cl_root);
                clTitleBar = vParent.findViewById(R.id.cl_title_bar);
                tvTitleLeft = vParent.findViewById(R.id.tv_left);
                tvTitleCenter = vParent.findViewById(R.id.tv_center);
                tvTitleRight = vParent.findViewById(R.id.tv_right);
                imgTitleLeft = vParent.findViewById(R.id.iv_left);
                vTitleLeft = vParent.findViewById(R.id.v_left);
                imgTitleCenter = vParent.findViewById(R.id.iv_center);
                imgTitleRight = vParent.findViewById(R.id.iv_right);
                vTitleLine = vParent.findViewById(R.id.v_title_line);
                vTitleLeft.setOnClickListener(v -> finish());
                tvTitleLeft.setOnClickListener(v -> finish());
                vStatusBar = vParent.findViewById(R.id.v_status_bar);
                srlMain = vParent.findViewById(R.id.srl_main);
                srlMain.setEnableRefresh(hasRefreshBar());
                srlMain.setEnableLoadMore(hasRefreshBar());
                srlMain.setEnabled(hasRefreshBar());
                srlMain.setOnRefreshListener(this);
                srlMain.setOnLoadMoreListener(this);
                vStatusBar.getLayoutParams().height = StatusBarUtil.getStatusBarHeight(this);
                vStatusBar.requestLayout();
                FrameLayout flRoot = vParent.findViewById(R.id.fl_content);
                flRoot.addView(vContentView,new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            }
            super.setContentView(hasTitleBar()?vParent:vContentView);
        }
        else {
            super.setContentView(layoutResID);
        }
        onInitView();
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        onInitView();
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(view, params);
        onInitView();
    }

    protected Handler mHandler = null;
    private boolean bIsInitView = false;
    private final void onInitView(){
        if(bIsInitView) return;
        bIsInitView = true;
        if(mHandler!=null) mHandler.post(()->onBindView(getWindow().getDecorView().findViewById(android.R.id.content)));
        if(mHandler!=null) mHandler.post(()->initView());
        if(mHandler!=null) mHandler.post(()->initData());
        if(isResumed){
            onInitLazy();
        }
    }

    private void onInitLazy(){
        if(bIsInitView){
            if(bIsInitLazy) return;
            bIsInitLazy = true;
            if(mHandler!=null) mHandler.post(()->initLazyView());
            if(mHandler!=null) mHandler.post(()->initLazyData());
        }
    }

    protected void onBindView(View vContent){ }
    private boolean bIsInitLayout = false;
    private void initLayout(){
        if(bIsInitLayout) return;
        bIsInitLayout = true;
        context = this;
        mHandler = new Handler();
//        if(Build.VERSION.SDK_INT>=26){
////            Log.d("initLayout","开启硬件加速~");
//            getWindow().setFlags(
//                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
//                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
//        }
        if(getContentViewResId()!= View.NO_ID){
            try { setContentView(View.NO_ID); } catch (Exception e) { }
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        initLayout();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        FrameLayout contentView = getWindow().getDecorView().findViewById(android.R.id.content);
//        getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
//            @Override
//            public void onGlobalLayout() {
//                InterceptedOnClickListener.setInterceptedOnClickListenerIfNeededFromViewGroup(contentView);
//            }
//        });
        initLayout();
    }

    boolean isResumed = false;
    boolean bIsInitLazy = false;
    @Override
    protected void onResume() {
        super.onResume();
        isResumed = true;
        onInitLazy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        isResumed = false;
        hideKeyboard();
    }

    @Override
    protected void onDestroy() {
        if(mHandler!=null){
            mHandler.removeCallbacksAndMessages(null);
        }
        super.onDestroy();
    }
    //endregion

    //region 智能刷新
    protected SmartRefreshLayout srlMain;
    protected boolean hasRefreshBar(){
        return false;
    }

    @Override
    public void onRefresh(@NonNull RefreshLayout refreshLayout) {

    }

    @Override
    public void onLoadMore(@NonNull RefreshLayout refreshLayout) {

    }
    //endregion

    //region 标题栏
    private BaseActivity setText(TextView tv, String text){
        if(tv!=null&&text!=null) tv.setText(text);
        return this;
    }
    public BaseActivity setStatusBarColor(Object color) {
        if(color instanceof Integer){
            vStatusBar.setBackgroundColor(getResources().getColor((int) color));
            return this;
        }
        if(color instanceof String){
            vStatusBar.setBackgroundColor(Color.parseColor((String) color));
            return this;
        }
        throw new RuntimeException(getClass().getSimpleName()+"->未定义的背景类型："+color);
    }
    public BaseActivity setTitleBarColor(Object color) {
        if(color instanceof Integer){
            clTitleBar.setBackgroundColor(getResources().getColor((int) color));
            return this;
        }
        if(color instanceof String){
            clTitleBar.setBackgroundColor(Color.parseColor((String) color));
            return this;
        }
        throw new RuntimeException(getClass().getSimpleName()+"->未定义的背景类型："+color);
    }

    public BaseActivity setLeftText(String text){
        return setText(tvTitleLeft,text);
    }

    public BaseActivity setCenterText(String text){
        return setText(tvTitleCenter,text);
    }

    public BaseActivity setRightText(String text){
        return setText(tvTitleRight,text);
    }

    private BaseActivity setTextColor(TextView tv, Object color){
        if(tv!=null){
            if(color instanceof Integer){//R.Color.xxx
                tv.setTextColor(getResources().getColor((int) color));
                return this;
            }
            if(color instanceof String){//#????????
                tv.setTextColor(Color.parseColor((String) color));
                return this;
            }
        }
        throw new RuntimeException(getClass().getSimpleName()+"->未定义的背景类型："+color);
    }

    public BaseActivity setLeftTextColor(Object color){
        return setTextColor(tvTitleLeft,color);
    }

    public BaseActivity setCenterTextColor(Object color){
        return setTextColor(tvTitleCenter,color);
    }

    public BaseActivity setRightTextColor(Object color){
        return setTextColor(tvTitleRight,color);
    }


    private BaseActivity setTextSize(TextView tv, float fSize_sp){
        if(tv!=null) tv.setTextSize(TypedValue.COMPLEX_UNIT_SP,fSize_sp);
        return this;
    }

    public BaseActivity setLeftTextSize(float fSize_sp){
        return setTextSize(tvTitleLeft,fSize_sp);
    }

    public BaseActivity setCenterTextSize(float fSize_sp){
        return setTextSize(tvTitleCenter,fSize_sp);
    }

    public BaseActivity setRightTextSize(float fSize_sp){
        return setTextSize(tvTitleRight,fSize_sp);
    }

    private BaseActivity setImageDrawable(ImageView img, @DrawableRes int resId){
        if(img!=null) img.setVisibility(View.VISIBLE);
        if(img!=null) img.setImageResource(resId);
        return this;
    }

    public BaseActivity setLeftImageDrawable(@DrawableRes int resId){
        return setImageDrawable(imgTitleLeft,resId);
    }

    public BaseActivity setLeftImageVisibility(int visibility){
        imgTitleLeft.setVisibility(visibility);
        return this;
    }

    public BaseActivity setCenterImageDrawable(@DrawableRes int resId){
        return setImageDrawable(imgTitleCenter,resId);
    }

    public BaseActivity setCenterImageVisibility(int visibility){
        imgTitleCenter.setVisibility(visibility);
        return this;
    }

    public BaseActivity setRightImageDrawable(@DrawableRes int resId){
        return setImageDrawable(imgTitleRight,resId);
    }

    public BaseActivity setRightImageVisibility(int visibility){
        imgTitleRight.setVisibility(visibility);
        return this;
    }
    //endregion

    //region ViewPager
    public static<T extends View> ViewPagerAdapter setupViewPager(ViewPager pager, List<T> views, ViewPager.OnPageChangeListener lsr){
        ViewPagerAdapter adapter;
        pager.setAdapter(adapter = new ViewPagerAdapter<T>(views));
        pager.setOffscreenPageLimit(views.size());
        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if(lsr!=null) lsr.onPageScrolled(position,positionOffset,positionOffsetPixels);
            }
            @Override
            public void onPageSelected(int position) {
                pager.setCurrentItem(position,true);
                if(lsr!=null) lsr.onPageSelected(position);
            }
            @Override
            public void onPageScrollStateChanged(int state) {
                if(lsr!=null) lsr.onPageScrollStateChanged(state);
            }
        });
        pager.setCurrentItem(0);
        return adapter;
    }
    private static class ViewPagerAdapter<T extends View> extends PagerAdapter {
        List<T> lstDatas;
        public ViewPagerAdapter(List<T> lstDatas){
            this.lstDatas = lstDatas;
        }
        @Override
        public int getCount() {
            return lstDatas.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {//移除一个给定位置的页面
            try {container.removeView(lstDatas.get(position)); } catch (Exception e) { }
        }
        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            container.addView(lstDatas.get(position));//给定位置添加显示一个view
            return lstDatas.get(position);//返回View本身
        }
    }
    //endregion

    //region 小工具
    private int _requestCodeGennerator = 0x7fff;
    final private int getNextRequestCode(){
        return (++_requestCodeGennerator % 0x7fff)+0x7fff;
    }

    final Map<Integer, OnActivityResultItemCallBack> mapResultCallbacks = new HashMap<>();
    final Set<OnActivityResultCallBack> setOnActivityResultCallBacks = new HashSet<>();
    public interface OnActivityResultCallBack {
        boolean OnActivityResult(int requestCode, int resultCode, Intent data);
    }

    public interface OnActivityResultItemCallBack {
        void OnActivityRequestResult(int resultCode, Intent data);
    }

    public void registerOnActivityResultCallBack(OnActivityResultCallBack callBack){
        if(callBack!=null){
            setOnActivityResultCallBacks.add(callBack);
        }
    }

    public void unregisterOnActivityResultCallBack(OnActivityResultCallBack callBack){
        if(callBack!=null){
            setOnActivityResultCallBacks.remove(callBack);
        }
    }

    public void startActivityForResult(Class clz , OnActivityResultItemCallBack callBack){
        int requestCode = getNextRequestCode();
        mapResultCallbacks.put(requestCode,callBack);
        startActivityForResult(new Intent(this,clz),requestCode);
    }

    public void startActivityForResult(Class clz, int requestCode, OnActivityResultItemCallBack callBack){
        mapResultCallbacks.put(requestCode,callBack);
        startActivityForResult(new Intent(this,clz),requestCode);
    }

    public void startActivityForResult(Class clz, Bundle data, OnActivityResultItemCallBack callBack){
        int requestCode = getNextRequestCode();
        mapResultCallbacks.put(requestCode,callBack);
        startActivityForResult(new Intent(this,clz){{putExtras(data);}},requestCode);
    }

    public void startActivityForResult(Class clz, Bundle data, int requestCode, OnActivityResultItemCallBack callBack){
        mapResultCallbacks.put(requestCode,callBack);
        startActivityForResult(new Intent(this,clz){{putExtras(data);}},requestCode);
    }

    public void startActivityForResult(Class clz, Bundle data, int flag, int requestCode, OnActivityResultItemCallBack callBack){
        mapResultCallbacks.put(requestCode,callBack);
        startActivityForResult(new Intent(this,clz){{putExtras(data); setFlags(flag);}},requestCode);
    }

    public void startActivityForResult(Intent intent, int requestCode, OnActivityResultItemCallBack callBack){
        mapResultCallbacks.put(requestCode,callBack);
        startActivityForResult(intent,requestCode);
    }

    public void startActivityForResult(Class clz, int flag, int requestCode){
        startActivityForResult(new Intent(this,clz){{setFlags(flag);}},requestCode);
    }

    public void startActivityForResult(Class clz, int requestCode){
        startActivityForResult(new Intent(this,clz),requestCode);
    }

    public void startActivityForResult(Class clz, Bundle data, int requestCode){
        startActivityForResult(new Intent(this,clz){{putExtras(data);}},requestCode);
    }

    public void startActivity(Class clz){
        startActivity(new Intent(this,clz));
    }

    public void startActivity(Class clz, Bundle data){
        startActivity(new Intent(this,clz){{putExtras(data);}});
    }

    public void startActivity(Class clz, int flag){
        startActivity(new Intent(this,clz){{setFlags(flag);}});
    }

    public void startActivity(Class clz, Bundle data, int flag){
        startActivity(new Intent(this,clz){{putExtras(data);setFlags(flag);}});
    }

//    private static long lStartTime = System.currentTimeMillis()-500L;
//    @Override
//    public void startActivity(Intent intent){
//        long now = System.currentTimeMillis();
//        if(now - lStartTime>500L){
//            lStartTime = now;
//            super.startActivity(intent);
//        }
//        else{
//            Log.e("startActivity",intent.toString());
//        }
//    }

    protected void toastCenter(String msg){
        ToastDialog.showCenter(this,msg);
    }

    protected void toastTop(String msg){
        ToastDialog.showBottom(this,msg);
    }

    public void Loge(String msg){
        Log.e(TAG,msg);
    }

    public final int px2dip(float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    public final int dp2px(final float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public int sp2px(float spValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }
    //endregion


    //region 动态权限
    public boolean hasPermision(String permissionToCheck){
        return PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this,permissionToCheck);
    }

    private EasyPermission.PermissionResultListener permissionResultListener = null;
    private String requestedPermission = null;

    public void requestPermission(String permission, EasyPermission.PermissionResultListener listener){
        permissionResultListener = listener;
        requestedPermission = permission;
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, 1<<15);
        } else if(!hasPermision(permission)){
            ActivityCompat.requestPermissions(this, new String[]{permission}, 1<<15);
        }
        else{
            if(Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permission) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
                if (Environment.isExternalStorageManager()) {
                    permissionResultListener.onPermissionGranted();
                    permissionResultListener = null;
                    requestedPermission = null;
                }
                else{
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + this.getPackageName()));
                    startActivityForResult(intent, 1<<15);
                }
                return;
            }
            permissionResultListener.onPermissionGranted();
            permissionResultListener = null;
            requestedPermission = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        for(OnActivityResultCallBack callBack:setOnActivityResultCallBacks){
            if(callBack.OnActivityResult(requestCode,resultCode,data)){
                return;//是否拦截此次事件
            }
        }
        if(requestCode==1<<15 && permissionResultListener!=null){
            if(hasPermision(requestedPermission)){
                if(Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(requestedPermission) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (Environment.isExternalStorageManager()) {
                        permissionResultListener.onPermissionGranted();
                    }
                    else{
                        permissionResultListener.onPermissionDenied();
                    }
                }
                else{
                    permissionResultListener.onPermissionGranted();
                }
            }
            else
                permissionResultListener.onPermissionDenied();
            permissionResultListener = null;
            requestedPermission = null;
            return;
        }
        OnActivityResultItemCallBack callBack = mapResultCallbacks.get(requestCode);
        if(callBack!=null){
            callBack.OnActivityRequestResult(resultCode,data);
            mapResultCallbacks.remove(requestCode);
            return;//中断子类的onActivityResult执行
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if (requestCode==1<<15&&permissionResultListener!=null){
            if(grantResults.length>0 && grantResults[0]== PackageManager.PERMISSION_GRANTED){
                if(Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(requestedPermission) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (Environment.isExternalStorageManager()) {
                        permissionResultListener.onPermissionGranted();
                    }
                    else{
                        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                        intent.setData(Uri.parse("package:" + this.getPackageName()));
                        startActivityForResult(intent, 1<<15);
                    }
                }
                else
                    permissionResultListener.onPermissionGranted();
            }
            else
                permissionResultListener.onPermissionDenied();
            permissionResultListener = null;
            requestedPermission = null;
            return;
        }
    }
    //endregion

    //region 软键盘
    public void hideKeyboard() { //隐藏软键盘
        try{
            if(getCurrentFocus()==null) return;
            if(getCurrentFocus().getWindowToken()==null) return;
            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),  0);
        }
        catch (Exception e){
            if(BuildConfig.DEBUG) Log.e("HideInput",e.getMessage());
        }
    }

    public void shownKeyboardDalayed(View v, long mills){
        App.PostDelayed(()->showKeyboard(v),mills);
    }

    protected InputMethodManager inputManager = null;
    public void showKeyboard(View v) {//获取焦点并且显示输入法

        try {v.clearFocus();} catch (Exception e) {}
        try {v.setFocusable(true);} catch (Exception e) {}
        try {v.setFocusableInTouchMode(true);} catch (Exception e) {}
        try {v.requestFocus();} catch (Exception e) {}
        if(inputManager==null) inputManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        if(v.hasFocus())inputManager.toggleSoftInput(0, 0);
        if(v.hasFocus())inputManager.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
    }

    /**监听软键盘动态调整布局高度*/
    public interface OnKeyBoardLayoutStateChangeListener {
        void onKeyBoardShow(int keyBoardHeight, int gapHeight);
        void onKeyBoardHide();
    }
    OnKeyBoardLayoutStateChangeListener onKeyBoardLayoutStateChangeListener = null;
    ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener = null;
    Rect rect = new Rect();
    boolean bInited = false;
    boolean bLastVisibelState = false;
    int gapHeight = 0;
    protected void setOnKeyBoardLayoutStateChangeListener(final OnKeyBoardLayoutStateChangeListener keyBoardLayoutStateChangeListener){
        removeOnKeyBoardLayoutStateChangeListener();
        onKeyBoardLayoutStateChangeListener = keyBoardLayoutStateChangeListener;
        if(keyBoardLayoutStateChangeListener==null) return;
        final View decorView = getWindow().getDecorView();
        decorView.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                BaseActivity.this.onGlobalLayout(onGlobalLayoutListener,onKeyBoardLayoutStateChangeListener);
            }
        });
    }
    protected void onGlobalLayout(ViewTreeObserver.OnGlobalLayoutListener listener, OnKeyBoardLayoutStateChangeListener keyBoardLayoutStateChangeListener){
        getWindow().getDecorView().getViewTreeObserver().removeOnGlobalLayoutListener(listener);
        getWindow().getDecorView().postDelayed(()->{//可能不会实时获取到，故作延时，否则可能导致判断出错
            Log.i("KeyBoard","onJudge");
            //获取当前根视图在屏幕上显示的大小
            getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
            //计算出可见屏幕的高度
            int displayHight = rect.bottom - rect.top;
            //获得屏幕整体的高度
            int hight = getWindow().getDecorView().getHeight();
            //获得键盘高度
            int keyboardHeight = hight - displayHight;
            final boolean visible = (double) displayHight / hight < 0.8d;
            if(!visible) gapHeight = hight - displayHight;
            if(!bInited||visible!=bLastVisibelState){
                getWindow().getDecorView().post(()->{
                    if(visible){
                        try { keyBoardLayoutStateChangeListener.onKeyBoardShow(keyboardHeight,gapHeight); } catch (Exception e) { }
                    }
                    else{
                        try { keyBoardLayoutStateChangeListener.onKeyBoardHide(); } catch (Exception e) { }
                    }
                });
                bInited = true;
                bLastVisibelState = visible;
            }
            getWindow().getDecorView().postDelayed(()->{
                        if(getWindow().getDecorView()!=null) //可能页面被关闭
                            getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(listener);
                    },
                    80L);
        },100L);
    }
    protected void removeOnKeyBoardLayoutStateChangeListener(){
        if(onKeyBoardLayoutStateChangeListener!=null&&onGlobalLayoutListener!=null)
            getWindow().getDecorView().getViewTreeObserver().removeOnGlobalLayoutListener(onGlobalLayoutListener);
    }
    //endregion

    //region 状态栏
    /** 获取主题色 */
    protected int getColorPrimary() {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(R.attr.colorPrimary, typedValue, true);
        return typedValue.data;
    }

    /** 子类可以重写改变状态栏背景颜色 */
    protected @ColorInt int setBackgroundStatusBarColor() {
        return getColorPrimary();
    }
    /**
     * 子类可以重写改变状态栏前景颜色
     * return 是否是黑色前景字体图标颜色
     */
    protected boolean getForegroundStatusBarColor(){
        return true;
    }
    /** 子类可以重写决定是否使用透明状态栏 */
    protected boolean translucentStatusBar() { return true; }

    /** 设置状态栏颜色 */
    protected void initSystemBarTint() {
        Window window = getWindow();

        if (translucentStatusBar()) {
            // 设置状态栏全透明
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.setStatusBarColor(Color.TRANSPARENT);
                window.setNavigationBarColor(Color.parseColor(getForegroundStatusBarColor()?"#000000":"#ffffff"));
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                SystemBarTintManager tintManager = new SystemBarTintManager(this);
                tintManager.setNavigationBarTintColor(Color.parseColor(getForegroundStatusBarColor()?"#000000":"#ffffff"));
            }
            return;
        }
        // 沉浸式状态栏
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //5.0以上使用原生方法
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(setBackgroundStatusBarColor());
            window.setNavigationBarColor(Color.parseColor(getForegroundStatusBarColor()?"#000000":"#ffffff"));
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            //4.4-5.0使用三方工具类，有些4.4的手机有问题，这里为演示方便，不使用沉浸式
//            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            tintManager.setStatusBarTintEnabled(true);
            tintManager.setNavigationBarTintColor(Color.parseColor(getForegroundStatusBarColor()?"#000000":"#ffffff"));
            tintManager.setStatusBarTintColor(setBackgroundStatusBarColor());
        }
        else{
            if (getForegroundStatusBarColor()) {
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            } else {
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            }
        }
    }
    //endregion

    //region old API
    protected static final int GOTO_SETTINGS_REQUEST_CODE = 1002;
    public static final String EXTRA_ENABLE_RIGHT_IN_ANIM = "enable_right_in_anim";
    public static final String EXTRA_ENABLE_RIGHT_OUT_ANIM = "enable_right_out_anim";

    protected void onNetworkConnected(){}

    protected void onNetworkDisconnected(){}

    protected void checkNetwork() {
        if (InternetUtils.isNetworkConnected(getApplicationContext()))
            onNetworkConnected();
        else
            onNetworkDisconnected();
    }

    protected void changeNotificationUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            getWindow().setStatusBarColor(getResources().getColor(R.color.theme_window_bg_color));
    }

    private static Fragment getVisibleFragment(FragmentManager mgr){
        FragmentManager fragmentManager = mgr;
        List<Fragment> fragments = fragmentManager.getFragments();
        for(androidx.fragment.app.Fragment fragment : fragments){
            if(fragment != null && fragment.isVisible())
                return fragment;
        }
        return null;
    }

    private Fragment getVisibleFragment(){
        return getVisibleFragment(getSupportFragmentManager());
    }
    public static void showFragment(FragmentManager mgr, @NonNull Fragment fragment, @IdRes int rootId){
        Fragment from = getVisibleFragment(mgr);
        String tag = fragment.getClass().getSimpleName();
        if(from==null){
            FragmentTransaction transaction = mgr.beginTransaction();
            transaction.replace(rootId, fragment, tag);
            transaction.commitAllowingStateLoss();
            return;
        }
        if(fragment==from) return;
        FragmentTransaction transaction = mgr.beginTransaction();
        if (!fragment.isAdded())
            transaction.hide(from).add(rootId, fragment, tag).commitAllowingStateLoss();
        else
            transaction.hide(from).show(fragment).commitAllowingStateLoss();
    }
    protected void showFragment(@NonNull Fragment fragment, @IdRes int rootId){
        showFragment(getSupportFragmentManager(),fragment,rootId);
    }

    protected void setStatusBarColor(@ColorInt int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            getWindow().setStatusBarColor(color);
    }

    protected void addFragment(@NonNull FragmentManager fragmentManager,
                               @NonNull Fragment fragment, int frameId) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(frameId, fragment);

        transaction.commitAllowingStateLoss();
    }

    protected void removeFragment(@NonNull FragmentManager fragmentManager,
                               @NonNull Fragment fragment) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.remove( fragment);
        transaction.commitAllowingStateLoss();
    }


    protected void addFragment(@NonNull FragmentManager fragmentManager,
                               @NonNull Fragment fragment, int frameId, String tag) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(frameId, fragment, tag);
        transaction.commitAllowingStateLoss();
    }


    @Override
    public Resources getResources() {
        Resources res = super.getResources();
        Configuration config = new Configuration();
        config.setToDefaults();
        res.updateConfiguration(config, res.getDisplayMetrics());
        return res;
    }
    //endregion
}
