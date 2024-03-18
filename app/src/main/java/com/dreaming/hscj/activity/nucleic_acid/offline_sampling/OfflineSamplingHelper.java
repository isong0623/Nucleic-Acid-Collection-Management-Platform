package com.dreaming.hscj.activity.nucleic_acid.offline_sampling;

import android.graphics.Color;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dreaming.hscj.App;
import com.dreaming.hscj.R;
import com.dreaming.hscj.base.BaseActivity;
import com.dreaming.hscj.core.ThreadPoolProvider;
import com.dreaming.hscj.utils.DensityUtils;

import java.util.List;
import java.util.Map;

import priv.songxusheng.easyjson.ESONObject;

public class OfflineSamplingHelper {

    public interface OnEditClickListener{
        void onClicked(LinearLayout parent, LinearLayout child, TextView tvName, TextView tvId, TextView tvPhone, ESONObject item);
    }
    public static LinearLayout buildOfflineViewListWithEdit(BaseActivity activity, Map<String,List<ESONObject>> map, OnEditClickListener listener){
        LinearLayout llParent = new LinearLayout(activity);
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            for (Map.Entry<String, List<ESONObject>> entry : map.entrySet()) {
                App.Post(()->llParent.addView(buildOfflineViewListWithEdit(activity,entry.getKey(),entry.getValue(),listener), new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT)));
            }
        });
        return llParent;
    }
    private static LinearLayout buildOfflineViewListWithEdit(BaseActivity activity, String tubNo, List<ESONObject> lst, OnEditClickListener listener){
        LinearLayout ll = new LinearLayout(activity);
        ll.setOrientation(LinearLayout.VERTICAL);

        App.Post(()->{
            LinearLayout llTitle = new LinearLayout(activity);

            TextView tvTub = new TextView(activity);
            tvTub.setText(tubNo+"   "+lst.size()+"人");
            tvTub.setTextColor(Color.parseColor("#000000"));
            tvTub.getPaint().setFakeBoldText(true);
            tvTub.setTextSize(TypedValue.COMPLEX_UNIT_SP,16);
            LinearLayout.LayoutParams pTub = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,(int) DensityUtils.dp2px(32f));
            pTub.weight = 1;
            pTub.gravity= Gravity.CENTER_VERTICAL | Gravity.LEFT;
            llTitle.addView(tvTub,pTub);


            ImageView ivMore = new ImageView(activity);
            ivMore.setImageResource(R.drawable.ic_offline_more);
            ivMore.setRotation(90);
            ivMore.setColorFilter(Color.parseColor("#bdbdbd"));
            LinearLayout.LayoutParams pMore = new LinearLayout.LayoutParams((int)DensityUtils.dp2px(28f),(int)DensityUtils.dp2px(28f));
            pMore.gravity = Gravity.CENTER;
            pMore.leftMargin  = activity.dp2px(4);
            pMore.rightMargin = activity.dp2px(4);
            llTitle.addView(ivMore,pMore);

            llTitle.setOnClickListener(v -> {
                boolean visible = ll.getChildCount()>1 && ll.getChildAt(1).getVisibility() == View.VISIBLE;
                if(!visible){
                    if(ll.getChildCount()<2){
                        LinearLayout llContainer = new LinearLayout(activity);
                        llContainer.setOrientation(LinearLayout.VERTICAL);
                        ll.addView(llContainer,LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        ThreadPoolProvider.getFixedThreadPool().execute(()->{
                            for(ESONObject item : lst){
                                String name = item.getJSONValue("name" ,"").trim();
                                String idNo = item.getJSONValue("id"   ,"").trim();
                                String phone= item.getJSONValue("phone","").trim();
                                App.Post(()->{
                                    LinearLayout llChild = new LinearLayout(activity);
                                    llChild.setOrientation(LinearLayout.HORIZONTAL);
                                    TextView tvLabel = new TextView(activity);
                                    tvLabel.setText(llContainer.getChildCount() == lst.size()-1?"└─":"├─");
                                    llChild.addView(tvLabel,LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.MATCH_PARENT);

                                    TextView tvName = new TextView(activity);
                                    tvName.setText(name);
                                    LinearLayout.LayoutParams pName = new LinearLayout.LayoutParams((int) DensityUtils.dp2px(60f), LinearLayout.LayoutParams.MATCH_PARENT);
                                    pName.weight = 1;
                                    pName.gravity = Gravity.CENTER;
                                    llChild.addView(tvName,pName);


                                    TextView tvId = new TextView(activity);
                                    tvId.setText(idNo);
                                    LinearLayout.LayoutParams pId = new LinearLayout.LayoutParams((int) DensityUtils.dp2px(180f), LinearLayout.LayoutParams.MATCH_PARENT);
                                    pId.weight = 3;
                                    pId.gravity = Gravity.CENTER;
                                    llChild.addView(tvId,pId);

                                    TextView tvPhone = new TextView(activity);
                                    tvPhone.setText(phone);
                                    LinearLayout.LayoutParams pPhone = new LinearLayout.LayoutParams((int) DensityUtils.dp2px(100f), LinearLayout.LayoutParams.MATCH_PARENT);
                                    pPhone.weight = 1;
                                    pPhone.gravity = Gravity.CENTER;
                                    llChild.addView(tvPhone,pPhone);

                                    TextView tvEdit = new TextView(activity);
                                    tvEdit.setText("编辑");
                                    tvEdit.setGravity(Gravity.CENTER);
                                    tvEdit.setTextColor(Color.WHITE);
                                    tvEdit.setBackgroundResource(R.drawable.shape_donate_bg);
                                    tvEdit.setOnClickListener(vv->listener.onClicked(llContainer,llChild,tvName,tvId,tvPhone,item));
                                    LinearLayout.LayoutParams pEdit = new LinearLayout.LayoutParams((int) DensityUtils.dp2px(46), (int) DensityUtils.dp2px(26));
                                    pEdit.gravity = Gravity.CENTER;
                                    pEdit.rightMargin = (int) DensityUtils.dp2px(4);
                                    llChild.addView(tvEdit,pEdit);

                                    llContainer.addView(llChild,LinearLayout.LayoutParams.MATCH_PARENT,(int) DensityUtils.dp2px(30f));
                                });
                            }
                        });
                    }
                }
                ll.getChildAt(1).setVisibility(visible?View.GONE:View.VISIBLE);
                ivMore.setRotation(visible?90:270);
            });

            ll.addView(llTitle,LinearLayout.LayoutParams.MATCH_PARENT,(int) DensityUtils.dp2px(32f));
        });

        return ll;
    }

    public interface OnDeleteClickListener{
        void onClicked(LinearLayout parent, LinearLayout child, String tubNo, String id);
    }
    public static LinearLayout buildOfflineViewListWithDelete(BaseActivity activity, Map<String,List<ESONObject>> map, OnDeleteClickListener listener){
        LinearLayout llParent = new LinearLayout(activity);
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            for (Map.Entry<String, List<ESONObject>> entry : map.entrySet()) {
                App.Post(()->llParent.addView(buildOfflineViewListWithDelete(activity,entry.getKey(),entry.getValue(),listener), new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT)));
            }
        });
        return llParent;
    }
    private static LinearLayout buildOfflineViewListWithDelete(BaseActivity activity, String tubNo, List<ESONObject> lst, OnDeleteClickListener listener){
        LinearLayout ll = new LinearLayout(activity);
        ll.setOrientation(LinearLayout.VERTICAL);

        App.Post(()->{
            LinearLayout llTitle = new LinearLayout(activity);

            TextView tvTub = new TextView(activity);
            tvTub.setText(tubNo+"   "+lst.size()+"人");
            tvTub.setTextColor(Color.parseColor("#000000"));
            tvTub.getPaint().setFakeBoldText(true);
            tvTub.setTextSize(TypedValue.COMPLEX_UNIT_SP,16);
            LinearLayout.LayoutParams pTub = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,(int) DensityUtils.dp2px(32f));
            pTub.weight = 1;
            pTub.gravity= Gravity.CENTER_VERTICAL | Gravity.LEFT;
            llTitle.addView(tvTub,pTub);


            ImageView ivMore = new ImageView(activity);
            ivMore.setImageResource(R.drawable.ic_offline_more);
            ivMore.setRotation(90);
            ivMore.setColorFilter(Color.parseColor("#bdbdbd"));
            LinearLayout.LayoutParams pMore = new LinearLayout.LayoutParams((int)DensityUtils.dp2px(28f),(int)DensityUtils.dp2px(28f));
            pMore.gravity = Gravity.CENTER;
            pMore.leftMargin  = activity.dp2px(4);
            pMore.rightMargin = activity.dp2px(4);
            llTitle.addView(ivMore,pMore);

            llTitle.setOnClickListener(v -> {
                boolean visible = ll.getChildCount()>1 && ll.getChildAt(1).getVisibility() == View.VISIBLE;
                if(!visible){
                    if(ll.getChildCount()<2){
                        LinearLayout llContainer = new LinearLayout(activity);
                        llContainer.setOrientation(LinearLayout.VERTICAL);
                        ll.addView(llContainer,LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        ThreadPoolProvider.getFixedThreadPool().execute(()->{
                            for(ESONObject item : lst){
                                String name = item.getJSONValue("name" ,"").trim();
                                String idNo = item.getJSONValue("id"   ,"").trim();
                                String phone= item.getJSONValue("phone","").trim();
                                App.Post(()->{
                                    LinearLayout llChild = new LinearLayout(activity);
                                    llChild.setOrientation(LinearLayout.HORIZONTAL);
                                    TextView tvLabel = new TextView(activity);
                                    tvLabel.setText(llContainer.getChildCount() == lst.size()-1?"└─":"├─");
                                    llChild.addView(tvLabel,LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.MATCH_PARENT);

                                    TextView tvName = new TextView(activity);
                                    tvName.setText(name);
                                    LinearLayout.LayoutParams pName = new LinearLayout.LayoutParams((int) DensityUtils.dp2px(60f), LinearLayout.LayoutParams.MATCH_PARENT);
                                    pName.weight = 1;
                                    pName.gravity = Gravity.CENTER;
                                    llChild.addView(tvName,pName);


                                    TextView tvId = new TextView(activity);
                                    tvId.setText(idNo);
                                    LinearLayout.LayoutParams pId = new LinearLayout.LayoutParams((int) DensityUtils.dp2px(180f), LinearLayout.LayoutParams.MATCH_PARENT);
                                    pId.weight = 3;
                                    pId.gravity = Gravity.CENTER;
                                    llChild.addView(tvId,pId);

                                    TextView tvPhone = new TextView(activity);
                                    tvPhone.setText(phone);
                                    LinearLayout.LayoutParams pPhone = new LinearLayout.LayoutParams((int) DensityUtils.dp2px(100f), LinearLayout.LayoutParams.MATCH_PARENT);
                                    pPhone.weight = 1;
                                    pPhone.gravity = Gravity.CENTER;
                                    llChild.addView(tvPhone,pPhone);

                                    TextView tvDelete = new TextView(activity);
                                    tvDelete.setText("删除");
                                    tvDelete.setGravity(Gravity.CENTER);
                                    tvDelete.setTextColor(Color.WHITE);
                                    tvDelete.setBackgroundResource(R.drawable.shape_btn_delete);
                                    tvDelete.setOnClickListener(vv->listener.onClicked(llContainer,llChild,tubNo,idNo));
                                    LinearLayout.LayoutParams pDelete = new LinearLayout.LayoutParams((int) DensityUtils.dp2px(46), (int) DensityUtils.dp2px(26));
                                    pDelete.gravity = Gravity.CENTER;
                                    pDelete.rightMargin = (int) DensityUtils.dp2px(4);
                                    llChild.addView(tvDelete,pDelete);

                                    llContainer.addView(llChild,LinearLayout.LayoutParams.MATCH_PARENT,(int) DensityUtils.dp2px(30f));
                                });
                            }
                        });
                    }
                }
                ll.getChildAt(1).setVisibility(visible?View.GONE:View.VISIBLE);
                ivMore.setRotation(visible?90:270);
            });

            ll.addView(llTitle,LinearLayout.LayoutParams.MATCH_PARENT,(int) DensityUtils.dp2px(32f));
        });

        return ll;
    }

    public static LinearLayout buildOfflineViewList(BaseActivity activity, Map<String,List<ESONObject>> map){
        LinearLayout llParent = new LinearLayout(activity);
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            for (Map.Entry<String, List<ESONObject>> entry : map.entrySet()) {
                App.Post(()->llParent.addView(buildOfflineViewList(activity,entry.getKey(),entry.getValue()), new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT)));
            }
        });
        return llParent;
    }
    private static LinearLayout buildOfflineViewList(BaseActivity activity, String tubNo, List<ESONObject> lst){
        LinearLayout ll = new LinearLayout(activity);
        ll.setOrientation(LinearLayout.VERTICAL);

        App.Post(()->{
            LinearLayout llTitle = new LinearLayout(activity);

            TextView tvTub = new TextView(activity);
            tvTub.setText(tubNo+"   "+lst.size()+"人");
            tvTub.setTextColor(Color.parseColor("#000000"));
            tvTub.getPaint().setFakeBoldText(true);
            tvTub.setTextSize(TypedValue.COMPLEX_UNIT_SP,16);
            LinearLayout.LayoutParams pTub = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,(int) DensityUtils.dp2px(32f));
            pTub.weight = 1;
            pTub.gravity= Gravity.CENTER_VERTICAL | Gravity.LEFT;
            llTitle.addView(tvTub,pTub);


            ImageView ivMore = new ImageView(activity);
            ivMore.setImageResource(R.drawable.ic_offline_more);
            ivMore.setRotation(90);
            ivMore.setColorFilter(Color.parseColor("#bdbdbd"));
            LinearLayout.LayoutParams pMore = new LinearLayout.LayoutParams((int)DensityUtils.dp2px(28f),(int)DensityUtils.dp2px(28f));
            pMore.gravity = Gravity.CENTER;
            pMore.leftMargin  = activity.dp2px(4);
            pMore.rightMargin = activity.dp2px(4);
            llTitle.addView(ivMore,pMore);

            llTitle.setOnClickListener(v -> {
                boolean visible = ll.getChildCount()>1 && ll.getChildAt(1).getVisibility() == View.VISIBLE;
                if(!visible){
                    if(ll.getChildCount()<2){
                        LinearLayout llContainer = new LinearLayout(activity);
                        llContainer.setOrientation(LinearLayout.VERTICAL);
                        ll.addView(llContainer,LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        ThreadPoolProvider.getFixedThreadPool().execute(()->{
                            for(ESONObject item : lst){
                                String name = item.getJSONValue("name" ,"").trim();
                                String idNo = item.getJSONValue("id"   ,"").trim();
                                String phone= item.getJSONValue("phone","").trim();
                                App.Post(()->{
                                    LinearLayout llChild = new LinearLayout(activity);
                                    llChild.setOrientation(LinearLayout.HORIZONTAL);
                                    TextView tvLabel = new TextView(activity);
                                    tvLabel.setText(llContainer.getChildCount() == lst.size()-1?"└─":"├─");
                                    llChild.addView(tvLabel,LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.MATCH_PARENT);

                                    TextView tvName = new TextView(activity);
                                    tvName.setText(name);
                                    LinearLayout.LayoutParams pName = new LinearLayout.LayoutParams((int) DensityUtils.dp2px(60f), LinearLayout.LayoutParams.MATCH_PARENT);
                                    pName.weight = 1;
                                    pName.gravity = Gravity.CENTER;
                                    llChild.addView(tvName,pName);


                                    TextView tvId = new TextView(activity);
                                    tvId.setText(idNo);
                                    LinearLayout.LayoutParams pId = new LinearLayout.LayoutParams((int) DensityUtils.dp2px(180f), LinearLayout.LayoutParams.MATCH_PARENT);
                                    pId.weight = 3;
                                    pId.gravity = Gravity.CENTER;
                                    llChild.addView(tvId,pId);

                                    TextView tvPhone = new TextView(activity);
                                    tvPhone.setText(phone);
                                    LinearLayout.LayoutParams pPhone = new LinearLayout.LayoutParams((int) DensityUtils.dp2px(100f), LinearLayout.LayoutParams.MATCH_PARENT);
                                    pPhone.weight = 1;
                                    pPhone.gravity = Gravity.CENTER;
                                    llChild.addView(tvPhone,pPhone);

                                    llContainer.addView(llChild,LinearLayout.LayoutParams.MATCH_PARENT,(int) DensityUtils.dp2px(30f));
                                });
                            }
                        });
                    }
                }
                ll.getChildAt(1).setVisibility(visible?View.GONE:View.VISIBLE);
                ivMore.setRotation(visible?90:270);
            });

            ll.addView(llTitle,LinearLayout.LayoutParams.MATCH_PARENT,(int) DensityUtils.dp2px(32f));
        });

        return ll;
    }

    public static LinearLayout buildOfflineSelectList(BaseActivity activity, Map<String,List<ESONObject>> map){
        LinearLayout llParent = new LinearLayout(activity);
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            for (Map.Entry<String, List<ESONObject>> entry : map.entrySet()) {
                App.Post(()->llParent.addView(buildOfflineSelectList(activity,entry.getKey(),entry.getValue()), new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT)));
            }
        });
        return llParent;
    }
    private static LinearLayout buildOfflineSelectList(BaseActivity activity, String tubNo, List<ESONObject> lst){
        LinearLayout ll = new LinearLayout(activity);
        ll.setOrientation(LinearLayout.VERTICAL);

        App.Post(()->{
            LinearLayout llTitle = new LinearLayout(activity);

            TextView tvTub = new TextView(activity);
            tvTub.setText(tubNo+"   "+lst.size()+"人");
            tvTub.setTextColor(Color.parseColor("#000000"));
            tvTub.getPaint().setFakeBoldText(true);
            tvTub.setTextSize(TypedValue.COMPLEX_UNIT_SP,16);
            LinearLayout.LayoutParams pTub = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,(int) DensityUtils.dp2px(32f));
            pTub.weight = 1;
            pTub.gravity= Gravity.CENTER_VERTICAL | Gravity.LEFT;
            llTitle.addView(tvTub,pTub);


            ImageView ivMore = new ImageView(activity);
            ivMore.setImageResource(R.drawable.ic_offline_more);
            ivMore.setRotation(90);
            ivMore.setColorFilter(Color.parseColor("#bdbdbd"));
            LinearLayout.LayoutParams pMore = new LinearLayout.LayoutParams((int)DensityUtils.dp2px(28f),(int)DensityUtils.dp2px(28f));
            pMore.gravity = Gravity.CENTER;
            pMore.leftMargin  = activity.dp2px(4);
            pMore.rightMargin = activity.dp2px(4);
            llTitle.addView(ivMore,pMore);

            llTitle.setOnClickListener(v -> {
                boolean visible = ll.getChildCount()>1 && ll.getChildAt(1).getVisibility() == View.VISIBLE;
                if(!visible){
                    if(ll.getChildCount()<2){
                        LinearLayout llContainer = new LinearLayout(activity);
                        llContainer.setOrientation(LinearLayout.VERTICAL);
                        ll.addView(llContainer,LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        ThreadPoolProvider.getFixedThreadPool().execute(()->{
                            for(ESONObject item : lst){
                                String name = item.getJSONValue("name" ,"").trim();
                                String idNo = item.getJSONValue("id"   ,"").trim();
                                String phone= item.getJSONValue("phone","").trim();
                                boolean checked = item.getJSONValue("isSelected",true);
                                App.Post(()->{
                                    LinearLayout llChild = new LinearLayout(activity);
                                    llChild.setOrientation(LinearLayout.HORIZONTAL);
                                    TextView tvLabel = new TextView(activity);
                                    tvLabel.setText(llContainer.getChildCount() == lst.size()-1?"└─":"├─");
                                    llChild.addView(tvLabel,LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.MATCH_PARENT);

                                    TextView tvName = new TextView(activity);
                                    tvName.setText(name);
                                    LinearLayout.LayoutParams pName = new LinearLayout.LayoutParams((int) DensityUtils.dp2px(60f), LinearLayout.LayoutParams.MATCH_PARENT);
                                    pName.weight = 1;
                                    pName.gravity = Gravity.CENTER;
                                    llChild.addView(tvName,pName);


                                    TextView tvId = new TextView(activity);
                                    tvId.setText(idNo);
                                    LinearLayout.LayoutParams pId = new LinearLayout.LayoutParams((int) DensityUtils.dp2px(180f), LinearLayout.LayoutParams.MATCH_PARENT);
                                    pId.weight = 3;
                                    pId.gravity = Gravity.CENTER;
                                    llChild.addView(tvId,pId);

                                    TextView tvPhone = new TextView(activity);
                                    tvPhone.setText(phone);
                                    LinearLayout.LayoutParams pPhone = new LinearLayout.LayoutParams((int) DensityUtils.dp2px(100f), LinearLayout.LayoutParams.MATCH_PARENT);
                                    pPhone.weight = 1;
                                    pPhone.gravity = Gravity.CENTER;
                                    llChild.addView(tvPhone,pPhone);

                                    CheckBox cb = new CheckBox(activity);
                                    cb.setChecked(checked);
                                    cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                        @Override
                                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                            item.putValue("isSelected",isChecked);
                                        }
                                    });
                                    LinearLayout.LayoutParams pCb = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                                    llChild.addView(cb,pCb);

                                    llContainer.addView(llChild,LinearLayout.LayoutParams.MATCH_PARENT,(int) DensityUtils.dp2px(30f));
                                });
                            }
                        });
                    }
                }
                ll.getChildAt(1).setVisibility(visible?View.GONE:View.VISIBLE);
                ivMore.setRotation(visible?90:270);
            });

            ll.addView(llTitle,LinearLayout.LayoutParams.MATCH_PARENT,(int) DensityUtils.dp2px(32f));
        });

        return ll;
    }
}
