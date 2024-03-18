package com.dreaming.hscj;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

import com.dreaming.hscj.utils.ZxingUtils;

import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ExportDoc {

    public void save(Bitmap bitmap) throws IOException {
        String path= "C:\\Users\\Isidore\\Desktop\\test.jpg";
        File file=new File(path);
        FileOutputStream fos=new FileOutputStream(file);
        bitmap.compress(Bitmap.CompressFormat.JPEG,100,fos);
        fos.close();
    }

    public void createQRImage(String name,String id) throws IOException {
        Bitmap bitmap = ZxingUtils.autoCreateCodeBitmap(id,1080,1080,"utf-8",null,null, Color.BLACK, Color.WHITE);
        Canvas c = new Canvas();
        save(bitmap);
    }

    @Test
    public void doTest() throws IOException{
        createQRImage("唐功瑞","370225195202120815");
    }
}
