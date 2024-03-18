package com.dreaming.hscj.activity.nucleic_acid.export;

import android.Manifest;
import android.graphics.Point;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dreaming.hscj.R;
import com.dreaming.hscj.base.contract.BaseMVPActivity;
import com.dreaming.hscj.core.EasyAdapter.EasyAdapter;
import com.dreaming.hscj.core.EasyAdapter.EasyViewHolder;
import com.dreaming.hscj.core.ViewInjector;
import com.dreaming.hscj.dialog.DialogManager;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.template.api.impl.ApiConfig;
import com.dreaming.hscj.utils.DensityUtils;
import com.dreaming.hscj.utils.EasyPermission;
import com.dreaming.hscj.utils.ToastUtils;
import com.dreaming.hscj.widget.ShownView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import priv.songxusheng.easydialog.EasyDialog;
import priv.songxusheng.easydialog.EasyDialogHolder;
import priv.songxusheng.easyjson.ESONObject;

public class NAExportActivity extends BaseMVPActivity<NAExportPresenter> implements INAExportContract.View {

    @Override
    public int getContentViewResId() {
        return R.layout.activity_nucleic_acid_export;
    }

    @BindView(R.id.iv_start_date)
    CardView cvStartDate;
    EditText etStartDate;
    String fmtOfStartDate;
    @BindView(R.id.iv_end_date)
    CardView cvEndDate;
    EditText etEndDate;
    String fmtOfEndDate;
    @BindView(R.id.sv_progress)
    ShownView svProcess;
    @BindView(R.id.pb_process)
    ProgressBar pbProgress;


    @BindView(R.id.sv_local_checked)
    ShownView svLocalChecked;
    @BindView(R.id.sv_local_uncheck)
    ShownView svLocalUncheck;
    @BindView(R.id.sv_net_checked)
    ShownView svNetChecked;
    @BindView(R.id.tv_export)
    TextView tvExport;

    @Override
    protected void onBindView(View vContent) {
        ButterKnife.bind(this,vContent);
    }

    @Override
    public void initView() {
        setCenterText("采样记录导出");

        Map<String,String> mTypeMapper = Template.getCurrentTemplate().apiOf(6).getRequest().getParamTypeMapper();
        ApiConfig.Locate.Query query = Template.getCurrentTemplate().getApiConfig().getQuery();
        String typeOfStartDate = mTypeMapper.get(query.getRequest().getStartDate());
        fmtOfStartDate  = typeOfStartDate.substring(typeOfStartDate.indexOf(":")+1);
        String typeOfEndDate   = mTypeMapper.get(query.getRequest().getEndDate());
        fmtOfEndDate    = typeOfEndDate.substring(typeOfEndDate.indexOf(":")+1);

        long startDateTimestamp = System.currentTimeMillis();
        try {
            startDateTimestamp = new SimpleDateFormat("yyyy-MM-dd").parse(new SimpleDateFormat("yyyy-MM-dd").format(System.currentTimeMillis())).getTime();
        } catch (ParseException e) {
        }
        long endDateTimestamp = startDateTimestamp + 24*3600000L;

        TextView tvStartDateName = cvStartDate.findViewById(R.id.tv_input_date);
        tvStartDateName.setText("开始时间");
        etStartDate = cvStartDate.findViewById(R.id.ev_date);
        etStartDate.setHint("请输入查询开始时间");
        String startDateText = new SimpleDateFormat(fmtOfStartDate).format(startDateTimestamp);
        etStartDate.setText(startDateText);
        ViewInjector.injectDate(this,cvStartDate,fmtOfStartDate);


        TextView tvEndDateName = cvEndDate.findViewById(R.id.tv_input_date);
        tvEndDateName.setText("结束时间");
        etEndDate = cvEndDate.findViewById(R.id.ev_date);
        etEndDate.setHint("请输入查询结束时间");
        String endDateText = new SimpleDateFormat(fmtOfEndDate).format(endDateTimestamp);
        etEndDate.setText(endDateText);
        ViewInjector.injectDate(this,cvEndDate,fmtOfEndDate);

        svLocalChecked.tvValue.setText("等待导出操作");
        svLocalUncheck.tvValue.setText("等待导出操作");
        svNetChecked  .tvValue.setText("等待导出操作");

        svLocalChecked.tvValue.setEnabled(false);
        svLocalUncheck.tvValue.setEnabled(false);
        svNetChecked  .tvValue.setEnabled(false);

        svLocalChecked.tvValue.setOnClickListener(v->showLocalCheckedDialog());
        svLocalUncheck.tvValue.setOnClickListener(v->showLocalUncheckDialog());
        svNetChecked  .tvValue.setOnClickListener(v->showNetCheckedDialog());
    }

    @Override
    public void onMessage(String message) {
        svProcess.tvValue.setText(message);
    }

    @Override
    public void onProgress(int progress) {
        pbProgress.setProgress(progress);
    }

    @Override
    public void onLocalCheckedUpdate(int count) {
        svLocalChecked.tvValue.setText(String.format("共%d条。",count));
    }

    @Override
    public void onLocalUncheckUpdate(int count) {
        svLocalUncheck.tvValue.setText(String.format("共%d条。",count));
    }

    @Override
    public void onNetCheckedUpdate(int count) {
        svNetChecked.tvValue.setText(String.format("共%d条。",count));
    }

    long lTime = System.currentTimeMillis() - 1000L;
    @OnClick(R.id.tv_export)
    void onExportClicked(){
        long now = System.currentTimeMillis();
        if(now - lTime < 1000L) return;
        lTime = now;

        String startDate = etStartDate.getText().toString();
        long start = 0;
        try {
            start = new SimpleDateFormat(fmtOfStartDate).parse(startDate).getTime();
        } catch (Exception e) {
            ToastUtils.show("起始日期不是有效的查询日期格式！");
            return;
        }

        String endDate = etEndDate.getText().toString();
        long end = 0;
        try {
            end = new SimpleDateFormat(fmtOfEndDate).parse(endDate).getTime();
        } catch (Exception e) {
            ToastUtils.show("结束日期不是有效的查询日期格式！");
            return;
        }

        if(start >= end){
            ToastUtils.show("起始日期不能大于结束日期！");
            return;
        }

        if(!hasPermision(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            ToastUtils.show("请授予写入外置存储器权限以保存导出文件！");
        }
        requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, new EasyPermission.PermissionResultListener() {
            @Override
            public void onPermissionGranted() {
                if(tvExport.getVisibility() == View.GONE) return;
                tvExport.setVisibility(View.GONE);
                mPresenter.export(startDate,endDate);
            }

            @Override
            public void onPermissionDenied() {
                ToastUtils.show("未授予程序写入外置存储器权限，无法导出文件！");
            }
        });
    }

    @Override
    public void showEmptyDialog() {
        DialogManager.showAlertDialogWithConfirm(this,"网络没有采样记录！",null);
    }

    @Override
    public void showSuccessDialog(List<String> paths) {
        svLocalChecked.tvValue.setEnabled(true);
        svLocalUncheck.tvValue.setEnabled(true);
        svNetChecked  .tvValue.setEnabled(true);

        StringBuilder sb = new StringBuilder();
        sb.append("导出成功！");
        for (String path : paths) {
            sb.append("\n");
            sb.append(path);
        }
        DialogManager.showAlertDialogWithConfirm(this,sb.toString(),null);
    }

    void showLocalCheckedDialog(){
        if(mPresenter.getLocalCheckedData().isEmpty()){
            ToastUtils.show("没有相关数据！");
            return;
        }
        Point p = DensityUtils.getScreenSize();
        new EasyDialog(R.layout.dialog_export_record, this)
            .setOnBindDialogListener(new EasyDialog.OnBindDialogListener() {
                @Override
                public void onBindDialog(EasyDialogHolder easyDialogHolder) {
                    TextView tvTitle = easyDialogHolder.getView(R.id.tv_title);
                    tvTitle.setText("本地已核酸检测人员");
                    CardView tvCheckedTitle = easyDialogHolder.getView(R.id.title_export_checked);
                    tvCheckedTitle.setVisibility(View.VISIBLE);
                    CardView tvUncheckTitle = easyDialogHolder.getView(R.id.title_export_uncheck);
                    tvUncheckTitle.setVisibility(View.INVISIBLE);
                    RecyclerView rvExport = easyDialogHolder.getView(R.id.rv_export);
                    rvExport.setLayoutManager(new LinearLayoutManager(NAExportActivity.this));
                    rvExport.setAdapter(new EasyAdapter(NAExportActivity.this, R.layout.recy_export_uncheck, mPresenter.getLocalCheckedData(), new EasyAdapter.IEasyAdapter<ESONObject>() {
                        @Override
                        public void convert(EasyViewHolder holder, ESONObject data, int position) {
                            holder.setText(R.id.tv_name      ,data.getJSONValue(Template.getCurrentTemplate().getIdCardNameFieldName(), ""));
                            holder.setText(R.id.tv_id_card_no,data.getJSONValue(Template.getCurrentTemplate().getIdCardNoFieldName(), ""));
                            holder.setText(R.id.tv_src       ,data.getJSONValue(mPresenter.tubNoFieldName,""));
                            holder.setText(R.id.tv_time      ,data.getJSONValue(mPresenter.samplingTimeFieldName,""));
                        }
                    }));
                }
            })
            .setForegroundResource(R.drawable.shape_dialog_corner_with_tl_tr)
            .setDialogParams(p.x,p.y- 180, Gravity.BOTTOM)
            .showDialog();
    }

    void showLocalUncheckDialog(){
        if(mPresenter.getLocalUnCheckData().isEmpty()){
            ToastUtils.show("没有相关数据！");
            return;
        }
        Point p = DensityUtils.getScreenSize();
        new EasyDialog(R.layout.dialog_export_record, this)
            .setOnBindDialogListener(new EasyDialog.OnBindDialogListener() {
                @Override
                public void onBindDialog(EasyDialogHolder easyDialogHolder) {
                    easyDialogHolder.setOnClickListener(R.id.iv_close,v -> easyDialogHolder.dismissDialog());
                    TextView tvTitle = easyDialogHolder.getView(R.id.tv_title);
                    CardView tvCheckedTitle = easyDialogHolder.getView(R.id.title_export_checked);
                    tvCheckedTitle.setVisibility(View.INVISIBLE);
                    CardView tvUncheckTitle = easyDialogHolder.getView(R.id.title_export_uncheck);
                    tvUncheckTitle.setVisibility(View.VISIBLE);
                    tvTitle.setText("本地未核酸检测人员");
                    RecyclerView rvExport = easyDialogHolder.getView(R.id.rv_export);
                    rvExport.setLayoutManager(new LinearLayoutManager(NAExportActivity.this));
                    rvExport.setAdapter(new EasyAdapter(NAExportActivity.this, R.layout.recy_export_uncheck, mPresenter.getLocalUnCheckData(), new EasyAdapter.IEasyAdapter<ESONObject>() {
                        @Override
                        public void convert(EasyViewHolder holder, ESONObject data, int position) {
                            holder.setText(R.id.tv_name      ,data.getJSONValue(Template.getCurrentTemplate().getIdCardNameFieldName(), ""));
                            holder.setText(R.id.tv_id_card_no,data.getJSONValue(Template.getCurrentTemplate().getIdCardNoFieldName(), ""));
                            String phone = data.getJSONValue(Template.getCurrentTemplate().getPhoneFieldName(), "");
                            holder.setText(R.id.tv_phone     ,phone);
                            holder.setOnClickListener(R.id.tv_phone,v -> DialogManager.showAlertDialog(NAExportActivity.this,"提示","要拨打电话到["+phone+"]吗？",v1 -> {},v2->{
                                ViewInjector.callPhone(NAExportActivity.this,phone);
                            }));
                        }
                    }));
                }
            })
            .setForegroundResource(R.drawable.shape_dialog_corner_with_tl_tr)
            .setDialogParams(p.x,p.y - 180, Gravity.BOTTOM)
            .showDialog();
    }

    void showNetCheckedDialog(){
        if(mPresenter.getNetCheckedData().isEmpty()){
            ToastUtils.show("没有相关数据！");
            return;
        }
        Point p = DensityUtils.getScreenSize();
        new EasyDialog(R.layout.dialog_export_record, this)
            .setOnBindDialogListener(new EasyDialog.OnBindDialogListener() {
                @Override
                public void onBindDialog(EasyDialogHolder easyDialogHolder) {
                    easyDialogHolder.setOnClickListener(R.id.iv_close,v -> easyDialogHolder.dismissDialog());
                    TextView tvTitle = easyDialogHolder.getView(R.id.tv_title);
                    tvTitle.setText("网络已核酸检测人员（除本地）");
                    RecyclerView rvExport = easyDialogHolder.getView(R.id.rv_export);
                    rvExport.setLayoutManager(new LinearLayoutManager(NAExportActivity.this));
                    rvExport.setAdapter(new EasyAdapter(NAExportActivity.this, R.layout.recy_export_uncheck, mPresenter.getNetCheckedData(), new EasyAdapter.IEasyAdapter<ESONObject>() {
                        @Override
                        public void convert(EasyViewHolder holder, ESONObject data, int position) {
                            holder.setText(R.id.tv_name      ,data.getJSONValue(mPresenter.query.getResponse().getName(),""));
                            holder.setText(R.id.tv_id_card_no,data.getJSONValue(mPresenter.query.getResponse().getIdNo(),""));
                            holder.setText(R.id.tv_src       ,data.getJSONValue(mPresenter.query.getResponse().getTubNo(),""));
                            holder.setText(R.id.tv_time      ,data.getJSONValue(mPresenter.query.getResponse().getSamplingTime(),""));
                        }
                    }));
                }
            })
            .setForegroundResource(R.drawable.shape_dialog_corner_with_tl_tr)
            .setDialogParams(p.x,p.y - 180, Gravity.BOTTOM)
            .showDialog();
    }
}
