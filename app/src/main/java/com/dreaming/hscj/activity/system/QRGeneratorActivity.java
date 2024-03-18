package com.dreaming.hscj.activity.system;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.util.Pair;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dreaming.hscj.App;
import com.dreaming.hscj.R;
import com.dreaming.hscj.activity.community.manage.CommunityMemberDetailActivity;
import com.dreaming.hscj.activity.community.search.CommunitySearchActivity;
import com.dreaming.hscj.activity.nucleic_acid.grouping.NAGroupingActivity;
import com.dreaming.hscj.base.BaseActivity;
import com.dreaming.hscj.core.EasyAdapter.EasyAdapter;
import com.dreaming.hscj.core.TTSEngine;
import com.dreaming.hscj.core.ThreadPoolProvider;
import com.dreaming.hscj.core.ocr.IDCardRecognizer;
import com.dreaming.hscj.dialog.DialogManager;
import com.dreaming.hscj.dialog.loading.LoadingDialog;
import com.dreaming.hscj.template.DataParser;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.utils.DensityUtils;
import com.dreaming.hscj.utils.EasyPermission;
import com.dreaming.hscj.utils.FileUtils;
import com.dreaming.hscj.utils.JsonUtils;
import com.dreaming.hscj.utils.ToastUtils;
import com.dreaming.hscj.utils.ZxingUtils;
import com.dreaming.hscj.widget.InputView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;
import priv.songxusheng.easydialog.EasyDialog;
import priv.songxusheng.easydialog.EasyDialogHolder;
import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

public class QRGeneratorActivity extends BaseActivity {

    @Override
    public int getContentViewResId() {
        return R.layout.activity_qr_generator;
    }

    @Override
    protected void onBindView(View vContent) {
        ButterKnife.bind(this,vContent);
    }

    @BindView(R.id.iv_img_width)
    InputView ivImgWidth;
    @BindView(R.id.iv_img_height)
    InputView ivImgHeight;
    @BindView(R.id.iv_img_h_num)
    InputView ivImgHNum;
    @BindView(R.id.iv_img_gap)
    InputView ivImgGap;

    @BindView(R.id.part_search)
    LinearLayout llPartSearch;
    @BindView(R.id.ev_id_no)
    EditText etIdNo;
    @BindView(R.id.part_add_mini)
    ConstraintLayout clPartAddMini;
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

    @BindView(R.id.iv_expansion)
    ImageView ivExpansion;

    int iKeyboardHeight = 0;
    int iGapHeight      = 0;
    OnKeyBoardLayoutStateChangeListener onKeyBoardLayoutStateChangeListener = new OnKeyBoardLayoutStateChangeListener() {
        @Override
        public void onKeyBoardShow(int keyBoardHeight, int gapHeight) {
            iKeyboardHeight = keyBoardHeight;
            iGapHeight      = gapHeight;
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) llPartSearch.getLayoutParams();
            params.bottomToTop = -1;
            params.bottomToBottom = 0;
            params.bottomMargin = keyBoardHeight - gapHeight + dp2px(4);
            llPartSearch.setLayoutParams(params);
            if(lstData.size()>0){
                clPartAddMini.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onKeyBoardHide() {
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) llPartSearch.getLayoutParams();
            params.bottomToTop = R.id.part_add;
            params.bottomToBottom = -1;
            params.bottomMargin = dp2px(20f);
            llPartSearch.setLayoutParams(params);
            clPartAddMini.setVisibility(View.INVISIBLE);
        }
    };

    @Override
    public void initView() {
        setCenterText("展示码打印生成");
        Toast.makeText(this,"默认宽高为A4纸张像素大小。\n\n非必要仅需对照A4纸修改生成码行个数！", Toast.LENGTH_LONG).show();
        EditText et = llPartSearch.findViewById(R.id.ev_id_no);
        et.setHint("请输入身份号后几位查询添加待生成人员");
        setOnKeyBoardLayoutStateChangeListener(onKeyBoardLayoutStateChangeListener);

        int visibility = View.VISIBLE;
        tvAddOcr      .setVisibility(visibility);
        tvAddSearching.setVisibility(visibility);
        tvAddCommunity.setVisibility(visibility);
        tvAddGrouping .setVisibility(visibility);

        llPartSearch.setVisibility(visibility);

        ivSamplingPlan.setVisibility(visibility);

        ivExpansion.setVisibility(View.INVISIBLE);
        ivExpansion.setRotation(visibility == View.INVISIBLE?360:180);

        tvPlanNum.setText(String.valueOf(lstData.size()));
        tvPlanNumMini.setText(tvPlanNum.getText().toString());
        if(tvPlanNum.getText().toString().equals("0")){
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
    
    List<ESONObject> lstData = new ArrayList<>();
    
    @OnClick(R.id.tv_search)
    void onSearchIdNoClicked(){
        String searchText = etIdNo.getText().toString().trim().toUpperCase();
        if(searchText.isEmpty()){
            ToastUtils.show("查询身份号不能为空");
            return;
        }
        hideKeyboard();
        App.PostDelayed(()->{
            MemberQueryActivity.doSelect(QRGeneratorActivity.this,1,0,new ESONObject().putValue(sId,searchText),1,new CommunitySearchActivity.ISelectCallback(){
                @Override
                public void onSelected(List<ESONObject> lstSelected) {
                    addPeopleToPlanSampling(lstSelected);
                }
            });
        },100);
    }

    @OnClick(R.id.tv_add_ocr)
    void onAddOcrClicked(){
        IDCardRecognizer.recognize(this, new IDCardRecognizer.IDCardRecognizeListener() {
            @Override
            public void onSuccess(String id, String type) {
                MemberQueryActivity.doSelect(QRGeneratorActivity.this,0,0,new ESONObject().putValue(sId,id),1,new CommunitySearchActivity.ISelectCallback(){
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
        CommunitySearchActivity.select(this, 0,new CommunitySearchActivity.ISelectCallback() {
            @Override
            public void onSelected(List<ESONObject> lstSelected) {
                addPeopleToPlanSampling(lstSelected);
            }
        });
    }

    @OnClick(R.id.tv_add_grouping)
    void onAddGroupingClicked(){
        NAGroupingActivity.doSelect(this,0,  new NAGroupingActivity.ISelectCallback() {
            @Override
            public void onSelected(List<ESONObject> lstSelected) {
                addPeopleToPlanSampling(lstSelected);
            }
        });
    }

    @OnClick(R.id.tv_add_community)
    void onAddCommunityClicked(){
        MemberQueryActivity.doSelect(this, 0, 0, new ESONObject(), 0, new CommunitySearchActivity.ISelectCallback() {
            @Override
            public void onSelected(List<ESONObject> lstSelected) {
                addPeopleToPlanSampling(lstSelected);
            }
        });
    }

    @OnLongClick(R.id.iv_sampling_plan)
    void onAddAllClicked(){
        DialogManager.showAlertDialog(
                this,
                "提示",
                "要添加所有的成员记录吗？",
                v -> {},
                v -> {
                    ESONArray datas = Template.getCurrentTemplate().getUserOverallDatabase().query(new ArrayList<>(),new ArrayList<>());
                    addPeopleToPlanSampling(JsonUtils.parseToList(datas));
                }
        );
    }


    void addPeopleToPlanSampling(List<ESONObject> lst){
        //Log.e(TAG,"onSelectResult->"+lst.size());
        List<ESONObject> lstSelected = lstData;
        int lastCount = lstSelected.size();
        for(int i=0,ni=lst.size();i<ni;++i){
            String idCardNo1 = lst.get(i).getJSONValue(sId,"");
            boolean contains = false;
            for(int j=0,nj=lstSelected.size();j<nj;++j){
                String idCardNo2 = lstSelected.get(j).getJSONValue(sId,"");
                if(idCardNo1.equals(idCardNo2)){
                    contains = true;
                    break;
                }
            }
            if(contains) {
                ToastUtils.show(this,idCardNo1+"重复选择！");
                continue;
            }
            lstSelected.add(lst.get(i));
        }
        int currCount = lstSelected.size();
        if(currCount - lastCount > 0){
            ToastUtils.show("成功添加"+(currCount - lastCount)+"条记录！");
        }
        tvPlanNum.setText(lstData.size()+"");
        tvPlanNumMini.setText(tvPlanNum.getText().toString());
        if(tvPlanNum.getText().toString().equals("0")){
            vPoint        .setVisibility(View.INVISIBLE);
            tvPlanNum     .setVisibility(View.INVISIBLE);
            tvSymbolAdd   .setVisibility(View.INVISIBLE);
        }
        else{
            vPoint        .setVisibility(View.VISIBLE);
            tvPlanNum     .setVisibility(View.VISIBLE);
            tvSymbolAdd   .setVisibility(View.VISIBLE);
        }
    }

    void showLoadDialog(){
        LoadingDialog.showDialog("GEN_QR",this);
    }

    void hideLoadDialog(){
        LoadingDialog.dismissDialog("GEN_QR");
    }

    @OnClick({R.id.iv_sampling_plan,R.id.part_add_mini})
    void onShowConvertPlanDialog(){
        if(lstData.size()==0){
            return;
        }
        hideKeyboard();
        Point p = DensityUtils.getScreenSize();
        new EasyDialog(R.layout.dialog_qr_generator_plan,this)
                .setOnBindDialogListener(new EasyDialog.OnBindDialogListener() {

                    @Override
                    public void onBindDialog(EasyDialogHolder holder) {
                        RecyclerView rv = holder.getView(R.id.rv_view_plan);
                        rv.setHasFixedSize(true);
                        rv.setLayoutManager(new LinearLayoutManager(QRGeneratorActivity.this));
                        rv.setAdapter(new EasyAdapter(QRGeneratorActivity.this, R.layout.recy_view_qr_generator_plan_item, lstData, (EasyAdapter.IEasyAdapter<ESONObject>) (holder1, data, position) -> {
                            holder1.setText(R.id.tv_index,String.valueOf(position+1));
                            String name = data.getJSONValue(sNm,"");
                            holder1.setText(R.id.tv_name,name);
                            holder1.setOnClickListener(R.id.tv_name,v -> {
                                if(name == null || name.trim().isEmpty()) return;
                                TTSEngine.speakChinese(name);
                            });
                            holder1.setText(R.id.tv_id_card_no,data.getJSONValue(sId,""));
                            holder1.setOnClickListener(R.id.tv_detail,v -> CommunityMemberDetailActivity.shownMember(QRGeneratorActivity.this,data.getJSONValue(sId,"")));
                            holder1.setOnClickListener(R.id.tv_remove,v -> {
                                DialogManager.showAlertDialog(QRGeneratorActivity.this,"提示","确定要从待生成记录中移除吗？",null, v1 -> {
                                    lstData.remove(position);
                                    rv.getAdapter().notifyItemRemoved(position);
                                    rv.getAdapter().notifyDataSetChanged();
                                });
                            });


                        }));
                        rv.invalidate();

                        holder.setOnClickListener(R.id.iv_close,v -> {
                            holder.dismissDialog();
                        });

                        holder.setOnClickListener(R.id.tv_start,v->{

                            requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, new EasyPermission.PermissionResultListener() {
                                @Override
                                public void onPermissionGranted() {
                                    DialogManager.showAlertDialog(QRGeneratorActivity.this,"提示","确定开始生成二维码图片吗？",null,v1 -> {
                                        int width  = 0;
                                        try { width = Integer.valueOf(ivImgWidth.tvValue.getText().toString()); } catch (Exception e) { }
                                        int height = 0;
                                        try { height = Integer.valueOf(ivImgHeight.tvValue.getText().toString()); } catch (Exception e) { }
                                        int gap = 0;
                                        try { gap = Integer.valueOf(ivImgGap.tvValue.getText().toString()); } catch (Exception e) { }
                                        int horizontalNum = 0;
                                        try { horizontalNum = Integer.valueOf(ivImgHNum.tvValue.getText().toString()); } catch (Exception e) { }
                                        doGenerate(width,height,gap,horizontalNum,lstData, FileUtils.getExternalDir());
                                    });
                                }

                                @Override
                                public void onPermissionDenied() {
                                    ToastUtils.show("无法获取读写权限，转换失败！");
                                }
                            });

                        });
                    }
                })
                .setForegroundResource(R.drawable.shape_dialog_corner_with_tl_tr)
                .setDialogParams(p.x,p.y- ((int)DensityUtils.dp2px(100f)), Gravity.BOTTOM)
                .showDialog();
    }
    
    String sId = Template.getCurrentTemplate().getIdCardNoFieldName();
    String sNm = Template.getCurrentTemplate().getIdCardNameFieldName();
    void doGenerate(int width, int height, int gap, int horizontalNum, List<ESONObject> lstData, String path){
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            try {
                App.Post(()->showLoadDialog());
                generateInternal(width, height, gap, horizontalNum, lstData, path);
            } catch (Exception e) {
                e.printStackTrace();
                App.Post(()->ToastUtils.show("处理失败！"));
            }
            finally {
                App.Post(()->hideLoadDialog());
            }
        });
    }
    void generateInternal(int width, int height, int gap, int horizontalNum, List<ESONObject> lstData, String path){
        if(lstData.size()<1){
            App.Post(()->ToastUtils.show("还没有选择待生成人员！"));
            return;
        }
        int hNum    = horizontalNum;
        if(hNum<1) {
            App.Post(()->ToastUtils.show("生成码行内个数非法！"));
            return;
        }
        int hPixel  = (width - (hNum+1) * gap)/hNum;
        if(hPixel<1) {
            App.Post(()->ToastUtils.show("生成码行内个数过大！"));
            return;
        }
        int vNum    = (height-gap) / (hPixel+gap);
        if(vNum<1) {
            App.Post(()->ToastUtils.show("生成码高度过小！"));
            return;
        }
        int hStart  = (width - hPixel*hNum - (hNum-1)*gap)/2;
        int vStart  = (height - hPixel*vNum - (vNum-1)*gap)/2;
        int pageNum = hNum * vNum;
        int picNum  = lstData.size()/pageNum + ((lstData.size()%pageNum)==0?0:1);

        float blankStartPercent = 130f/358f;
        float blankWidthPercent = 70f/358f;

        float fWidth = hPixel * blankWidthPercent;
        float fStart = (hPixel - fWidth)/2 ;
        float fEnd   = fStart + fWidth;

        List<Pair<File,Integer>> lstShown = new ArrayList<>();

        for(int i=0;i<picNum;++i){
            Bitmap bParent = Bitmap.createBitmap(width,height, Bitmap.Config.ARGB_8888);
            Canvas cParent = new Canvas(bParent);
            Paint  pParent = new Paint();
            pParent.setAntiAlias(true);
            pParent.setDither(true);
            pParent.setColor(Color.WHITE);
            cParent.drawRect(new Rect(0,0,width,height),pParent);

            pParent.setColor(Color.BLACK);

            for(int y=vStart,v=0;v<vNum;++v,y+=hPixel+gap){
                int idxY = i*pageNum + v*hNum;
                if(idxY >= lstData.size()) break;

                for(int x = hStart, h=0;h<hNum;++h,x+=hPixel+gap){
                    int idx = idxY + h;
                    if(idx >= lstData.size()) break;
                    ESONObject item = lstData.get(idx);
                    String id = item.getJSONValue(sId,"");
                    String nm = item.getJSONValue(sNm,"").trim();
                    nm = nm.substring(0,Math.min(4,nm.length()));

                    Bitmap bItem = ZxingUtils.autoCreateCodeBitmap(id,hPixel,hPixel,"utf-8",null,"0", Color.BLACK, Color.WHITE);
                    Canvas cItem = new Canvas(bItem);
                    Paint pItem = new Paint();
                    pItem.setAntiAlias(true);
                    pItem.setDither(true);

                    pItem.setColor(Color.WHITE);
                    pItem.setFakeBoldText(true);
                    pItem.setTextAlign(Paint.Align.LEFT);
                    cItem.drawRect(new RectF(fStart,fStart,fEnd,fEnd),pItem);
                    pItem.setColor(Color.BLACK);


                    float lTextSize = 1;
                    float rTextSize = 500;
                    float midTextSize;

                    while(rTextSize-lTextSize>0.1f){
                        midTextSize = lTextSize + (rTextSize-lTextSize)/2f;
                        pItem.setTextSize(midTextSize);
                        Rect bounds = new Rect();
                        pItem.getTextBounds("国", 0, 1, bounds);
                        float rw  = bounds.right - bounds.left;
                        float rmw = rw/2;
                        float neededWidth = rw*nm.length() + rmw*(nm.length()+1);
                        if(neededWidth>fWidth){
                            rTextSize = midTextSize;
                        }
                        else if(neededWidth < fWidth){
                            lTextSize = midTextSize;
                        }
                        else{
                            break;
                        }
                    }
                    pItem.setTextSize(lTextSize);

                    Rect bounds = new Rect();
                    pItem.getTextBounds("国", 0, 1, bounds);
                    Paint.FontMetricsInt fontMetrics = pItem.getFontMetricsInt();
                    float rw  = bounds.right - bounds.left;
                    float rmw = rw/2;

                    float fTextLength = rw * nm.length() + rmw * (nm.length()+1);
                    float rStart = fStart + rmw + (fWidth - fTextLength)/2;
                    float baseline = fStart + (fWidth - fontMetrics.bottom + fontMetrics.top) / 2 - fontMetrics.top;
                    for(int j=0,nj=nm.length();j<nj;++j){
                        cItem.drawText(nm.substring(j,j+1),rStart, baseline, pItem);
                        rStart += rw + rmw;
                    }

                    cItem.save();

                    cParent.drawBitmap(bItem,new Rect(0,0,hPixel,hPixel),new Rect(x,y,x+hPixel,y+hPixel),pParent);

                    cItem.restore();
                    bItem.recycle();
                }
            }

            cParent.save();
            try {
                File file=new File(path+"/"+System.currentTimeMillis()+".jpeg");
                FileOutputStream fos = new FileOutputStream(file);
                bParent.compress(Bitmap.CompressFormat.JPEG,100,fos);
                fos.close();
                lstShown.add(new Pair<>(file,Math.min(pageNum,lstData.size()-i*pageNum)));
            } catch (IOException e) {
                e.printStackTrace();
            }
            cParent.restore();
        }
        App.Post(()->showGenerateSuccessDialog(lstShown));
    }

    void showGenerateSuccessDialog(List<Pair<File,Integer>> lstShown){
        if(lstShown.size()==0){
            return;
        }
        hideKeyboard();
        Point p = DensityUtils.getScreenSize();
        new EasyDialog(R.layout.dialog_qr_generator_result,this)
                .setOnBindDialogListener(new EasyDialog.OnBindDialogListener() {

                    @Override
                    public void onBindDialog(EasyDialogHolder holder) {
                        RecyclerView rv = holder.getView(R.id.rv_view_plan);
                        rv.setHasFixedSize(true);
                        rv.setLayoutManager(new LinearLayoutManager(QRGeneratorActivity.this));
                        rv.setAdapter(new EasyAdapter(QRGeneratorActivity.this, R.layout.recy_view_qr_generator_result_item, lstShown, (EasyAdapter.IEasyAdapter<Pair<File,Integer>>) (holder1, data, position) -> {
                            holder1.setText(R.id.tv_index,String.valueOf(position+1));

                            holder1.setText(R.id.tv_path,data.first.getAbsolutePath());
                            holder1.setOnClickListener(R.id.tv_open,v -> {
                                try {
                                    Intent i = new Intent(Intent.ACTION_VIEW);
                                    i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    Uri uri= FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".fileProvider",data.first);
                                    i.setDataAndType(uri, "image/*");
                                    startActivity(i);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    ToastUtils.show("文件打开失败！");
                                }
                            });
                        }));
                        rv.invalidate();

                        holder.setOnClickListener(R.id.tv_done,v->{
                            finish();
                            holder.dismissDialog();
                        });
                    }
                })
                .setForegroundResource(R.drawable.shape_dialog_corner_with_tl_tr)
                .setAllowDismissWhenTouchOutside(false)
                .setAllowDismissWhenBackPressed(false)
                .setDialogParams(p.x,p.y- ((int)DensityUtils.dp2px(100f)), Gravity.BOTTOM)
                .showDialog();
    }
}
