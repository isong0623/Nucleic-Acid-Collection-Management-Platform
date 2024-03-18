package com.dreaming.hscj.core.ocr;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.core.content.FileProvider;

import com.baidu.paddle.lite.demo.ocr.OcrResultModel;
import com.baidu.paddle.lite.demo.ocr.PredictEngine;
import com.baidu.paddle.lite.demo.ocr.Utils;
import com.dreaming.hscj.App;
import com.dreaming.hscj.R;
import com.dreaming.hscj.base.BaseActivity;
import com.dreaming.hscj.base.ToastDialog;
import com.dreaming.hscj.core.ThreadPoolProvider;
import com.dreaming.hscj.utils.DensityUtils;
import com.dreaming.hscj.utils.EasyPermission;
import com.dreaming.hscj.utils.FileUtils;
import com.dreaming.hscj.utils.ImageUtils;
import com.dreaming.hscj.utils.ToastUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import priv.songxusheng.easydialog.EasyDialog;
import priv.songxusheng.easydialog.EasyDialogHolder;

public class ImageRecognizer {

    private static File createImageFile() {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = App.sInstance.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = null;
        try {
            image = File.createTempFile(
                    /* prefix */imageFileName,
                    /* suffix */".bmp",
                    /* directory */storageDir
            );
        } catch (Exception e) {
            image = new File(storageDir+"/"+imageFileName+".bmp");
            FileUtils.autoMkdirs(image);
        }

        return image;
    }

    public static <T> void doRecognize(BaseActivity activity, Bitmap image, IRecognizeListener<T> listener){
        new EasyDialog(R.layout.dialog_ocr_waiting,activity)
                .setOnBindDialogListener(new EasyDialog.OnBindDialogListener() {
                    @Override
                    public void onBindDialog(EasyDialogHolder easyDialogHolder) {
                        Bitmap compress = ImageUtils.sizeCompress(image);
                        PredictEngine.recognize(compress, new PredictEngine.PredictListener() {
                            @Override
                            public void onSuccess(List<OcrResultModel> results)  {
                                ThreadPoolProvider.getFixedThreadPool().execute(()->{
                                    try {
                                        T result = listener.process(results);
                                        if(result != null){
                                            App.Post(()->listener.onSuccess(result));
                                        }
                                        else{
                                            App.Post(()->listener.onFailure());
                                        }
                                    } finally {
                                        App.Post(()->easyDialogHolder.dismissDialog());
                                    }
                                });
                            }

                            @Override
                            public void onFailure(String errMsg) {
                                easyDialogHolder.dismissDialog();
                                listener.onFailure();
                            }
                        });
                    }
                })
                .setForegroundResource(R.drawable.shape_ocr_waiting)
                .setAllowDismissWhenBackPressed(false)
                .setAllowDismissWhenTouchOutside(false)
                .setDialogParams((int) DensityUtils.dp2px(100),(int)DensityUtils.dp2px(100))
                .showDialog();
    }

    private static final String TAG = IDCardRecognizer.class.getSimpleName();
    private static final String TAKE_PHOTO_SAVE_PATH = App.sInstance.getCacheDir().getAbsolutePath()+"/ocr/";
    public static <T> void recognize(BaseActivity activity, IRecognizeListener<T> recognizeListener){
        activity.requestPermission(EasyPermission.WRITE_EXTERNAL_STORAGE, new EasyPermission.PermissionResultListener() {
            @Override
            public void onPermissionGranted() {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                final File photoFile = createImageFile();
                // Ensure that there's a camera activity to handle the intent
                if (takePictureIntent.resolveActivity(activity.getPackageManager()) != null) {
                    // Create the File where the photo should go

                    // Continue only if the File was successfully created
                    if (photoFile != null) {
                        Log.i(TAG, "FILEPATH " + activity.getExternalFilesDir("Pictures").getAbsolutePath());
                        Uri photoURI = FileProvider.getUriForFile(activity, "com.dreaming.hscj.fileProvider", photoFile);
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

                        if (!activity.hasPermision(Manifest.permission.CAMERA)){
                            ToastUtils.show("识别图片需要相机权限，请给于相机权限。");
                        }

                        activity.requestPermission(Manifest.permission.CAMERA, new EasyPermission.PermissionResultListener() {
                            @Override
                            public void onPermissionGranted() {
                                if (!activity.hasPermision(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                                    ToastUtils.show("识别图片需要存储权限，请给于存储权限。");
                                }
                                activity.requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, new EasyPermission.PermissionResultListener() {
                                    @Override
                                    public void onPermissionGranted() {
                                        activity.startActivityForResult(takePictureIntent, 2000, new BaseActivity.OnActivityResultItemCallBack() {
                                            @Override
                                            public void OnActivityRequestResult(int resultCode, Intent data) {
                                                if(resultCode == activity.RESULT_OK){
                                                    File fToRecognize = photoFile;
                                                    try {
                                                        FileUtils.copy(photoFile,new File(TAKE_PHOTO_SAVE_PATH,photoFile.getName()));
                                                        FileUtils.delete(photoFile);
                                                        fToRecognize = new File(TAKE_PHOTO_SAVE_PATH,photoFile.getName());
                                                    } catch (Exception e) {}

                                                    ExifInterface exif = null;
                                                    try {
                                                        exif = new ExifInterface(fToRecognize.getAbsolutePath());
                                                    } catch (IOException e) {
                                                        e.printStackTrace();
                                                    }
                                                    int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
                                                    Log.i(TAG, "rotation " + orientation);
                                                    Bitmap image = BitmapFactory.decodeFile(fToRecognize.getAbsolutePath());
                                                    image = Utils.rotateBitmap(image, orientation);
                                                    doRecognize(activity,image,recognizeListener);
                                                }
                                            }
                                        });
                                    }

                                    @Override
                                    public void onPermissionDenied() {
                                        ToastUtils.show("没有授予读取外置存储器权限，无法保存图片！");
                                    }
                                });
                            }

                            @Override
                            public void onPermissionDenied() {
                                ToastUtils.show("没有授予相机权限，无法拍摄图片！");
                            }
                        });

                        Log.i(TAG, "startActivityForResult finished");
                    }
                }
                else{
                    Intent intent = new Intent(Intent.ACTION_PICK, null);
                    intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                    activity.startActivityForResult(intent, 2001, new BaseActivity.OnActivityResultItemCallBack() {
                        @Override
                        public void OnActivityRequestResult(int resultCode, Intent data) {
                            if(resultCode == activity.RESULT_OK){
                                if (data == null)  return;
                                try {
                                    ContentResolver resolver = activity.getContentResolver();
                                    Uri uri = data.getData();
                                    Bitmap image = MediaStore.Images.Media.getBitmap(resolver, uri);
                                    String[] proj = {MediaStore.Images.Media.DATA};
                                    Cursor cursor = activity.managedQuery(uri, proj, null, null, null);
                                    cursor.moveToFirst();
                                    doRecognize(activity,image,recognizeListener);
                                } catch (IOException e) {
                                    Log.e(TAG, e.toString());
                                }
                            }
                        }
                    });
                }
            }

            @Override
            public void onPermissionDenied() {
                ToastDialog.showBottom(activity,"没有读写外部存储权限无法进行识别！");
            }
        });
    }

}
