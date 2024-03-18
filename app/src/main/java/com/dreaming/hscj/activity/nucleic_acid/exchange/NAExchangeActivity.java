package com.dreaming.hscj.activity.nucleic_acid.exchange;

import android.Manifest;
import android.graphics.Point;
import android.view.Gravity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
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
import com.dreaming.hscj.template.api.impl.Api;
import com.dreaming.hscj.template.api.impl.ApiConfig;
import com.dreaming.hscj.utils.DensityUtils;
import com.dreaming.hscj.utils.EasyPermission;
import com.dreaming.hscj.utils.StringUtils;
import com.dreaming.hscj.utils.ToastUtils;
import com.dreaming.hscj.widget.InputView;
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

/**
 *
 */
public class NAExchangeActivity extends BaseMVPActivity<NAExchangePresenter> implements INAExchangeContract.View{
    @Override
    public int getContentViewResId() {
        return R.layout.activity_nucleic_acid_exchange;
    }

    @Override
    protected void onBindView(View vContent) {
        ButterKnife.bind(this,vContent);
    }

    @BindView(R.id.iv_src_tub_no)
    InputView ivSrcTubNo;
    @BindView(R.id.iv_tar_tub_no)
    InputView ivTarTubNo;
    @BindView(R.id.rb_normal)
    RadioButton rbNormal;
    @BindView(R.id.rb_regex)
    RadioButton rbRegex;
    @BindView(R.id.tv_test)
    TextView tvTest;
    @BindView(R.id.iv_start_date)
    CardView cvStartDate;
    @BindView(R.id.iv_end_date)
    CardView cvEndDate;
    @BindView(R.id.sv_progress)
    ShownView svProcess;
    @BindView(R.id.pb_process)
    ProgressBar pbProgress;

    @BindView(R.id.sv_transfer_ready)
    ShownView svTransferReady;
    @BindView(R.id.sv_transfer_end)
    ShownView svTransferEnd;

    @BindView(R.id.tv_search)
    TextView tvSearch;

    CompoundButton.OnCheckedChangeListener onMatchModeChanged = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(isChecked){
                if(buttonView.getId() == rbNormal.getId()){
                    cvStartDate.setVisibility(View.GONE);
                    cvEndDate  .setVisibility(View.GONE);
                    tvTest.setEnabled(false);
                }
                else if(buttonView.getId() == rbRegex.getId()){
                    cvStartDate.setVisibility(View.VISIBLE);
                    cvEndDate  .setVisibility(View.VISIBLE);
                    tvTest.setEnabled(true);
                }
            }
        }
    };

    String fmtOfStartDate,fmtOfEndDate;
    EditText etStartDate,etEndDate;

    @Override
    public void initView() {
        setCenterText("条码转移");

        rbNormal.setOnCheckedChangeListener(onMatchModeChanged);
        rbRegex.setOnCheckedChangeListener(onMatchModeChanged);

        Map<String,String> mTypeMapper = Template.getCurrentTemplate().apiOf(6).getRequest().getParamTypeMapper();
        ApiConfig.Locate.Query query = Template.getCurrentTemplate().getApiConfig().getQuery();
        String typeOfStartDate = mTypeMapper.get(query.getRequest().getStartDate());
        fmtOfStartDate  = typeOfStartDate.substring(typeOfStartDate.indexOf(":")+1);
        String typeOfEndDate   = mTypeMapper.get(query.getRequest().getEndDate());
        fmtOfEndDate    = typeOfEndDate.substring(typeOfEndDate.indexOf(":")+1);

        long startDateTimestamp = System.currentTimeMillis();
        try {
            startDateTimestamp = new SimpleDateFormat("yyyy-MM-dd").parse(new SimpleDateFormat("yyyy-MM-dd").format(System.currentTimeMillis())).getTime();
        } catch (ParseException e) { }
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

        svTransferReady.tvValue.setEnabled(false);
        svTransferEnd  .tvValue.setEnabled(false);

        svTransferReady.tvValue.setOnClickListener(v -> showTransferReadyDialog());
        svTransferEnd  .tvValue.setOnClickListener(v -> showTransferEndDialog());
    }

    @OnClick(R.id.tv_test)
    void onTestChecked(){
        String srcNo = ivSrcTubNo.tvValue.getText().toString().trim();
        String tarNo = ivTarTubNo.tvValue.getText().toString().trim();
        Point p = DensityUtils.getScreenSize();
        new EasyDialog(R.layout.dialog_regex_test, this)
                .setOnBindDialogListener(new EasyDialog.OnBindDialogListener() {
                    @Override
                    public void onBindDialog(EasyDialogHolder holder) {
                        InputView ivSrc = holder.getView(R.id.iv_src_tub_no);
                        InputView ivTar = holder.getView(R.id.iv_tar_tub_no);
                        ivSrc.tvValue.setText(srcNo);
                        ivTar.tvValue.setText(tarNo);

                        holder.setOnClickListener(R.id.iv_close,v->{
                            if(!ivSrc.tvValue.getText().toString().equals(srcNo) || !ivTar.tvValue.getText().toString().equals(tarNo)){
                                DialogManager.showAlertDialog(NAExchangeActivity.this,"通知","要应用当前正则匹配参数吗？",v1 -> holder.dismissDialog(),v1 -> {
                                    ivSrcTubNo.tvValue.setText(ivSrc.tvValue.getText().toString());
                                    ivTarTubNo.tvValue.setText(ivTar.tvValue.getText().toString());
                                    holder.dismissDialog();
                                });
                                return;
                            }
                            holder.dismissDialog();
                        });

                        EditText etTest = holder.getView(R.id.et_test);
                        holder.setOnClickListener(R.id.tv_set_1,v -> {
                            ivSrc.tvValue.setText("QD(?<a>([0-9]{8}))");
                            ivTar.tvValue.setText("QD1${a}");

                            etTest.setText("QD00851295\nQD00851291\nQD100851295\nQD00351295\nQD102851295");
                            ToastUtils.show("此场景适合在扫码器扫描时缺失1的情况！");
                        });

                        holder.setOnClickListener(R.id.tv_change,v -> {

                            String regSrc = ivSrc.tvValue.getText().toString();
                            if(regSrc.isEmpty()){
                                ToastUtils.show("转移条码不能为空！");
                                return;
                            }
                            String regTar = ivTar.tvValue.getText().toString();
                            if(regTar.isEmpty()){
                                ToastUtils.show("目标条码不能为空！");
                                return;
                            }
                            String text = etTest.getText().toString().trim();
                            if(text.isEmpty()){
                                ToastUtils.show("待转移测试条码不能为空！");
                                return;
                            }

                            StringBuilder sb = new StringBuilder();
                            String splits[] = text.split("\n");
                            for(String split : splits){
                                if(sb.length() != 0) sb.append("\n");
                                sb.append(split);
                                sb.append(" -> ");
                                try {
                                    String src = split.replaceAll(regSrc,"");
                                    if(!src.isEmpty()){
                                        sb.append("不匹配");
                                        continue;
                                    }
                                    String tar = StringUtils.replaceAll(split,regSrc,regTar);
                                    sb.append(tar);
                                } catch (Exception e) {
                                    sb.append("错误");
                                }
                            }
                            etTest.setText(sb.toString());
                            ToastUtils.show("转换完成！");
                        });
                    }
                })
                .setForegroundResource(R.drawable.shape_dialog_corner_with_tl_tr)
                .setDialogParams(p.x,p.y/2, Gravity.BOTTOM)
                .setAllowDismissWhenTouchOutside(false)
                .setAllowDismissWhenBackPressed(false)
                .showDialog();

    }

    long lTime = System.currentTimeMillis() - 1000L;
    @OnClick(R.id.tv_search)
    void onSearchClicked(){
        long now = System.currentTimeMillis();
        if(now - lTime < 1000L) return;
        lTime = now;

        String srcNo = ivSrcTubNo.tvValue.getText().toString().trim();
        if(srcNo.isEmpty()){
            ToastUtils.show("转移条码不能为空！");
            return;
        }
        String tarNo = ivTarTubNo.tvValue.getText().toString().trim();
        if(tarNo.isEmpty()){
            ToastUtils.show("目标条码不能为空！");
            return;
        }

        if(rbRegex.isChecked()){
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
                ToastUtils.show("请授予写入外置存储器权限以保存转移结果文件！");
            }
            requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, new EasyPermission.PermissionResultListener() {
                @Override
                public void onPermissionGranted() {
                    if(tvSearch.getVisibility() == View.GONE) return;
                    tvSearch.setVisibility(View.GONE);
                    mPresenter.transfer(srcNo,tarNo,startDate,endDate);
                }

                @Override
                public void onPermissionDenied() {
                    ToastUtils.show("未授予程序写入外置存储器权限，无法导出转移结果！");
                }
            });
            return;
        }

        if(!hasPermision(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            ToastUtils.show("请授予写入外置存储器权限以保存导出文件！");
        }
        requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, new EasyPermission.PermissionResultListener() {
            @Override
            public void onPermissionGranted() {
                if(tvSearch.getVisibility() == View.GONE) return;
                tvSearch.setVisibility(View.GONE);
                mPresenter.transfer(srcNo,tarNo);
            }

            @Override
            public void onPermissionDenied() {
                ToastUtils.show("未授予程序写入外置存储器权限，无法导出文件！");
            }
        });
    }

    @Override
    public void onTransferSuccess(String path) {
        svTransferReady.tvValue.setEnabled(true);
        svTransferEnd  .tvValue.setEnabled(true);

        DialogManager.showAlertDialogWithConfirm(this,"转换成功！\n"+path,null);
    }

    @Override
    public void onTransferFailure() {
        DialogManager.showAlertDialogWithConfirm(this,"转换失败！",null);
    }

    @Override
    public void showUnmatchedDialog(List<ESONObject> log, android.view.View.OnClickListener onAbort, android.view.View.OnClickListener onContinue) {
        Point p = DensityUtils.getScreenSize();
        new EasyDialog(R.layout.dialog_exchange_no_match, this)
                .setOnBindDialogListener(new EasyDialog.OnBindDialogListener() {
                    @Override
                    public void onBindDialog(EasyDialogHolder easyDialogHolder) {
                        TextView tvTitle = easyDialogHolder.getView(R.id.tv_title);
                        tvTitle.setText("");
                        easyDialogHolder.setOnClickListener(R.id.tv_continue,v -> {

                        });
                        RecyclerView rvExchange = easyDialogHolder.getView(R.id.rv_exchange);
                        rvExchange.setLayoutManager(new LinearLayoutManager(NAExchangeActivity.this));
                        rvExchange.setAdapter(new EasyAdapter(NAExchangeActivity.this, R.layout.recy_exchange_no_match, log, new EasyAdapter.IEasyAdapter<ESONObject>() {
                            @Override
                            public void convert(EasyViewHolder holder, ESONObject data, int position) {
                                holder.setText(R.id.tv_src       ,data.getJSONValue(mPresenter.query.getResponse().getTubNo(),""));
                                holder.setText(R.id.tv_name      ,data.getJSONValue(mPresenter.query.getResponse().getName() ,""));
                                holder.setText(R.id.tv_id_card_no,data.getJSONValue(mPresenter.query.getResponse().getIdNo() ,""));
                                holder.setText(R.id.tv_phone     ,data.getJSONValue(mPresenter.query.getResponse().getPhone(),""));
                            }
                        }));
                    }
                })
                .setForegroundResource(R.drawable.shape_dialog_corner_with_tl_tr)
                .setDialogParams(p.x,p.y-80, Gravity.BOTTOM)
                .showDialog();
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
    public void onTransferReadyUpdate(int count) {
        svTransferReady.tvValue.setText(String.format("共%d条。",count));
    }

    @Override
    public void onTransferEndUpdate(int count) {
        svTransferEnd.tvValue.setText(String.format("共%d条。",count));
    }

    void showTransferReadyDialog(){
        if(mPresenter.getTransferReadyRecodes().isEmpty()){
            ToastUtils.show("没有相关数据！");
            return;
        }
        Point p = DensityUtils.getScreenSize();
        new EasyDialog(R.layout.dialog_exchange_transfer_record, this)
            .setOnBindDialogListener(new EasyDialog.OnBindDialogListener() {
                @Override
                public void onBindDialog(EasyDialogHolder easyDialogHolder) {
                    TextView tvTitle = easyDialogHolder.getView(R.id.tv_title);
                    tvTitle.setText("未转移完成数据");
                    CardView cvTitleEnd   = easyDialogHolder.getView(R.id.title_transfer_end);
                    cvTitleEnd.setVisibility(View.VISIBLE);
                    CardView cvTitleReady = easyDialogHolder.getView(R.id.title_transfer_ready);
                    cvTitleReady.setVisibility(View.INVISIBLE);
                    RecyclerView rvExchange = easyDialogHolder.getView(R.id.rv_exchange);
                    rvExchange.setLayoutManager(new LinearLayoutManager(NAExchangeActivity.this));
                    rvExchange.setAdapter(new EasyAdapter(NAExchangeActivity.this, R.layout.recy_exchange_transfer_ready, mPresenter.getTransferReadyRecodes(), new EasyAdapter.IEasyAdapter<ESONObject>() {
                        @Override
                        public void convert(EasyViewHolder holder, ESONObject data, int position) {
                            holder.setText(R.id.tv_barcode   ,data.getJSONValue(mPresenter.query.getResponse().getTubNo(),""));
                            holder.setText(R.id.tv_name      ,data.getJSONValue(mPresenter.query.getResponse().getName() ,""));
                            holder.setText(R.id.tv_id_card_no,data.getJSONValue(mPresenter.query.getResponse().getIdNo() ,""));
                            holder.setText(R.id.tv_state     ,data.getJSONValue(mPresenter.stateFieldName,""));
                        }
                    }));
                }
            })
            .setForegroundResource(R.drawable.shape_dialog_corner_with_tl_tr)
            .setDialogParams(p.x,p.y-80, Gravity.BOTTOM)
            .showDialog();
    }

    void showTransferEndDialog(){
        if(mPresenter.getTransferEndRecodes().isEmpty()){
            ToastUtils.show("没有相关数据！");
            return;
        }
        Point p = DensityUtils.getScreenSize();
        new EasyDialog(R.layout.dialog_exchange_transfer_record, this)
                .setOnBindDialogListener(new EasyDialog.OnBindDialogListener() {
                    @Override
                    public void onBindDialog(EasyDialogHolder easyDialogHolder) {
                        TextView tvTitle = easyDialogHolder.getView(R.id.tv_title);
                        tvTitle.setText("已转移数据");
                        CardView cvTitleEnd   = easyDialogHolder.getView(R.id.title_transfer_end);
                        cvTitleEnd.setVisibility(View.INVISIBLE);
                        CardView cvTitleReady = easyDialogHolder.getView(R.id.title_transfer_ready);
                        cvTitleReady.setVisibility(View.VISIBLE);
                        RecyclerView rvExchange = easyDialogHolder.getView(R.id.rv_exchange);
                        rvExchange.setLayoutManager(new LinearLayoutManager(NAExchangeActivity.this));
                        rvExchange.setAdapter(new EasyAdapter(NAExchangeActivity.this, R.layout.recy_exchange_transfer_end, mPresenter.getTransferEndRecodes(), new EasyAdapter.IEasyAdapter<ESONObject>() {
                            @Override
                            public void convert(EasyViewHolder holder, ESONObject data, int position) {
                                holder.setText(R.id.tv_src       ,data.getJSONValue(mPresenter.query.getResponse().getTubNo(),""));
                                holder.setText(R.id.tv_dst       ,data.getJSONValue(mPresenter.tarFieldName                  ,""));
                                holder.setText(R.id.tv_name      ,data.getJSONValue(mPresenter.query.getResponse().getName() ,""));
                                holder.setText(R.id.tv_id_card_no,data.getJSONValue(mPresenter.query.getResponse().getIdNo() ,""));
                            }
                        }));
                    }
                })
                .setForegroundResource(R.drawable.shape_dialog_corner_with_tl_tr)
                .setDialogParams(p.x,p.y-80, Gravity.BOTTOM)
                .showDialog();
    }
}
