package com.dreaming.hscj.base;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.dreaming.hscj.R;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public abstract class BaseFragment extends Fragment {

    @Deprecated //zlt Version
    protected int getLayoutId(){return View.NO_ID;}

    //sxs Version
    protected int getContentViewResId(){return View.NO_ID;}

    protected boolean isLazyLoadMode(){ return false; }

    protected long getNoneLazyModeLoadTime(){
        return 0L;
    }

    public void onFragmentResume(){}

    boolean hasTitleBar(){return false;}

    //region 生命周期
    public interface IFragmentLifeCallBack{
        void onCreate(BaseFragment fragment);
        void onStart(BaseFragment fragment);
        void onResume(BaseFragment fragment);
        void onPause(BaseFragment fragment);
        void onStop(BaseFragment fragment);
        void onDestory(BaseFragment fragment);
    }
    final List<IFragmentLifeCallBack> lstCallBacks = new ArrayList<>();

    protected View vContentView;
    protected ConstraintLayout vParent;
    protected FrameLayout flRoot;
    private String mTag;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if(mHandler==null) mHandler = new Handler();
        super.onViewCreated(view, savedInstanceState);
        if(getContentViewResId()!= View.NO_ID){
//            Log.e("InterceptedOnClick",this.toString());
//            mHandler.postDelayed(()->InterceptedOnClickListener.setInterceptedOnClickListenerIfNeededFromViewGroup(flRoot),1000L);
            if(flRoot==null){
                flRoot = (FrameLayout) view;
            }
//            view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
//                @Override
//                public void onGlobalLayout() {
//                    InterceptedOnClickListener.setInterceptedOnClickListenerIfNeededFromViewGroup(view);
//                }
//            });
            if(!isLazyLoadMode()){
                mHandler.postDelayed(()->notifyInitFragment(), getNoneLazyModeLoadTime());
            }
        }
        else{
//            Log.e("InterceptedOnClick",this.toString());
//            mHandler.postDelayed(()->InterceptedOnClickListener.setInterceptedOnClickListenerIfNeededFromViewGroup(flRoot),1000L);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if(mHandler==null) mHandler = new Handler();
        if(getContentViewResId()!= View.NO_ID){//sxs Version
            _TAG = this.getClass().getSimpleName();
            flRoot = (FrameLayout) inflater.inflate(R.layout.fragment_init,null);
            return flRoot;
        }
        return inflater.inflate(getLayoutId(), container, false);
    }

    public BaseFragment addLifeListener(IFragmentLifeCallBack callBack){
        if(callBack==null) return this;
        if(lstCallBacks.contains(callBack)) return this;
        lstCallBacks.add(callBack);
        return this;
    }

    public void removeLifeListener(IFragmentLifeCallBack callBack){
        if(callBack==null) return;
        lstCallBacks.remove(callBack);
    }

    public void notifyInitFragment(){
        if(flRoot==null||flRoot.findViewById(R.id.fl_progress_load)==null){return;}
//        InterceptedOnClickListener.setInterceptedOnClickListenerIfNeededFromViewGroup(flRoot);
        LayoutInflater inflater = LayoutInflater.from(getContext());
        vContentView = inflater.inflate(getContentViewResId(),null);
        vParent = null;
        if(SCREEN_HEIGHT==0){
            WindowManager manager = getActivity().getWindowManager();
            DisplayMetrics outMetrics = new DisplayMetrics();
            manager.getDefaultDisplay().getMetrics(outMetrics);
            SCREEN_WIDTH = outMetrics.widthPixels;
            SCREEN_HEIGHT = outMetrics.heightPixels;
        }
        if(hasTitleBar()){
            vParent = (ConstraintLayout) inflater.inflate(R.layout.base_title,null);
            FrameLayout flRoot = vParent.findViewById(R.id.fl_content);
            flRoot.addView(vContentView,new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        }
        onInitView();
        if(getActivity() instanceof BaseActivity){
            BaseActivity baseActivity = (BaseActivity) getActivity();
            baseActivity.registerOnActivityResultCallBack(onActivityResultCallBack);
        }
    }

    public void setContentView(View vContentView){
        if(flRoot==null||flRoot.findViewById(R.id.fl_progress_load)==null){return;}
        if(vContentView.getParent()!=null) return;
        flRoot.removeView(flRoot.findViewById(R.id.fl_progress_load));
        flRoot.addView(vContentView,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        if(activity instanceof BaseActivity){
            ((BaseActivity) activity).registerOnActivityResultCallBack(onActivityResultCallBack);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(mHandler==null)mHandler = new Handler();
        _TAG = this.getClass().getSimpleName();
        Log.e(_TAG,"onCreate");
        for(IFragmentLifeCallBack callBack: lstCallBacks) callBack.onCreate(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.e(_TAG,"onStart");
        for(IFragmentLifeCallBack callBack: lstCallBacks) callBack.onStart(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.e(_TAG,"onResume");
        for(IFragmentLifeCallBack callBack: lstCallBacks) callBack.onResume(this);
        initLazyView();
        initLazyData();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.e(_TAG,"onPause");
        for(IFragmentLifeCallBack callBack: lstCallBacks) callBack.onPause(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.e(_TAG,"onStop");
        for(IFragmentLifeCallBack callBack: lstCallBacks) callBack.onStop(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releaseInstance(this.getClass());
        for(IFragmentLifeCallBack callBack: lstCallBacks) callBack.onDestory(this);
        if(mHandler!=null) mHandler.removeCallbacksAndMessages(null);
    }
    //endregion

    //region 子类抽象函数
    protected String _TAG;
    protected Context context;
    public static int SCREEN_HEIGHT=0,SCREEN_WIDTH=0;
    protected Handler mHandler = null;

    public void initView(){}
    public void initData(){}
    public void initLazyView(){}
    public void initLazyData(){}
    public void onBindView(View vContent){}

    boolean bIsinitedView = false;
    public void onInitView(){
        if(bIsinitedView) return;
        bIsinitedView = true;
        if(mHandler!=null) mHandler.post(()->onBindView(hasTitleBar()?vParent:vContentView));
        if(mHandler!=null) mHandler.post(()->initView());
        if(mHandler!=null) mHandler.post(()->initData());
        if(mHandler!=null) mHandler.post(()->initLazyView());
        if(mHandler!=null) mHandler.post(()->initLazyData());
        if(mHandler!=null) mHandler.post(()->setContentView(hasTitleBar()?vParent:vContentView));
    }
    //endregion

    //region 功能函数
    final Map<Integer, BaseActivity.OnActivityResultItemCallBack> mapResultCallbacks = new HashMap<>();
    final BaseActivity.OnActivityResultCallBack onActivityResultCallBack = new BaseActivity.OnActivityResultCallBack() {
        @Override
        public boolean OnActivityResult(int requestCode, int resultCode, Intent data) {
            BaseActivity.OnActivityResultItemCallBack callBack = mapResultCallbacks.get(requestCode);
            if(callBack!=null){
                callBack.OnActivityRequestResult(resultCode,data);
                mapResultCallbacks.remove(requestCode);
                return true;//中断子类的onActivityResult执行
            }
            return false;
        }
    };

    private int _requestCodeGennerator = 0x8fff;
    final private int getNextRequestCode(){
        return ++_requestCodeGennerator % 0xffff;
    }

    protected void startActivityForResult(Class clz, int requestCode, BaseActivity.OnActivityResultItemCallBack callBack){
        if(mapResultCallbacks.get(requestCode)!=null){
            Log.e(_TAG, String.format("startActivityForResult->(%s,%d) requestCode '%d' was Uesd",clz.getSimpleName(),requestCode,requestCode));
        }
        mapResultCallbacks.put(requestCode,callBack);
        getActivity().startActivityForResult(new Intent(getActivity(),clz),requestCode);
    }

    protected void startActivityForResult(Class clz, BaseActivity.OnActivityResultItemCallBack callBack){
        int requestCode = getNextRequestCode();
        if(mapResultCallbacks.get(requestCode)!=null){
            Log.e(_TAG, String.format("startActivityForResult->(%s,%d) requestCode '%d' was Uesd",clz.getSimpleName(),requestCode,requestCode));
        }
        mapResultCallbacks.put(requestCode,callBack);
        getActivity().startActivityForResult(new Intent(getActivity(),clz),requestCode);
    }

    protected void startActivityForResult(Class clz, Bundle data, int requestCode, BaseActivity.OnActivityResultItemCallBack callBack){
        mapResultCallbacks.put(requestCode,callBack);
        getActivity().startActivityForResult(new Intent(getActivity(),clz){{putExtras(data);}},requestCode);
    }

    protected void startActivityForResult(Class clz, Bundle data, BaseActivity.OnActivityResultItemCallBack callBack){
        int requestCode = getNextRequestCode();
        mapResultCallbacks.put(requestCode,callBack);
        getActivity().startActivityForResult(new Intent(getActivity(),clz){{putExtras(data);}},requestCode);
    }

    protected void startActivityForResult(Class clz, int requestCode){
        getActivity().startActivityForResult(new Intent(getActivity(),clz),requestCode);
    }

    protected void startActivityForResult(Class clz, Bundle data, int requestCode){
        getActivity().startActivityForResult(new Intent(getActivity(),clz){{putExtras(data);}},requestCode);
    }

    protected void startActivity(Class clz){
        startActivity(new Intent(getActivity(),clz));
    }

    protected void startActivity(Class clz, Bundle data){
        getActivity().startActivity(new Intent(getActivity(),clz){{putExtras(data);}});
    }

    public void startActivity(Class clz, int flag){
        getActivity().startActivity(new Intent(getActivity(),clz){{setFlags(flag);}});
    }

    public void startActivity(Class clz, Bundle data, int flag){
        getActivity().startActivity(new Intent(getActivity(),clz){{putExtras(data);setFlags(flag);}});
    }
    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     */
    public final int px2dip(float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    /**
     * dp 转 px
     *
     * @param dpValue dp 值
     * @return px 值
     */
    public final int dp2px(final float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
    //endregion

    //region 单例模式
    private static volatile Map<Class,BaseFragment> intances = new HashMap<>();
    private static <T extends BaseFragment> T get(Class target){
        synchronized (target){
            return (T) intances.get(target);
        }
    }

    public static <T extends BaseFragment> T getInstance(Class<T> target){
        T instance = get(target);
        if(instance == null){
            synchronized (target){
                instance = get(target);
                if(instance == null){
                    try {
                        Constructor<T> instanceConstructor = target.getConstructor();
                        instance =  instanceConstructor.newInstance();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    intances.put(target,instance);
                }
            }
        }
        return instance;
    }

    public static void releaseInstance(Class target){
        synchronized (target){
            intances.remove(target);
        }
    }
    //endregion
}