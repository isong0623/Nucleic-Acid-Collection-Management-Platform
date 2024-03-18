package com.dreaming.hscj.activity.community.backup;

import android.util.Base64;
import android.util.Log;

import com.dreaming.hscj.App;
import com.dreaming.hscj.Constants;
import com.dreaming.hscj.core.ThreadPoolProvider;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.template.database.DatabaseTemplate;
import com.dreaming.hscj.template.database.wrapper.BaseDatabase;
import com.dreaming.hscj.template.database.wrapper.ICountListener;
import com.dreaming.hscj.utils.FileUtils;
import com.dreaming.hscj.utils.ToastUtils;
import com.dreaming.security.Defender;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.EncryptionMethod;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Random;

import priv.songxusheng.easyjson.ESONObject;

public class CommunityBackupPresenter extends ICommunityBackupContract.Presenter{
    private static final String TAG = CommunityBackupPresenter.class.getSimpleName();
    int num1 = 0;
    @Override
    void queryDb1Num() {
        Template.getCurrentTemplate().getUserOverallDatabase().count(new ICountListener() {
            @Override
            public void onSuccess(int count) {
                num1 = count;
                mView.setDb1Num(count);
            }

            @Override
            public void onFailure(String err) {
                ToastUtils.show(err);
            }
        });
    }

    int num2 = 0;
    @Override
    void queryDb2Num() {
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            int count = Template.getCurrentTemplate().getNucleicAcidGroupingDatabase().countGroup();
            if(count>-1){
                num2 = count;
                App.Post(()-> mView.setDb2Num(count));
            }
            else{
                App.Post(()->ToastUtils.show("查询分组记录失败！"));
            }
        });
    }

    private String nextPassword(int num){
        StringBuilder sb = new StringBuilder();
        Random random = new Random(System.currentTimeMillis());
        String password = "0123456789aAbBcCdDeEfFgGhHiIjJkKlLmMnNoOpPqQrRsStTuUvVwWxXyYzZ~`!@#$%^&*()_+=-[]\\|}{;':\"<>?,./";
        for(int i=0;i<num;++i){
            sb.append(password.charAt(random.nextInt(password.length())));
        }

        return sb.toString();
    }

    @Override
    void backup() {
        if(num1<1){
            App.Post(()->mView.onBackUpFailure("当前数据库没有数据，无法备份！"));
            return;
        }
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            try {
                String password = nextPassword(256);
                Log.e(TAG,"pwd->"+password);

                Long now = System.currentTimeMillis();

                String encrypt  = new Defender().encrypt(password);
                Log.e(TAG,"encrypt->"+encrypt);

                String zipName = String.format("hscj_%s_%s.mbu", Template.getCurrentTemplate().getDatabaseSetting().getRegionCode(),new SimpleDateFormat("yyyyMMddHHmmss").format(now));
                File fZip = new File(FileUtils.getExternalDir(), zipName);
                if(!fZip.exists()) {
                    fZip.getParentFile().mkdirs();
                }
                else{
                    fZip.delete();
                }
                ZipFile zip = new ZipFile(fZip,password.toCharArray());
                Log.e(TAG,"zip->"+fZip.getAbsolutePath());

                File fEncrypt = new File(App.sInstance.getCacheDir(),"package.info");
                FileUtils.writeToFile(fEncrypt,encrypt,false);

                ZipParameters zipParameters = new ZipParameters();
                zipParameters.setEncryptionMethod(EncryptionMethod.ZIP_STANDARD);
                zipParameters.setEncryptFiles(true);

                File fDatabaseInfo = new File(App.sInstance.getCacheDir(),"database.info");
                FileUtils.writeToFile(fDatabaseInfo, Constants.DBConfig.getSelectedDatabase().toString(),false);
                zip.addFile(fDatabaseInfo,zipParameters);

                ESONObject e = Constants.DBConfig.getSelectedDatabase();
                String regionName  = Template.getCurrentTemplate().getDatabaseSetting().getRegionName();
                String townName    = e.getJSONValue("townName","");
                String villageName = e.getJSONValue("villageName","");
                String dbPath = BaseDatabase.getDatabaseDir(
                        Template.getCurrentTemplate().getDatabaseSetting().getUnifySocialCreditCodes(),
                        0L,
                        regionName,
                        townName,
                        villageName
                );

                ESONObject o = Constants.TemplateConfig.getCurrentTemplate();
                String tPath = o.getJSONValue("path","");

                File fDir = new File(dbPath);
                for(File fDb : fDir.listFiles()){
                    zip.addFile(fDb,zipParameters);
                    Log.e(TAG,"add->"+ fDb.getAbsolutePath());
                }

                if(!tPath.trim().isEmpty() && tPath.endsWith(".template")){
                    File t = new File(tPath);
                    if(t.exists()){
                        zip.addFile(t,zipParameters);
                        Log.e(TAG,"add->"+t.getAbsolutePath());
                    }
                }

                zip.close();

                zip = new ZipFile(fZip);
                zip.addFile(fEncrypt);
                zip.close();

                App.Post(()->mView.onBackUpSuccess(fZip.getAbsolutePath()));
            } catch (Exception e) {
                App.Post(()->mView.onBackUpFailure(e.getMessage()));
                e.printStackTrace();
            }
        });
    }
}
