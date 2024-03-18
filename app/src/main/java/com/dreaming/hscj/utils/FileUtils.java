package com.dreaming.hscj.utils;

import android.util.Log;

import com.dreaming.hscj.App;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class FileUtils {
    private static final String TAG = FileUtils.class.getSimpleName();
    public static void delete(File f){
        if(!f.exists()) return;
        if(f.isDirectory()){
            File fs[] = f.listFiles();
            if(fs!=null){
                for(File f0:fs){
                    delete(f0);
                }
            }
        }
        f.delete();
    }

    public static void autoMkdirs(File f){
        if(f.getAbsolutePath().endsWith("\\")||f.getAbsolutePath().endsWith("/")){
            if(!f.exists()) f.mkdirs();
            return;
        }
        if(!f.getParentFile().exists())f.getParentFile().mkdirs();
    }

    public static void recreateFile(File f){
        delete(f);
        createFile(f);
    }

    public static void createFile(File f){
        autoMkdirs(f);
        if(!f.exists()) {
            try { f.createNewFile(); } catch (Exception e) { }
        }
    }

    public static void copy(File fSrc, File fDst) throws IOException {
        if(fSrc == null || fDst==null) return;
        if(!fSrc.exists()) return;
//        Log.e(TAG,"src:"+fSrc.getAbsolutePath());
//        Log.e(TAG,"dst:"+fDst.getAbsolutePath());
        String srcPath = fSrc.getAbsolutePath().replaceAll("//","/").replaceAll("\\\\","/");
        String dstPath = fDst.getAbsolutePath().replaceAll("//","/").replaceAll("\\\\","/");
        if(srcPath.equals(dstPath)) return;

        if(fSrc.isDirectory()){
            fDst.mkdirs();
            File []fCopySrcs = fSrc.listFiles();
            if(fCopySrcs==null) return;
            for(File fCopySrc:fCopySrcs){
                String cpyDstPath = fCopySrc.getAbsolutePath().replaceAll(srcPath, dstPath);
                cpyDstPath = cpyDstPath.replaceAll("//","/");
                cpyDstPath = cpyDstPath.replaceAll("\\\\","/");

                File fCopyDst = new File(cpyDstPath);
                copy(fCopySrc,fCopyDst);
            }
        }
        else{
            if(!fDst.getParentFile().exists()) fDst.getParentFile().mkdirs();
            if(!fDst.exists()) fDst.createNewFile();
            copy(new FileInputStream(fSrc),new FileOutputStream(fDst));
        }
    }


    public static void copy(InputStream is, OutputStream os) {
        try {
            byte[] buffer = new byte[4096];
            int readCount;
            while((readCount = is.read(buffer))>0){
                os.write(buffer,0,readCount);
                os.flush();
            }
        } catch (Exception e) { }
        finally {
            try { is.close(); } catch (Exception e) { e.printStackTrace(); }
            try { os.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }

    public static String readAll(String path){
        StringBuilder sb = new StringBuilder();
        try {
            FileReader fr = new FileReader(path);
            BufferedReader bf = new BufferedReader(fr);
            String str;
            // 按行读取字符串
            while ((str = bf.readLine()) != null) {
                sb.append(str);
            }
            bf.close();
            fr.close();
        } catch (Exception e) { }

        return sb.toString();
    }

    public static String readAll(InputStream is){
        StringBuilder sb = new StringBuilder();

        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        try {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        } catch (Exception e) {}
        finally {
            try { is.close(); } catch (Exception e) { }
        }

        return sb.toString();
    }

    public static void writeToFile(File f, StringBuilder sb, boolean append){
        writeToFile(f,sb.toString(),append);
    }
    public static void writeToFile(File f, StringBuilder sb){
        writeToFile(f,sb,false);
    }

    public static void writeToFile(File f, String data, boolean append){
        try{
            if(!f.getParentFile().exists()) f.getParentFile().mkdirs();

            if(!f.exists() || !append) f.createNewFile();

            //true = append file
            FileWriter fileWritter = new FileWriter(f.getAbsolutePath(),append);
            fileWritter.write(data);
            fileWritter.flush();
            fileWritter.close();
        }catch(Exception e){
        }
    }
    public static void writeToFile(File f, String data){
        writeToFile(f,data,true);
    }


    public static String getExternalDir(){
        String path1 = App.sInstance.getExternalCacheDir().getAbsolutePath();
        String path2 = path1.lastIndexOf("Android") >-1 ? path1.substring(0,path1.lastIndexOf("Android")) : "/storage/emulated/0";
        return path2;
    }
}
