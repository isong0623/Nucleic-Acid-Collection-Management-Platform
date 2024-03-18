package com.dreaming.hscj.core.EasyAdapter;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


public class EasyViewHolder extends RecyclerView.ViewHolder {

    private View vRoot;
    public EasyViewHolder(View itemView) {
        super(itemView);
        vRoot = itemView;
    }

    public View getRootView(){return vRoot;}

    public final <T extends View> T getView(@IdRes int id){
        return vRoot.findViewById(id);
    }

    public <T> T getViewAs(@IdRes int id, T defaultView){
        try {
            return (T)vRoot.findViewById(id);
        } catch (Exception e) {
            return defaultView;
        }
    }


    public int getVisibility(@IdRes int id){
        return getView(id).getVisibility();
    }

    public ImageView getViewAsImageView(@IdRes int id){
        try{ return (ImageView)getView(id); }catch (Exception e){e.printStackTrace();}
        return null;
    }

    public EditText getViewAsEditText(@IdRes int id){
        try{ return (EditText)getView(id); }catch (Exception e){e.printStackTrace();}
        return null;
    }

    public TextView getViewAsTextView(@IdRes int id){
        try{ return (TextView)getView(id); }catch (Exception e){e.printStackTrace(); }
        return null;
    }

    public void setOnClickListener(@IdRes int id , View.OnClickListener listener){
        try { getView(id).setOnClickListener(listener); } catch (Exception e) {e.printStackTrace();}
    }

//    public void setImageByUrl(@IdRes int id ,@NonNull String url){
//        ImageUtils.loadImage(getRootView().getContext(),getViewAsImageView(id),url);
//    }
//    //加载imageView 并且加圆角属性
//    public void setImageCornerByUrl(@IdRes int id , @NonNull String url, int corners){
//        RequestOptions options = new RequestOptions()
//                .transform(new CenterCrop(), new RoundedCorners((int) DensityUtils.dp2px(corners)));
//        ImageUtils.loadImage(getRootView().getContext(),
//                getViewAsImageView(id),
//                url,
//                options,
//                R.drawable.default_img_bg,
//                R.drawable.default_img_bg);
////        ImageUtils.loadImage(corners,getRootView().getContext(),getViewAsImageView(id),url);
//    }
//
//    public void setImageCircleByUrl(@IdRes int id ,@NonNull String url){
//        ImageUtils.loadCircleImage(getRootView().getContext(),getViewAsImageView(id),url, R.drawable.ic_default_user_icon, R.drawable.ic_default_user_icon);
//    }

    public void setText(@IdRes int id , String text){
        try { getViewAsTextView(id).setText(text);} catch (Exception e) {e.printStackTrace();}
    }

    public void setImageDrawable(@IdRes int id, Drawable drawable){
        try { getViewAsImageView(id).setImageDrawable(drawable);} catch (Exception e) {e.printStackTrace();}
    }

    public void setTextColor(@IdRes int id , int color){
        try { getViewAsTextView(id).setTextColor(color);} catch (Exception e) {e.printStackTrace();}
    }

    public void setBackgroundRes(@IdRes int id , @DrawableRes int res){
        try { getView(id).setBackgroundResource(res); } catch (Exception e) {e.printStackTrace();}
    }
    public void setVisible(@IdRes int id, boolean visible){
        try { getView(id).setVisibility(visible? View.VISIBLE: View.GONE); } catch (Exception e) {e.printStackTrace();}
    }

    public void setVisibility(@IdRes int id, int visibility){
        try { getView(id).setVisibility(visibility); } catch (Exception e) {e.printStackTrace();}
    }

    public void setImageResource(@IdRes int id , @DrawableRes int res){
        try { getViewAsImageView(id).setImageResource(res);} catch (Exception e) {e.printStackTrace();}
    }

    public void setBackgroundColor(@IdRes int id, int color){
        try { getView(id).setBackgroundColor(color); } catch (Exception e) {e.printStackTrace();}
    }

    public void setTag(@IdRes int id, Object tag){
        try { getView(id).setTag(tag); } catch (Exception e) {e.printStackTrace();}
    }

    public void setImageBitmap(@IdRes int id, Bitmap bitmap){
        try { getViewAsImageView(id).setImageBitmap(bitmap);} catch (Exception e) {e.printStackTrace();}
    }

    public void onItemSelected() {
        if(Build.VERSION.SDK_INT>=21)itemView.setTranslationZ(10);
    }

    public void onItemClear() {
        if(Build.VERSION.SDK_INT>=21)itemView.setTranslationZ(0);
    }
}