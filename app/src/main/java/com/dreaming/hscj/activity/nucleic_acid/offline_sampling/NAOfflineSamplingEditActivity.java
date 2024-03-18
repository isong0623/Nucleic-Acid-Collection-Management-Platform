package com.dreaming.hscj.activity.nucleic_acid.offline_sampling;

import android.content.Intent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.dreaming.hscj.R;
import com.dreaming.hscj.base.BaseActivity;
import com.dreaming.hscj.core.BundleBuilder;
import com.dreaming.hscj.core.ViewInjector;
import com.dreaming.hscj.dialog.DialogManager;
import com.dreaming.hscj.entity.OfflineExcel;
import com.dreaming.hscj.utils.ToastUtils;
import com.dreaming.hscj.widget.InputView;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;

public class NAOfflineSamplingEditActivity extends BaseActivity {
    public static void createOfflineFile(BaseActivity activity, File f, IOfflineEditCallback cb){
        activity.startActivityForResult(NAOfflineSamplingEditActivity.class, BundleBuilder.create().put("create", true).put("path", f.getAbsolutePath()).build(), 1024, new OnActivityResultItemCallBack() {
            @Override
            public void OnActivityRequestResult(int resultCode, Intent data) {
                if(data == null || data.getStringExtra("path") == null){
                    cb.onClosed(f.getAbsolutePath());
                    return;
                }
                cb.onClosed(data.getStringExtra("path"));
            }
        });
    }

    public static void editOfflineFile(BaseActivity activity, File f, IOfflineEditCallback cb){
        activity.startActivityForResult(NAOfflineSamplingEditActivity.class, BundleBuilder.create().put("path", f.getAbsolutePath()).build(), 1024, new OnActivityResultItemCallBack() {
            @Override
            public void OnActivityRequestResult(int resultCode, Intent data) {
                if(data == null || data.getStringExtra("path") == null){
                    cb.onClosed(f.getAbsolutePath());
                    return;
                }
                cb.onClosed(data.getStringExtra("path"));
            }
        });
    }

    public interface IOfflineEditCallback{
        void onClosed(String newPath);
    }

    @Override
    public int getContentViewResId() {
        return R.layout.activity_nucleic_acid_offline_sampling_file_edit;
    }

    @BindView(R.id.ev_file_name)
    EditText etFileName;

    @BindView(R.id.iv_sampling_address)
    InputView ivSamplingAddress ;
    @BindView(R.id.iv_sampling_people)
    InputView ivSamplingPeople  ;
    @BindView(R.id.iv_sender_people)
    InputView ivSenderPeople;
    @BindView(R.id.iv_send_phone)
    InputView ivSenderPhoneNo;
    @BindView(R.id.iv_receiver_people)
    InputView ivReceiverPeople;

    @BindView(R.id.ev_send_date)
    EditText  etSendTime;
    @BindView(R.id.iv_send_date)
    ImageView ivSendTime;

    @BindView(R.id.ev_receive_date)
    EditText  etReceiveTime;
    @BindView(R.id.iv_receive_date)
    ImageView ivReceiveTime ;

    @BindView(R.id.tv_delete)
    TextView tvDelete;
    @BindView(R.id.tv_save)
    TextView tvSave;

    @Override
    protected void onBindView(View vContent) {
        ButterKnife.bind(this,vContent);
    }

    boolean bIsCreate;
    String strPath;
    @Override
    public void initView() {
        bIsCreate = getIntent().getBooleanExtra("create",false);
        strPath   = getIntent().getStringExtra("path");
        setCenterText(bIsCreate?"创建离线文件":"编辑离线文件");
        ViewInjector.injectOfflineDate(this,etSendTime,ivSendTime);
        ViewInjector.injectOfflineDate(this,etReceiveTime,ivReceiveTime);

        Intent intent = new Intent();
        intent.putExtra("path",strPath);
        setResult(RESULT_OK,intent);

        if(bIsCreate){
            initForCreate(new File(strPath));
        }
        else{
            initForEdit(new File(strPath));
        }
    }

    private boolean checkOfflineTime(String time){
        if(time == null) return false;
        time = time.trim();
        try {
            String month = time.substring(0,time.indexOf("月"));
            time = time.substring(time.indexOf("月")+1);
            String day = time.substring(0,time.indexOf("日"));
            time = time.substring(time.indexOf("日")+1);
            String hour = time.substring(0,time.indexOf("时"));
            time = time.substring(time.indexOf("时")+1);

            int m = Integer.parseInt(month);
            int d = Integer.parseInt(day);
            int h = Integer.parseInt(hour);

            long now = System.currentTimeMillis();
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(now);
            int year = c.get(Calendar.YEAR);
            int mon = c.get(Calendar.MONTH)+1;

            if(mon == 1 && m == 12){
                --year;
                c.set(Calendar.YEAR,year);
            }

            c.set(year,m,d,h,1);
            if(c.get(Calendar.YEAR)!=year) return false;
            if(c.get(Calendar.MONTH)!=m) return false;
            if(c.get(Calendar.DAY_OF_MONTH)!=d) return false;
            if(c.get(Calendar.HOUR_OF_DAY)!=h) return false;
            return time.length() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public void initForCreate(File f){
        Calendar c = Calendar.getInstance();
        etFileName.setText(String.format("表格（二）（人工登记）新冠肺炎核酸20合一混采检测登记表——%04d年%02d月%02d日%02d时%02d分",
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH)+1,
                c.get(Calendar.DAY_OF_MONTH),
                c.get(Calendar.HOUR_OF_DAY),
                c.get(Calendar.MINUTE)
        ));
        tvDelete.setVisibility(View.GONE);

//        ivSamplingAddress.tvValue.setText("唐家采样点");
//        ivSamplingPeople .tvValue.setText("张三");
//        ivSenderPeople   .tvValue.setText("李四");
//        etSendTime               .setText("7月18日4时");
//        ivSenderPhoneNo  .tvValue.setText("13888888888");
//        ivReceiverPeople .tvValue.setText("王五");
//        etReceiveTime            .setText("7月18日6时");

        tvSave.setText("新建");
        tvSave.setOnClickListener(v -> {
            if(etFileName.getText().toString().trim().isEmpty()) { ToastUtils.show(etFileName.getHint().toString());return;}
            if(ivSamplingAddress.tvValue.getText().toString().trim().isEmpty()){ToastUtils.show(ivSamplingAddress.tvValue.getHint().toString());return;}
            if(ivSamplingPeople .tvValue.getText().toString().trim().isEmpty()){ToastUtils.show(ivSamplingPeople .tvValue.getHint().toString());return;}
            if(!checkOfflineTime(etSendTime.getText().toString())) {ToastUtils.show(etSendTime.getHint().toString());return;}
            if(ivSenderPeople   .tvValue.getText().toString().trim().isEmpty()){ToastUtils.show(ivSenderPeople   .tvValue.getHint().toString());return;}
            if(ivSenderPhoneNo  .tvValue.getText().toString().trim().isEmpty()){ToastUtils.show(ivSenderPhoneNo  .tvValue.getHint().toString());return;}
            if(ivReceiverPeople .tvValue.getText().toString().trim().isEmpty()){ToastUtils.show(ivReceiverPeople .tvValue.getHint().toString());return;}
            if(!checkOfflineTime(etReceiveTime.getText().toString())){ToastUtils.show(etReceiveTime.getHint().toString());return;}

            OfflineExcel.Config config = OfflineExcel.Config.getDefault();
            config.path                  = f.getAbsolutePath()+File.separator+etFileName.getText().toString().trim().replaceAll(".xls","").replaceAll(".xlsx","")+".xls";
            config.samplingAddress.value = ivSamplingAddress.tvValue.getText().toString().trim();
            config.samplingPeople .value = ivSamplingPeople .tvValue.getText().toString().trim();
            config.senderPeople   .value = ivSenderPeople   .tvValue.getText().toString().trim();
            config.senderPhoneNo  .value = ivSenderPhoneNo  .tvValue.getText().toString().trim();
            config.receiverPeople .value = ivReceiverPeople .tvValue.getText().toString().trim();

            config.samplingTime   .value = "";
            config.sendTime       .value = etSendTime.getText().toString().trim();
            config.receiveTime    .value = etReceiveTime.getText().toString().trim();
            try {
                OfflineExcel excel = OfflineExcel.create(config);
                try {
                    excel.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    ToastUtils.show("离线文件关闭失败！");
                }
            } catch (IOException e) {
                e.printStackTrace();
                ToastUtils.show("创建离线文件失败！");
                return;
            }
            Intent intent = new Intent();
            intent.putExtra("path",config.path);
            setResult(RESULT_OK,intent);
            finish();
        });
    }

    public void initForEdit(File f){

        OfflineExcel excel = null;
        try {
            excel = OfflineExcel.load(f.getAbsolutePath());
            etFileName.setText(f.getName().replaceAll(".xls","").replaceAll(".xlsx",""));
            ivSamplingAddress.tvValue.setText(excel.getConfig().samplingAddress.value);
            ivSamplingPeople .tvValue.setText(excel.getConfig().samplingPeople .value);
            ivSenderPeople   .tvValue.setText(excel.getConfig().senderPeople   .value);
            etSendTime               .setText(excel.getConfig().sendTime       .value);
            ivSenderPhoneNo  .tvValue.setText(excel.getConfig().senderPhoneNo  .value);
            ivReceiverPeople .tvValue.setText(excel.getConfig().receiverPeople .value);
            etReceiveTime            .setText(excel.getConfig().receiveTime    .value);
        } catch (Exception e) {
            ToastUtils.show("文件读取失败！");
            finish();
            return;
        }
        tvDelete.setOnClickListener(v -> DialogManager.showAlertDialog(this,"通知","确定要删除该离线文件吗？",null, v1->{
            if(f.delete()){
                ToastUtils.show("删除成功！");
                Intent intent = new Intent();
                intent.putExtra("path","");
                setResult(RESULT_OK,intent);
                finish();
            }
            else{
                ToastUtils.show("删除失败！");
            }
        }));
        final OfflineExcel finalExcel = excel;
        tvSave.setOnClickListener(v -> {
            if(etFileName.getText().toString().trim().isEmpty()) {ToastUtils.show(etFileName.getHint().toString());return;}
            String newPath = f.getAbsolutePath();
            newPath = newPath.substring(0,newPath.lastIndexOf(f.getName()));
            newPath = newPath + File.separator + etFileName.getText().toString().trim().replaceAll(".xls","").replaceAll(".xlsx","")+".xls";

            if(!finalExcel.getConfig().path.equals(newPath)){
                try {
                    finalExcel.setPath(newPath);
                } catch (Exception e) {
                    e.printStackTrace();
                    ToastUtils.show("重命名失败！");
                    return;
                }
            }

            if(ivSamplingAddress.tvValue.getText().toString().trim().isEmpty()){ToastUtils.show(ivSamplingAddress.tvValue.getHint().toString());return;}
            if(ivSamplingPeople .tvValue.getText().toString().trim().isEmpty()){ToastUtils.show(ivSamplingPeople .tvValue.getHint().toString());return;}
            if(ivSenderPeople   .tvValue.getText().toString().trim().isEmpty()){ToastUtils.show(ivSenderPeople   .tvValue.getHint().toString());return;}
            if(!checkOfflineTime(etSendTime.getText().toString())) {ToastUtils.show(etSendTime.getHint().toString());return;}
            if(ivSenderPhoneNo  .tvValue.getText().toString().trim().isEmpty()){ToastUtils.show(ivSenderPhoneNo  .tvValue.getHint().toString());return;}
            if(ivReceiverPeople .tvValue.getText().toString().trim().isEmpty()){ToastUtils.show(ivReceiverPeople .tvValue.getHint().toString());return;}
            if(!checkOfflineTime(etReceiveTime.getText().toString())){ToastUtils.show(etReceiveTime.getHint().toString());return;}

            if(!finalExcel.getConfig().samplingAddress.value.equals(ivSamplingAddress.tvValue.getText().toString().trim())) finalExcel.setSamplingAddress(ivSamplingAddress.tvValue.getText().toString().trim());
            if(!finalExcel.getConfig().samplingPeople .value.equals(ivSamplingPeople .tvValue.getText().toString().trim())) finalExcel.setSamplingPeople (ivSamplingPeople .tvValue.getText().toString().trim());
            if(!finalExcel.getConfig().senderPeople   .value.equals(ivSenderPeople   .tvValue.getText().toString().trim())) finalExcel.setSenderPeople   (ivSenderPeople   .tvValue.getText().toString().trim());
            if(!finalExcel.getConfig().senderPhoneNo  .value.equals(ivSenderPhoneNo  .tvValue.getText().toString().trim())) finalExcel.setSenderPhoneNo  (ivSenderPhoneNo  .tvValue.getText().toString().trim());
            if(!finalExcel.getConfig().receiverPeople .value.equals(ivReceiverPeople .tvValue.getText().toString().trim())) finalExcel.setReceiverPeople (ivReceiverPeople .tvValue.getText().toString().trim());

            try { finalExcel.close(); } catch (Exception e) { }

            Intent intent = new Intent();
            intent.putExtra("path",newPath);
            setResult(RESULT_OK,intent);
            finish();
        });
    }
}
