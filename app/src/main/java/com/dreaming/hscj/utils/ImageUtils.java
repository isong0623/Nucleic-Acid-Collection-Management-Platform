package com.dreaming.hscj.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

public class ImageUtils {
    /**
     * 尺寸压缩（通过缩放图片像素来减少图片占用内存大小）
     *
     * @param bmp
     */
    public static Bitmap sizeCompress(Bitmap bmp) {
        // 尺寸压缩倍数,值越大，图片尺寸越小
        int targetWidth  = 1024;
        int targetHeight = bmp.getHeight() * targetWidth / bmp.getWidth();
        if(targetWidth>bmp.getWidth()){
            targetWidth = bmp.getWidth();
            targetHeight= bmp.getHeight();
        }

        // 压缩Bitmap到对应尺寸
        Bitmap result = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        Rect rect = new Rect(0, 0, targetWidth, targetHeight);
        canvas.drawBitmap(bmp, null, rect, null);

        return result;

//        ByteArrayOutputStream baos = new ByteArrayOutputStream();

//        // 把压缩后的数据存放到baos中
//        result.compress(Bitmap.CompressFormat.JPEG, 100, baos);
//        try {
//            FileOutputStream fos = new FileOutputStream(file);
//            fos.write(baos.toByteArray());
//            fos.flush();
//            fos.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }
}
