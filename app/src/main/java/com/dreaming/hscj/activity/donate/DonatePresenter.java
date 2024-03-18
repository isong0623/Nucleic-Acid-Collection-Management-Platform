package com.dreaming.hscj.activity.donate;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

import com.dreaming.hscj.App;
import com.dreaming.hscj.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class DonatePresenter extends IDonateContract.Presenter{
    @Override
    void createDonateByWxPayImage() {
        File file = new File("/storage/emulated/0/hsjc/donate_best335.png");
        Drawable drawable = App.sInstance.getResources().getDrawable(R.drawable.ic_donate_we_chat_pay);
        Bitmap bitmap = Bitmap.createBitmap( 1080, 1324,
                drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, 1080, 1324);
        drawable.draw(canvas);
        if(!file.getParentFile().exists()){
            file.getParentFile().mkdirs();
        }
        try { file.createNewFile(); } catch (IOException e) { }
        try {
            FileOutputStream out = new FileOutputStream(file);
            if (bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                out.flush();
                out.close();
            }
        } catch (Exception e) {}
    }
}
