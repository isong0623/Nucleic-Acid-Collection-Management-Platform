package com.dreaming.hscj.activity.nucleic_acid.searching;

import android.app.DatePickerDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.dreaming.hscj.Constants;
import com.dreaming.hscj.R;
import com.dreaming.hscj.base.contract.BaseMVPActivity;
import com.dreaming.hscj.core.ViewInjector;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.template.api.impl.Api;
import com.dreaming.hscj.template.api.impl.ApiConfig;
import com.dreaming.hscj.template.api.wrapper.ApiParam;
import com.dreaming.hscj.utils.DensityUtils;
import com.dreaming.hscj.utils.ToastUtils;
import com.dreaming.hscj.widget.ShownView;

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import priv.songxusheng.easyjson.ESONObject;

public class NASearchingActivity extends BaseMVPActivity<NASearchingPresenter> implements INASearchingContract.View{
    private static final String TAG = NASearchingActivity.class.getSimpleName();
    @Override
    public int getContentViewResId() {
        return R.layout.activity_nucleic_acid_searching;
    }

    @Override
    protected void onBindView(View vContent) {
        ButterKnife.bind(this,vContent);
    }

    @BindView(R.id.tv_search)
    TextView tvSearch;

    @Override
    public void initView() {
        setCenterText("网络核酸采样记录查询");

        setOnKeyBoardLayoutStateChangeListener(new OnKeyBoardLayoutStateChangeListener() {
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) tvSearch.getLayoutParams();
            @Override
            public void onKeyBoardShow(int keyBoardHeight, int gapHeight) {
                params.bottomMargin = keyBoardHeight - gapHeight + dp2px(10);
                tvSearch.setLayoutParams(params);
                Log.e(TAG,"onKeyboardShow");
            }

            @Override
            public void onKeyBoardHide() {
                Log.e(TAG,"onKeyboardHide");
                params.bottomMargin = dp2px(30f);
                tvSearch.setLayoutParams(params);
            }
        });

        buildViewTree();
    }

    boolean bEnableScanBarcode = false;
    EditText etBarcode = null;
    @Override
    protected boolean enableVolumeUpScanBarcode() {
        return bEnableScanBarcode;
    }

    @Override
    protected void onScanBarcodeSuccess(String barcode) {
        etBarcode.setText(barcode);
        ToastUtils.show("扫描成功！");
    }

    @Override
    protected void onScanBarcodeFailure() {
        ToastUtils.show("扫描失败！");
    }

    boolean bEnableScanIdCard  = false;
    EditText etIdCardNo = null;
    @Override
    protected boolean enableVolumeDownRecognizeIDCard() {
        return bEnableScanIdCard;
    }

    @Override
    protected void onRecognizeCardSuccess(String idCard, String type) {
        etIdCardNo.setText(idCard);
        ToastUtils.show("识别到"+type+":"+idCard);
    }

    @Override
    protected void onRecognizeCardFailure() {
        ToastUtils.show("扫描失败！");
    }

    @BindView(R.id.ll_content)
    LinearLayout llContent;
    void buildViewTree(){
        ApiConfig.Locate.Scan scan = Template.getCurrentTemplate().getApiConfig().getScan();

        Map<Integer, Set<String>> mBarcode = new HashMap<>();
        Map<Integer, Set<String>> mIdCard  = new HashMap<>();

        List<String> lstBarcode = scan.getLstBarcodeFields();
        for(String barcode : lstBarcode){
            if(!barcode.startsWith("Api")) continue;
            String index = barcode.substring(4,5);
            Integer type ;
            try { type = Integer.parseInt(index); } catch (Exception e) { continue; }
            int id1 = barcode.indexOf("\"")+1;
            if(id1<0) continue;
            int id2 = barcode.lastIndexOf("\"");
            if(id2<0) continue;
            if(id1==id2+1) continue;

            String key = barcode.substring(id1,id2);
            Set<String> set = mBarcode.get(type);
            if(set == null) set = new HashSet<>();
            set.add(key);
            mBarcode.put(type,set);
        }

        List<String> lstIdCard = scan.getLstIdCardIdFields();
        for(String idCardName : lstIdCard){
            if(!idCardName.startsWith("Api")) continue;
            String index = idCardName.substring(4,5);
            Integer type = null;
            try { type = Integer.parseInt(index); } catch (Exception e) { continue; }
            int id1 = idCardName.indexOf("\"")+1;
            if(id1<0) continue;
            int id2 = idCardName.lastIndexOf("\"");
            if(id2<0) continue;
            if(id1==id2+1) continue;

            String key = idCardName.substring(id1,id2);
            Set<String> set = mIdCard.get(type);
            if(set == null) set = new HashSet<>();
            set.add(key);
            mIdCard.put(type,set);
        }

        List<ApiParam> lst = Template.getCurrentTemplate().apiOf(Api.TYPE_NC_HISTORY_SEARCH).getRequest().getParams();
        for(int i=0,ni=lst.size();i<ni;++i){
            ApiParam p = lst.get(i);
            if(p == null) continue;
            String key = p.getName();
            if(key == null) continue;

            View v = null;
            EditText et = null;

            try {
                Set<String> sBarcode = mBarcode.get(Api.TYPE_NC_HISTORY_SEARCH);
                if(sBarcode!=null && sBarcode.contains(key)){
                    bEnableScanBarcode = true;
                    v = LayoutInflater.from(this).inflate(R.layout.view_input_barcode,null,false);
                    et = v.findViewById(R.id.ev_barcode);
                    etBarcode = et;
                    continue;
                }

                Set<String> sIdCard  = mIdCard.get(Api.TYPE_NC_HISTORY_SEARCH);
                if(sIdCard!=null && sIdCard.contains(key)){
                    bEnableScanIdCard = true;
                    v = LayoutInflater.from(this).inflate(R.layout.view_input_id_card,null,false);
                    et = v.findViewById(R.id.ev_recognize_id_card);
                    etIdCardNo = et;
                    continue;
                }

                String type = p.getType();
                if(type == null) continue;
                if(type.startsWith(ApiParam.TYPE_DATE)){
                    v = LayoutInflater.from(this).inflate(R.layout.view_input_date,null,false);
                    et = v.findViewById(R.id.ev_date);
                    TextView tvName = v.findViewById(R.id.tv_input_date);
                    tvName.setText(p.getDescription());
                    ViewInjector.injectDate(this, (CardView) v,type.substring(type.indexOf(":")+1));
                    continue;
                }

                v = LayoutInflater.from(this).inflate(R.layout.view_input,null,false);
                et = v.findViewById(R.id.ev_value);
                TextView tvName = v.findViewById(R.id.tv_name);
                tvName.setText(p.getDescription());
            } finally {
                if(v == null || et == null) continue;
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) DensityUtils.dp2px(40f));
                params.topMargin = (int) DensityUtils.dp2px(1);
                llContent.addView(v,params);
                String defaultValue = p.getDefaultValue();
                if(defaultValue!=null && !"null".equals(defaultValue)){
                    et.setText(defaultValue);
                    mPresenter.update(p.getName(),defaultValue);
                }
                et.clearFocus();
                et.setHint("请输入"+p.getDescription());
                et.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) { }

                    @Override
                    public void afterTextChanged(Editable s) {
                        mPresenter.update(p.getName(),s.toString());
                    }
                });
            }

        }
    }

    @OnClick(R.id.tv_search)
    void onSearchClicked(){
        NASearchingResultActivity.doSearch(this,mPresenter.eSearch);
    }
}
