package com.dreaming.hscj.activity.menu;

import android.util.Pair;
import android.view.View;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dreaming.hscj.Constants;
import com.dreaming.hscj.R;
import com.dreaming.hscj.activity.login.LoginActivity;
import com.dreaming.hscj.activity.nucleic_acid.searching.LocalSamplingDialog;
import com.dreaming.hscj.base.BaseActivity;
import com.dreaming.hscj.base.BaseFragment;
import com.dreaming.hscj.core.EasyAdapter.EasyAdapter;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.template.database.wrapper.DatabaseConfig;
import com.dreaming.hscj.utils.ToastUtils;

import java.util.List;

public abstract class BaseMenuFragment extends BaseFragment {

    @Override
    protected int getContentViewResId() {
        return R.layout.fragment_menu;
    }

    RecyclerView rvMenu;
    @Override
    public void onBindView(View vContent) {
        rvMenu = vContent.findViewById(R.id.rv_menu);
    }

    abstract List<Pair<Integer,String>> getAdapterConfig();
    abstract List<Class> getJumpingClassConfig();
    abstract List<Boolean> getLoginConfig();
    abstract List<Boolean> getDbCreateConfig();

    @Override
    public void initView() {
        rvMenu.setLayoutManager(new GridLayoutManager(getContext(),2){@Override public boolean canScrollVertically() { return false; }});
        rvMenu.setAdapter(new EasyAdapter(getContext(), R.layout.recy_menu, getAdapterConfig(), (EasyAdapter.IEasyAdapter<Pair<Integer, String>>) (holder, data, position) -> {
            holder.setText         (R.id.tv_text ,data.second);
            holder.setImageResource(R.id.img_icon,data.first );
            holder.setOnClickListener(R.id.fl_container,v -> onClicked(v,position));
        }));
    }

    private boolean shouldLogin(int position){
        List<Boolean> lstLoginConfig = getLoginConfig();
        if(lstLoginConfig == null || position<0 || position>= lstLoginConfig.size()) return false;
        return lstLoginConfig.get(position);
    }
    private boolean shouldCreateDB(int position){
        List<Boolean> lstDbCreateConfig = getDbCreateConfig();
        if(lstDbCreateConfig == null || position<0 || position>= lstDbCreateConfig.size()) return false;
        return lstDbCreateConfig.get(position);
    }
    private Class getJumpingClass(int position){
        List<Class> lstJumpingClass = getJumpingClassConfig();
        if(lstJumpingClass == null || position<0 || position>= lstJumpingClass.size()) return null;
        return lstJumpingClass.get(position);
    }

    protected void onClicked(View v, int position){
        if(Template.getCurrentTemplate() == null){
            ToastUtils.show("初始化失败！");
            return;
        }
        if(shouldLogin(position)){
            if(!Constants.User.isLogin()){
                ToastUtils.show("该功能需要登录！");
                LoginActivity.doLogin((BaseActivity) getActivity(), new LoginActivity.ILoginListener() {
                    @Override
                    public void onSuccess() {
                        ToastUtils.show(getContext(),"登录成功！");
                        Template.getCurrentTemplate().getUserOverallDatabase().sync();
                        onClicked(v,position);
                    }

                    @Override
                    public void onFailure() {
                        ToastUtils.show(getContext(),"登录失败！");
                    }
                });
                return;
            }
        }

        if(shouldCreateDB(position)){
            if(Constants.DBConfig.getSelectedDatabase().length()==0){
                DatabaseConfig.showCreateDatabaseDialog((BaseActivity) getActivity(), new DatabaseConfig.IDBCreateListener() {
                    @Override
                    public void onSuccess(String townName, String villageName) {
                        Constants.DBConfig.setSelectedDatabase(1);
                        ToastUtils.show("数据库创建成功！");
                        onClicked(v,position);
                    }

                    @Override
                    public void onFailure() {
                        ToastUtils.show("数据库创建失败！");
                    }
                });
                return;
            }
        }

        Class jumping = getJumpingClass(position);
        if(jumping!=null){
            startActivity(jumping);
            return;
        }

        LocalSamplingDialog.showLocalSamplingDialog((BaseActivity) getActivity());
    }
}
