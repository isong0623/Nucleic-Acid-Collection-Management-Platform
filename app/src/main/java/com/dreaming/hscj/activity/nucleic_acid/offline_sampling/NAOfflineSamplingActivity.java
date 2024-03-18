package com.dreaming.hscj.activity.nucleic_acid.offline_sampling;

import android.Manifest;
import android.graphics.Color;
import android.graphics.Point;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dreaming.hscj.Constants;
import com.dreaming.hscj.R;
import com.dreaming.hscj.activity.community.search.CommunitySearchActivity;
import com.dreaming.hscj.activity.nucleic_acid.grouping.NAGroupingActivity;
import com.dreaming.hscj.activity.system.MemberQueryActivity;
import com.dreaming.hscj.activity.system.ZxingActivity;
import com.dreaming.hscj.base.contract.BaseMVPActivity;
import com.dreaming.hscj.core.EasyAdapter.EasyAdapter;
import com.dreaming.hscj.core.EasyAdapter.EasyViewHolder;
import com.dreaming.hscj.core.TTSEngine;
import com.dreaming.hscj.core.ThreadPoolProvider;
import com.dreaming.hscj.core.ocr.IDCardRecognizer;
import com.dreaming.hscj.entity.OfflineExcel;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.utils.CheckUtils;
import com.dreaming.hscj.utils.DensityUtils;
import com.dreaming.hscj.utils.EasyPermission;
import com.dreaming.hscj.utils.FileUtils;
import com.dreaming.hscj.utils.ToastUtils;
import com.dreaming.hscj.widget.InputView;

import org.apache.xmlbeans.impl.piccolo.util.DuplicateKeyException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import priv.songxusheng.easydialog.EasyDialog;
import priv.songxusheng.easydialog.EasyDialogHolder;
import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

public class NAOfflineSamplingActivity extends BaseMVPActivity<NAOfflineSamplingPresenter> implements INAOfflineSamplingContract.View {
    //表格（二）（人工登记）新冠肺炎核酸20合一混采检测登记表

    @Override
    public int getContentViewResId() {
        return R.layout.activity_nucleic_acid_offline_sampling;
    }

    @Override
    protected void onBindView(View vContent) {
        ButterKnife.bind(this,vContent);
    }

    @BindView(R.id.tv_offline_file_name)
    TextView tvOfflineFileName;
    @BindView(R.id.tv_edit_offline_file)
    TextView tvOfflineFileEdit;
    @BindView(R.id.cv_input_barcode)
    CardView cvOfflineInputBarcode;
    @BindView(R.id.ev_barcode)
    EditText etBarcode;
    @BindView(R.id.c_offline_input)
    ConstraintLayout cOfflineInput;
    @BindView(R.id.iv_offline_input_name)
    InputView ivOfflineInputName;
    @BindView(R.id.iv_offline_input_id_no)
    InputView ivOfflineInputIdNo;
    @BindView(R.id.iv_offline_input_phone)
    InputView ivOfflineInputPhone;
    @BindView(R.id.tv_offline_save)
    TextView tvOfflineSave;
    @BindView(R.id.tv_offline_delete)
    TextView tvOfflineDelete;

    @BindView(R.id.cl_recommend_barcode)
    ConstraintLayout clRecommendBarcode;
    @BindView(R.id.cl_recommend_name)
    ConstraintLayout clRecommendName;
    @BindView(R.id.cl_recommend_id)
    ConstraintLayout clRecommendId;
    @BindView(R.id.cl_recommend_id_search)
    ConstraintLayout clRecommendIdSearch;

    @BindView(R.id.part_add_mini)
    ConstraintLayout clPartAddMini;
    @BindView(R.id.part_add)
    ConstraintLayout clPartAdd;

    @BindView(R.id.iv_expansion)
    ImageView ivExpansion;

    @BindView(R.id.tv_plan_num_mini)
    TextView tvPlanNumMini;

    @BindView(R.id.tv_plan_num)
    TextView tvPlanNum;
    @BindView(R.id.v_point)
    View vPoint;
    @BindView(R.id.iv_sampling_plan)
    ImageView ivSamplingPlan;
    @BindView(R.id.tv_symbol_add)
    TextView tvSymbolAdd;

    @BindView(R.id.tv_add_ocr)
    TextView tvAddOcr;
    @BindView(R.id.tv_add_searching)
    TextView tvAddSearching;
    @BindView(R.id.tv_add_grouping)
    TextView tvAddGrouping;
    @BindView(R.id.tv_add_community)
    TextView tvAddCommunity;

    @Override
    public void initView() {
        setCenterText("核酸离线采样");
        setOnKeyBoardLayoutStateChangeListener(onKeyBoardLayoutStateChangeListener);
        setRightText("上传");
        tvTitleRight.setOnClickListener(v->{
            final String path = mPresenter.getPath();
            try {
                mPresenter.close();
            } catch (Exception e) {
                e.printStackTrace();
                ToastUtils.show("文件保存失败！");
                finish();
                return;
            }
            NAOfflineSamplingUploadActivity.doUpload(this,mPresenter.getPath(),vv->{
                try {
                    mPresenter.setOfflineExcel(OfflineExcel.load(path));
                    setControlViewVisible(true);
                } catch (IOException e) {
                    e.printStackTrace();
                    ToastUtils.show("文件加载失败！");
                    finish();
                }
            });
        });
        ivOfflineInputName.tvValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            long lLastSearchTime = 0L;
            @Override
            public void afterTextChanged(Editable s) {
                if(s == null) return;
                if(s.toString().trim().length() == 0) return;
                long now = System.currentTimeMillis();
                if(now - 1000L<lLastSearchTime) return;
                lLastSearchTime = now;
                mPresenter.matchLocalName(s.toString().trim());
            }
        });
        ivOfflineInputName.tvValue.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus) clRecommendName.setVisibility(View.GONE);
            }
        });
        ivOfflineInputIdNo.tvValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
            long lLastSearchTime = 0L;
            @Override
            public void afterTextChanged(Editable s) {
                String str = s == null ? "" : s.toString().trim();
                if(str.length() == 0) return;
                if(str.length() == 18 && !CheckUtils.isValidIdCard(str)){
                    ToastUtils.show("身份号【"+str+"】不是一个有效的身份证！");
                }

                long now = System.currentTimeMillis();
                if(now - 1000L<lLastSearchTime) return;
                lLastSearchTime = now;
                mPresenter.matchLocalId(str);
            }
        });
        ivOfflineInputIdNo.tvValue.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus) clRecommendId.setVisibility(View.GONE);
            }
        });

        etBarcode.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus){
                    etBarcode.setText(mPresenter.getTubNo());
                }
            }
        });

        if(!hasPermision(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            ToastUtils.show("程序需要读写权限来操作离线文件，请授予权限！");
        }

        requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, new EasyPermission.PermissionResultListener() {
            @Override
            public void onPermissionGranted() {
                showSelectOfflineExcelFileDialog(true);
            }

            @Override
            public void onPermissionDenied() {
                ToastUtils.show("没有存储权限，无法操作离线文件！");
            }
        });

    }

    @Override
    protected boolean onKeyEnterDown() {
        if(etBarcode.hasFocus()){
            String barcode = etBarcode.getText()==null?"":etBarcode.getText().toString().trim();
            if(barcode.isEmpty()){
                ToastUtils.show("条码输入不能为空！");
                return true;
            }
            mPresenter.setTubNo(barcode);
            setInputVisible(true);
            setControlViewVisible(true);
            hideKeyboard();
            if(cOfflineInput.getVisibility() == View.VISIBLE) shownKeyboardDalayed(ivOfflineInputName.tvValue,500);
            return true;
        }
        if(ivOfflineInputName.tvValue.hasFocus()){
            if(ivOfflineInputName.tvValue.getText()==null || ivOfflineInputName.tvValue.getText().toString().trim().isEmpty()) return true;
            hideKeyboard();
            shownKeyboardDalayed(ivOfflineInputIdNo.tvValue,500);
            return true;
        }
        if(ivOfflineInputIdNo.tvValue.hasFocus()){
            if(ivOfflineInputIdNo.tvValue.getText()==null || ivOfflineInputIdNo.tvValue.getText().toString().trim().isEmpty()) return true;
            hideKeyboard();
            shownKeyboardDalayed(ivOfflineInputPhone.tvValue,500);
            return true;
        }
        if(ivOfflineInputPhone.tvValue.hasFocus()){
            if(ivOfflineInputPhone.tvValue.getText()==null || ivOfflineInputPhone.tvValue.getText().toString().trim().isEmpty()) return true;
            if(ivOfflineInputPhone.tvValue.getText().toString().trim().length() == 11){
                tvOfflineSave.callOnClick();
                return true;
            }
        }
        return super.onKeyEnterDown();
    }

    private void showSelectOfflineExcelFileDialog(boolean bShouldFinish){
        Point p = DensityUtils.getScreenSize();
        new EasyDialog(R.layout.dialog_offline_sampling_file_select,this)
                .setOnBindDialogListener(new EasyDialog.OnBindDialogListener() {
                    RecyclerView rvOfflineFileShown;
                    EasyAdapter adapter;
                    final List<File> lstDatas = new ArrayList<>();
                    TextView tvEmpty;
                    CardView cvTitle;
                    File fDir;
                    void updateFileList(){
                        lstDatas.clear();
                        if(fDir.exists()&&fDir.listFiles() != null) {
                            for(File f:fDir.listFiles()){
                                if(f.isFile()) lstDatas.add(f);
                            }
                        }

                        adapter.notifyDataSetChanged();
                        tvEmpty.setVisibility(lstDatas.isEmpty()?View.VISIBLE:View.GONE);
                        cvTitle.setVisibility(lstDatas.isEmpty()?View.GONE:View.VISIBLE);
                    }

                    @Override
                    public void onBindDialog(EasyDialogHolder easyDialogHolder) {
                        easyDialogHolder.setOnClickListener(R.id.iv_close,v -> {
                            easyDialogHolder.dismissDialog();
                            if(bShouldFinish) finish();
                        });
                        //external sd/hscj/offline_sampling/模板id/采样点/表格（二）（人工登记）新冠肺炎核酸20合一混采检测登记表——2022年04月12日15时12分12秒
                        ESONObject eCurrent = Constants.DBConfig.getSelectedDatabase();
                        fDir = new File(String.format("%s/hscj/offline_sampling/%s/%d/",
                                FileUtils.getExternalDir(),
                                Template.getCurrentTemplate().getDatabaseSetting().getSPUnify(),
                                eCurrent.getJSONValue("id",0)
                                ));

                        easyDialogHolder.setOnClickListener(R.id.tv_create, v -> NAOfflineSamplingEditActivity.createOfflineFile(NAOfflineSamplingActivity.this, fDir, new NAOfflineSamplingEditActivity.IOfflineEditCallback() {
                            @Override
                            public void onClosed(String newPath) {
                                updateFileList();
                            }
                        }));
                        cvTitle = easyDialogHolder.getView(R.id.part_recy_title);
                        tvEmpty = easyDialogHolder.getView(R.id.tv_data_empty);
                        rvOfflineFileShown = easyDialogHolder.getView(R.id.rv_view_offline_sampling);
                        rvOfflineFileShown.setLayoutManager(new LinearLayoutManager(NAOfflineSamplingActivity.this));
                        rvOfflineFileShown.setAdapter(adapter = new EasyAdapter(NAOfflineSamplingActivity.this, R.layout.recy_offline_file_select, lstDatas, new EasyAdapter.IEasyAdapter<File>() {
                            @Override
                            public void convert(EasyViewHolder holder, File data, int position) {
                                holder.setText(R.id.tv_file_name,data.getName());
                                holder.setOnClickListener(R.id.tv_edit,v -> NAOfflineSamplingEditActivity.editOfflineFile(NAOfflineSamplingActivity.this,data, new NAOfflineSamplingEditActivity.IOfflineEditCallback() {
                                    @Override
                                    public void onClosed(String newPath) {
                                       updateFileList();
                                    }
                                }));
                                holder.setOnClickListener(R.id.tv_select,v -> {
                                    try {
                                        OfflineExcel excel = OfflineExcel.load(data.getAbsolutePath());
                                        mPresenter.setOfflineExcel(excel);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        ToastUtils.show("离线文件读取失败！");
                                        return;
                                    }
                                    easyDialogHolder.dismissDialog();
                                });
                            }
                        },false));
                        updateFileList();
                    }
                })
                .setAllowDismissWhenTouchOutside(false)
                .setAllowDismissWhenBackPressed(false)
                .setDialogParams(p.x,p.y - dp2px(80), Gravity.BOTTOM)
                .showDialog();
    }

    private void setupRecommend(ConstraintLayout cl, List<ESONObject> lstMatched, List<String> lstShown){
        View vClose = cl.findViewById(R.id.iv_close);
        vClose.setOnClickListener(v -> cl.setVisibility(View.GONE));

        ScrollView sv = cl.findViewById(R.id.sv_main);
        LinearLayout ll = (LinearLayout) sv.getChildAt(0);
        ll.removeAllViews();

        if(lstShown.isEmpty() ||
                lstShown.size() ==1 &&
                        lstMatched.get(0).getJSONValue(mPresenter.sUserName,"").equals(ivOfflineInputName.tvValue.getText().toString().trim()) &&
                        lstMatched.get(0).getJSONValue(mPresenter.sIdCardNoName,"").equals(ivOfflineInputIdNo.tvValue.getText().toString().trim())

        ) {
            cl.setVisibility(View.GONE);
            return;
        }
        cl.setVisibility(View.VISIBLE);
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            for(int i=0,ni=lstShown.size();i<ni;++i){
                final int position = i;
                post(()->{
                    TextView tv = new TextView(this);
                    tv.setText(lstShown.get(position));
                    tv.setTextSize(TypedValue.COMPLEX_UNIT_SP,14);
                    tv.setTextColor(Color.parseColor("#000000"));
                    tv.setOnClickListener(v -> {
                        setInputValue(lstMatched.get(position));
                        cl.setVisibility(View.GONE);
                    });
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp2px(30));
                    params.gravity = Gravity.LEFT|Gravity.CENTER_VERTICAL;
                    ll.addView(tv,params);
                });
            }
        });
    }

    @Override
    public void autoShownMatchName(List<ESONObject> lstMatched, List<String> lstShown) {
        if(!ivOfflineInputName.hasFocus()) return;
        setupRecommend(clRecommendName,lstMatched,lstShown);
    }

    @Override
    public void autoShownMatchId(List<ESONObject> lstMatched, List<String> lstShown) {
        if(!ivOfflineInputIdNo.hasFocus()) return;
        setupRecommend(clRecommendId,lstMatched,lstShown);
    }

    @Override
    public void setOfflineFileName(String name){
        tvOfflineFileName.setText(name);
    }

    @Override
    protected boolean enableVolumeUpScanBarcode() {
        return true;
    }

    @Override
    protected void onScanBarcodeSuccess(String barcode) {
        mPresenter.setTubNo(barcode);
        etBarcode.setText(barcode);
        setControlViewVisible(true);
        clRecommendBarcode.setVisibility(View.GONE);
        setInputValue("","","");
        setControlViewVisible(true);
    }

    @Override
    protected void onScanBarcodeFailure() {
        ToastUtils.show("条码扫描失败！");
    }

    @Override
    protected boolean enableVolumeDownRecognizeIDCard() {
        return true;
    }

    @Override
    protected void onRecognizeCardSuccess(String idCard, String type) {
        setInputValue("",idCard,"");
        ToastUtils.show("识别到["+type+"]:"+idCard);
        setControlViewVisible(true);
    }

    @Override
    protected void onRecognizeCardFailure() {
        ToastUtils.show("识别身份号失败！");
    }

    @Override
    public void showBarcodeInputViewAndEnableKeyAct() {
        cvOfflineInputBarcode.setVisibility(View.VISIBLE);
        hideKeyboard();
        shownKeyboardDalayed(etBarcode,500);
    }

    private void setInputVisible(boolean visible){
        cOfflineInput.setVisibility(visible?View.VISIBLE:View.GONE);
    }

    void setControlViewVisible(boolean visible){
        int visibility = visible?View.VISIBLE:View.INVISIBLE;
        clPartAdd.setVisibility(visibility);
        clPartAddMini.setVisibility(visibility);
        if(mPresenter.getOfflineInput() >= mPresenter.maxNum){
            tvAddOcr      .setVisibility(View.INVISIBLE);
            tvAddSearching.setVisibility(View.INVISIBLE);
            tvAddCommunity.setVisibility(View.INVISIBLE);
            tvAddGrouping .setVisibility(View.INVISIBLE);
        }
        else{
            tvAddOcr      .setVisibility(visibility);
            tvAddSearching.setVisibility(visibility);
            tvAddCommunity.setVisibility(visibility);
            tvAddGrouping .setVisibility(visibility);
            setInputVisible(true);
        }

        ivExpansion.setVisibility(visibility);
        ivSamplingPlan.setVisibility(visibility);

        ivExpansion.setRotation(visibility == View.INVISIBLE?360:180);

        tvPlanNum.setText(String.valueOf(mPresenter.getOfflineInput()));
        tvPlanNumMini.setText(tvPlanNum.getText().toString());
        if(visible && tvPlanNum.getText().toString().equals("0")){
            vPoint        .setVisibility(View.INVISIBLE);
            tvPlanNum     .setVisibility(View.INVISIBLE);
            tvSymbolAdd   .setVisibility(View.INVISIBLE);
        }
        else{
            vPoint        .setVisibility(visibility);
            tvPlanNum     .setVisibility(visibility);
            tvSymbolAdd   .setVisibility(visibility);
        }
    }

    void setInputValue(ESONObject item){
        String name = item.getJSONValue(mPresenter.sUserName,"");
        String id   = item.getJSONValue(mPresenter.sIdCardNoName,"");
        String phone= item.getJSONValue(mPresenter.sPhone,"");
        setInputValue(name,id,phone);
    }

    void setInputValue(String name,String id,String phone){
        if(name == null) name = "";
        if(id   == null) id   = "";
        if(phone== null) phone= "";
        ivOfflineInputName.tvValue.setText(name);
        ivOfflineInputIdNo.tvValue.setText(id);
        ivOfflineInputPhone.tvValue.setText(phone);
    }

    void addPeopleToPlanSampling(List<ESONObject> lst){
        if(lst == null) return;
        int lastCount = mPresenter.getOfflineInput();
        for(ESONObject item : lst){
            String name = item.getJSONValue(mPresenter.sUserName,"");
            String id   = item.getJSONValue(mPresenter.sIdCardNoName,"");
            String phone= item.getJSONValue(mPresenter.sPhone,"");
            if(phone.trim().isEmpty()){
                List<String> lstK = new ArrayList<>(); lstK.add(mPresenter.sIdCardNoName);
                List<Object> lstV = new ArrayList<>(); lstV.add(id);
                ESONArray datas = Template.getCurrentTemplate().getUserOverallDatabase().query(lstK,lstV);
                if(datas.length()>0){
                    phone = datas.getArrayValue(0,new ESONObject()).getJSONValue(mPresenter.sPhone,"");
                }
            }
            try {
                mPresenter.write(mPresenter.getTubNo(),id,name,phone);
                TTSEngine.speakSync(name+"，"+mPresenter.getOfflineInput()+"。");
                setInputValue("","","");
            } catch (IOException e) {
                e.printStackTrace();
                ToastUtils.show(e.getMessage());
                setInputValue(name,id,phone);
            } catch (DuplicateKeyException e) {
                e.printStackTrace();
                ToastUtils.show(e.getMessage());
                setInputValue(name,id,phone);
            }
            catch (Exception e){
                ToastUtils.show(e.getMessage());
                setInputValue(name,id,phone);
            }
        }
        int currCount = mPresenter.getOfflineInput();
        if(currCount - lastCount > 0){
            ToastUtils.show("成功添加"+(currCount - lastCount)+"条记录！");
        }

        setControlViewVisible(true);
    }

    @OnClick(R.id.tv_select_barcode)
    void onSelectBarcodeClicked(){
        List<String> lst = mPresenter.getOfflineTubNoList();

        if(lst == null || lst.isEmpty()){
            ToastUtils.show("还没有采集过任何信息！");
            return;
        }

        setupRecommend(clRecommendBarcode,new ArrayList<>(),new ArrayList<>());

        ScrollView sv = clRecommendBarcode.findViewById(R.id.sv_main);
        LinearLayout ll = (LinearLayout) sv.getChildAt(0);
        clRecommendBarcode.setVisibility(View.VISIBLE);

        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            for(int i=0,ni=lst.size();i<ni;++i){
                final int position = i;
                post(()->{
                    TextView tv = new TextView(this);
                    tv.setText(lst.get(position));
                    tv.setTextSize(TypedValue.COMPLEX_UNIT_SP,14);
                    tv.setTextColor(Color.parseColor("#000000"));
                    tv.setOnClickListener(v -> {
                        String tubNo = lst.get(position).split(" ")[0];
                        mPresenter.setTubNo(tubNo);
                        setControlViewVisible(true);
                        etBarcode.setText(tubNo);
                        clRecommendBarcode.setVisibility(View.GONE);
                        setInputValue("","","");
                        hideKeyboard();
                        shownKeyboardDalayed(ivOfflineInputName.tvValue,500);
                    });
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp2px(30));
                    params.gravity = Gravity.LEFT|Gravity.CENTER_VERTICAL;
                    ll.addView(tv,params);
                });
            }
        });
    }

    @OnClick(R.id.tv_scan_barcode)
    void onScanBarcodeClicked(){
        ZxingActivity.Scan(this, new ZxingActivity.IScanListener() {
            @Override
            public void onSuccess(String code) {
                onScanBarcodeSuccess(code);
            }

            @Override
            public void onFailure() {
                onScanBarcodeFailure();
            }
        });
    }

    @OnClick(R.id.tv_edit_offline_file)
    void onOfflineFileEditClicked(){
        final File fOld = new File(mPresenter.getPath());
        try {
            mPresenter.close();
        } catch (Exception e) {
            e.printStackTrace();
            ToastUtils.show("文件关闭失败，请参照此文件目录下的backup备份文件夹下的文件进行补正和修复！");
            return;
        }

        NAOfflineSamplingEditActivity.editOfflineFile(this, fOld, new NAOfflineSamplingEditActivity.IOfflineEditCallback() {
            @Override
            public void onClosed(String newPath) {
                if(newPath.isEmpty()){
                    showSelectOfflineExcelFileDialog(true);
                    return;
                }

                File fNew = new File(newPath);
                if(fOld.getName().equals(fNew)) return;
                try {
                    mPresenter.setOfflineExcel(OfflineExcel.load(newPath));
                    tvOfflineFileName.setText(fNew.getName());
                    setControlViewVisible(true);
                } catch (IOException e) {
                    e.printStackTrace();
                    ToastUtils.show("重新载入文件失败！");
                }
            }
        });
    }

    @OnClick(R.id.tv_offline_save)
    void onOfflineItemSaveClicked(){
        String name = ivOfflineInputName .tvValue.getText().toString().trim();
        String id   = ivOfflineInputIdNo .tvValue.getText().toString().trim();
        String phone= ivOfflineInputPhone.tvValue.getText().toString().trim();
        List<ESONObject> lst = new ArrayList<>();
        ESONObject e = new ESONObject()
                .putValue(mPresenter.sUserName    , name )
                .putValue(mPresenter.sIdCardNoName, id   )
                .putValue(mPresenter.sPhone       , phone);
        lst.add(e);
        addPeopleToPlanSampling(lst);
        if(cOfflineInput.getVisibility() == View.VISIBLE) showKeyboard(ivOfflineInputName.tvValue);
    }

    @OnClick(R.id.tv_offline_delete)
    void onOfflineItemDeleteClicked(){
        String id = ivOfflineInputIdNo.tvValue.getText().toString().trim();
        try {
            mPresenter.delete(mPresenter.getTubNo(),id);
            setInputValue("","","");
            ToastUtils.show("删除成功！");
        } catch (Exception e) {
            e.printStackTrace();
            ToastUtils.show(e.getMessage());
        }
    }

    int iKeyboardHeight = 0;
    int iGapHeight      = 0;
    OnKeyBoardLayoutStateChangeListener onKeyBoardLayoutStateChangeListener = new OnKeyBoardLayoutStateChangeListener() {
        @Override
        public void onKeyBoardShow(int keyBoardHeight, int gapHeight) {
            iKeyboardHeight = keyBoardHeight;
            iGapHeight      = gapHeight;
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) clPartAddMini.getLayoutParams();
            params.bottomToTop = -1;
            params.bottomToBottom = 0;
            params.bottomMargin = keyBoardHeight - gapHeight + dp2px(4);
            clPartAddMini.setLayoutParams(params);
            if(mPresenter.getOfflineInput()>0){
                clPartAddMini.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onKeyBoardHide() {
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) clPartAddMini.getLayoutParams();
            params.bottomToTop = R.id.part_add;
            params.bottomToBottom = -1;
            params.bottomMargin = dp2px(20f);
            clPartAddMini.setLayoutParams(params);
            clPartAddMini.setVisibility(View.INVISIBLE);
            clRecommendId.setVisibility(View.GONE);
            clRecommendName.setVisibility(View.GONE);
        }
    };

    @OnClick(R.id.tv_add_ocr)
    void onAddOcrClicked(){
        IDCardRecognizer.recognize(this, new IDCardRecognizer.IDCardRecognizeListener() {
            @Override
            public void onSuccess(String id, String type) {
                MemberQueryActivity.doSelect(NAOfflineSamplingActivity.this,0,0,new ESONObject().putValue(mPresenter.sIdCardNoName,id),1,new CommunitySearchActivity.ISelectCallback(){
                    @Override
                    public void onSelected(List<ESONObject> lstSelected) {
                        addPeopleToPlanSampling(lstSelected);
                    }
                });
            }

            @Override
            public void onFailure() {
                ToastUtils.show("识别失败！");
            }
        });

    }

    @OnClick(R.id.tv_add_searching)
    void onAddSearchingClicked(){
        CommunitySearchActivity.select(this, mPresenter.maxNum - mPresenter.getOfflineInput(), new CommunitySearchActivity.ISelectCallback() {
            @Override
            public void onSelected(List<ESONObject> lstSelected) {
                addPeopleToPlanSampling(lstSelected);
            }
        });
    }

    @OnClick(R.id.tv_add_grouping)
    void onAddGroupingClicked(){
        NAGroupingActivity.doSelect(this, mPresenter.maxNum - mPresenter.getOfflineInput(), new NAGroupingActivity.ISelectCallback() {
            @Override
            public void onSelected(List<ESONObject> lstSelected) {
                addPeopleToPlanSampling(lstSelected);
            }
        });
    }

    @OnClick(R.id.tv_add_community)
    void onAddCommunityClicked(){
        MemberQueryActivity.doSelect(this, 0, 0, new ESONObject(), mPresenter.maxNum - mPresenter.getOfflineInput(), new CommunitySearchActivity.ISelectCallback() {
            @Override
            public void onSelected(List<ESONObject> lstSelected) {
                addPeopleToPlanSampling(lstSelected);
            }
        });
    }

    @OnClick(R.id.iv_expansion)
    void onExpansionClicked(){
        Object tag = ivExpansion.getTag();
        int visibility = tag == null || ((tag instanceof Boolean || tag.getClass().equals(boolean.class)) && !((boolean) tag)) ? View.INVISIBLE : View.VISIBLE;
        ivExpansion.setTag(visibility == View.INVISIBLE);
        ivExpansion.setRotation(visibility == View.INVISIBLE?360:180);
        tvAddOcr      .setVisibility(visibility);
        tvAddSearching.setVisibility(visibility);
        tvAddCommunity.setVisibility(visibility);
        tvAddGrouping .setVisibility(visibility);
        ivSamplingPlan.setVisibility(visibility);

        tvPlanNum.setText(String.valueOf(mPresenter.getOfflineInput()));
        tvPlanNumMini.setText(tvPlanNum.getText().toString());
        if(visibility == View.VISIBLE && tvPlanNum.getText().toString().equals("0")){
            vPoint        .setVisibility(View.INVISIBLE);
            tvPlanNum     .setVisibility(View.INVISIBLE);
            tvSymbolAdd   .setVisibility(View.INVISIBLE);
        }
        else{
            vPoint        .setVisibility(visibility);
            tvPlanNum     .setVisibility(visibility);
            tvSymbolAdd   .setVisibility(visibility);
        }
    }

    @OnClick({R.id.part_add_mini,R.id.iv_sampling_plan})
    void onShowSamplingListClicked(){
        if(mPresenter.getOfflineInput()<1) return;
        List<ESONObject> lstData = mPresenter.getOfflineViewList();
        if(lstData == null || lstData.isEmpty()) return;
        hideKeyboard();
        Point p = DensityUtils.getScreenSize();
        new EasyDialog(R.layout.dialog_offline_sampling_view,this)
                .setOnBindDialogListener(new EasyDialog.OnBindDialogListener() {
                    @Override
                    public void onBindDialog(EasyDialogHolder easyDialogHolder) {
                        easyDialogHolder.setText(R.id.tv_title,String.format("离线记录：%s",mPresenter.getTubNo()));
                        easyDialogHolder.setOnClickListener(R.id.iv_close,v -> easyDialogHolder.dismissDialog());
                        RecyclerView rv = easyDialogHolder.getView(R.id.rv_view_offline_sampling);
                        rv.setLayoutManager(new LinearLayoutManager(NAOfflineSamplingActivity.this));
                        rv.setAdapter(new EasyAdapter(NAOfflineSamplingActivity.this, R.layout.recy_view_offline_sampling_item, lstData, new EasyAdapter.IEasyAdapter<ESONObject>() {
                            @Override
                            public void convert(EasyViewHolder holder, ESONObject data, int position) {
                                holder.setText(R.id.tv_name      , data.getJSONValue("name" ,""));
                                holder.setText(R.id.tv_id_card_no, data.getJSONValue("id"   ,""));
                                holder.setText(R.id.tv_phone     , data.getJSONValue("phone",""));
                                holder.setOnClickListener(R.id.tv_edit,v->{
                                    setInputValue(data);
                                    easyDialogHolder.dismissDialog();
                                });
                            }
                        }));
                    }
                })
                .setDialogParams(p.x,p.y - dp2px(80),Gravity.BOTTOM)
                .showDialog();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            mPresenter.close();
        } catch (Exception e) {
            e.printStackTrace();
            ToastUtils.show("文件关闭失败，请参照此文件目录下的backup备份文件夹下的文件进行补正和修复！\n\n"+e.getMessage());
        }
    }
}
