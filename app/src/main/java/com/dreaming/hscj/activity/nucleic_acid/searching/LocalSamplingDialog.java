package com.dreaming.hscj.activity.nucleic_acid.searching;

import android.graphics.Point;
import android.view.Gravity;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dreaming.hscj.App;
import com.dreaming.hscj.Constants;
import com.dreaming.hscj.R;
import com.dreaming.hscj.activity.community.manage.CommunityMemberDetailActivity;
import com.dreaming.hscj.activity.nucleic_acid.sampling.NASamplingActivity;
import com.dreaming.hscj.base.BaseActivity;
import com.dreaming.hscj.core.EasyAdapter.EasyAdapter;
import com.dreaming.hscj.core.TTSEngine;
import com.dreaming.hscj.core.ThreadPoolProvider;
import com.dreaming.hscj.core.ViewInjector;
import com.dreaming.hscj.dialog.loading.LoadingDialog;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.template.database.impl.DaySamplingLogDatabase;
import com.dreaming.hscj.template.database.impl.UserOverallDatabase;
import com.dreaming.hscj.utils.DensityUtils;
import com.dreaming.hscj.utils.JsonUtils;
import com.dreaming.hscj.widget.ShownView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import priv.songxusheng.easydialog.EasyDialog;
import priv.songxusheng.easydialog.EasyDialogHolder;
import priv.songxusheng.easyjson.ESONObject;

public class LocalSamplingDialog {
    private static void showLocalSamplingDetailDialog(BaseActivity activity, boolean bIsShownInputed){
        Point p = DensityUtils.getScreenSize();
        new EasyDialog(R.layout.dialog_local_sampling_detail,activity)
                .setOnBindDialogListener(new EasyDialog.OnBindDialogListener() {
                    @Override
                    public void onBindDialog(EasyDialogHolder dialogHolder) {
                        if(bIsShownInputed){
                            dialogHolder.setText(R.id.tv_title,"查看本地核酸已检测记录");
                        }
                        else{
                            dialogHolder.setText(R.id.tv_title,"查看本地核酸未检测记录");
                        }

                        dialogHolder.setOnClickListener(R.id.iv_close,v -> dialogHolder.dismissDialog());

                        DaySamplingLogDatabase db5 = Template.getCurrentTemplate().getDaySamplingLogDatabase();
                        UserOverallDatabase db2 = Template.getCurrentTemplate().getUserOverallDatabase();

                        RecyclerView rv = dialogHolder.getView(R.id.rv_preview);
                        List<ESONObject> lstData = new ArrayList<>();

                        rv.setHasFixedSize(true);
                        rv.setLayoutManager(new LinearLayoutManager(activity));
                        EasyAdapter adapter = new EasyAdapter(activity, R.layout.recy_local_sampling_detail, lstData, (EasyAdapter.IEasyAdapter<ESONObject>) (holder1, data, position) -> {
                            String idCardNo = data.getJSONValue(db2.getIdFieldName(),"");
                            String name     = data.getJSONValue(db2.getNameFiledName(),"");
                            holder1.setText(R.id.tv_index,String.valueOf(position+1));
                            holder1.setText(R.id.tv_name,name);
                            holder1.setText(R.id.tv_id_card_no,idCardNo);
                            holder1.setOnClickListener(R.id.tv_name,v -> TTSEngine.speakChinese(name));
                            holder1.setOnClickListener(R.id.tv_detail,v -> CommunityMemberDetailActivity.shownMember(activity,idCardNo));
                        });
                        rv.setAdapter(adapter);
                        LoadingDialog.showDialog("Query",activity);
                        ThreadPoolProvider.getFixedThreadPool().execute(()->{
                            if(bIsShownInputed){//已录入
                                lstData.clear();
                                lstData.addAll(JsonUtils.parseToList(db5.ask(3600000L*24)));
                            }
                            else{//未录入
                                List<ESONObject> lstLocal = JsonUtils.parseToList(db5.ask(3600000L*24));
                                List<ESONObject> lstNoneSampling = new ArrayList<>();
                                Set<String> sSampling = new HashSet<>();
                                for(ESONObject local:lstLocal){
                                    String idCardNo = local.getJSONValue(db2.getIdFieldName(),"");
                                    if(idCardNo == null ||idCardNo.isEmpty()) continue;
                                    sSampling.add(idCardNo);
                                }
                                List<ESONObject> lstAll   = JsonUtils.parseToList(db2.query(new ArrayList<>(),new ArrayList<>()));
                                for(ESONObject all:lstAll){
                                    String idCardNo = all.getJSONValue(db2.getIdFieldName(),"");
                                    if(idCardNo == null ||idCardNo.isEmpty()) continue;
                                    if(sSampling.contains(idCardNo)) continue;
                                    lstNoneSampling.add(all);
                                }
                                lstData.clear();
                                lstData.addAll(lstNoneSampling);
                            }
                            App.Post(()->rv.getAdapter().notifyDataSetChanged());
                            App.Post(()->LoadingDialog.dismissDialog("Query"));
                        });
                    }
                })
                .setDialogParams(p.x,p.y- ((int) DensityUtils.dp2px(100f)), Gravity.BOTTOM)
                .showDialog();
    }

    public static void showLocalSamplingDialog(BaseActivity activity){
        new EasyDialog(R.layout.dialog_local_sampling,activity)
                .setOnBindDialogListener(new EasyDialog.OnBindDialogListener() {
                    @Override
                    public void onBindDialog(EasyDialogHolder dialogHolder) {
                        ShownView svDbInfo = dialogHolder.getView(R.id.sv_db_info);
                        ESONObject object = Constants.DBConfig.getSelectedDatabase();
                        String sTownName    = object.getJSONValue("townName"   ,"");
                        String sVillageName = object.getJSONValue("villageName","");
                        svDbInfo.tvValue.setText(String.format("%s/%s/%s", Template.getCurrentTemplate().getDatabaseSetting().getRegionName(),sTownName,sVillageName));

                        ShownView svInputed = dialogHolder.getView(R.id.sv_local_input_info);
                        ShownView svUninput = dialogHolder.getView(R.id.sv_local_no_input_info);
                        ThreadPoolProvider.getFixedThreadPool().execute(()->{
                            DaySamplingLogDatabase db5 = Template.getCurrentTemplate().getDaySamplingLogDatabase();
                            UserOverallDatabase    db2 = Template.getCurrentTemplate().getUserOverallDatabase();

                            int count1 = db5.count(3600000L*24L);
                            int count2 = db2.countSync() - count1;
                            if(count1<0){
                                App.Post(()->svInputed.tvValue.setText("查询失败"));
                                App.Post(()->svInputed.tvValue.setOnClickListener(v -> {}));
                            }
                            else if(count1 == 0){
                                App.Post(()->svInputed.tvValue.setText("无记录"));
                                App.Post(()->svInputed.tvValue.setOnClickListener(v -> {}));
                            }
                            else{
                                App.Post(()->svInputed.tvValue.setText(String.format("共%d人，点击查看。",count1)));
                                App.Post(()->svInputed.tvValue.setOnClickListener(v -> showLocalSamplingDetailDialog(activity,true)));
                            }

                            if(count2<0){
                                App.Post(()->svUninput.tvValue.setText("查询失败"));
                                App.Post(()->svUninput.tvValue.setOnClickListener(v -> {}));
                            }
                            else if(count2 == 0){
                                App.Post(()->svUninput.tvValue.setText("无记录"));
                                App.Post(()->svUninput.tvValue.setOnClickListener(v -> {}));
                            }
                            else{
                                App.Post(()->svUninput.tvValue.setText(String.format("共%d人，点击查看。",count2)));
                                App.Post(()->svUninput.tvValue.setOnClickListener(v -> showLocalSamplingDetailDialog(activity,false)));
                            }

                        });

                        dialogHolder.setOnClickListener(R.id.tv_online,v->activity.startActivity(NASearchingActivity.class));
                    }
                })
                .setForegroundResource(R.drawable.shape_common_dialog)
                .setDialogHeight((int) DensityUtils.dp2px(210f))
                .showDialog();
    }
}
