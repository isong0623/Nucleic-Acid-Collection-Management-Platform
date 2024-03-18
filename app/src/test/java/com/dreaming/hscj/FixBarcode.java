package com.dreaming.hscj;

import com.dreaming.hscj.utils.ExcelUtils;

import org.junit.Test;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import priv.songxusheng.easyjson.ESONObject;

public class FixBarcode {

    ApiProvider apiProvider;

    Map<String, List<ExportExcel.User>> mSrc = new HashMap<>();
    Map<String, List<ExportExcel.Log>>  mLog = new HashMap<>();

    public FixBarcode() throws Exception {}

    void writeUser(String townName, ExcelUtils.Writer writer, ExportExcel.User user, int row, boolean bIsSampling){
        writer.write(0,row,0,townName);
        writer.write(0,row,1,user.name);
        writer.write(0,row,2,user.id);
        writer.write(0,row,3,user.sex);
        writer.write(0,row,4,bIsSampling?"转移成功":"转移失败");
    }

    void excludeSuccess(String path) throws Exception {
        int cfg[] = new int[]{1,2,4};
        ExcelUtils.Reader reader = new ExcelUtils.Reader(new FileInputStream(path));
        for(int i=0,ni=reader.getRowCount(0);i<ni;++i){
            List<String> values = new ArrayList<>();
            for(int j=0,nj=cfg.length;j<nj;++j){
                reader.readAtSync(0, i, cfg[j], new ExcelUtils.IExcelReadListener() {
                    @Override
                    public void onRead(int row, int column, int rowCount, int columnCount, String value) {
                        values.add(value);
                    }
                });
            }
            try {
                if("转移成功".equalsIgnoreCase(values.get(2))){
                    for (Map.Entry<String, List<ExportExcel.Log>> entry : mLog.entrySet()) {
                        boolean bFinded = false;
                        for (ExportExcel.Log log : entry.getValue()) {
                            if(log.isMatchUser(new ExportExcel.User(values.get(0),values.get(1),"1","a",""))){
                                entry.getValue().remove(log);
                                if(entry.getValue().isEmpty()){
                                    mLog.remove(entry.getKey());
                                }
                                bFinded = true;
                                break;
                            }
                        }
                        if(bFinded) break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    void doFixInternal(String townName){
        List<ExportExcel.User> writedUser = new ArrayList<>();

        ExcelUtils.Writer writer = new ExcelUtils.Writer();
        for (Map.Entry<String, List<ExportExcel.Log>> entry : mLog.entrySet()) {
            String name = entry.getKey();
            List<ExportExcel.User> userList = mSrc.get(name);
            if(userList == null) continue;
            List<ExportExcel.Log> logList = entry.getValue();
            for(int i=0,ni=userList.size();i<ni;++i){
                ExportExcel.User user = userList.get(i);
                for(ExportExcel.Log log : logList){
                    if(log.isMatchUser(user)){
                        userList.remove(i);
                        --i;
                        --ni;
                        writedUser.add(user);
                        user.setTime(log.time);
                        user.setTubNo(log.tubNo);
                        ESONObject userInfo = null;
                        for(int j=0;j<3;++j){
                            if(userInfo!=null && userInfo.length()>0) break;
                            try {
                                userInfo = apiProvider.findUserInfoById(user.id);
                            } catch (Exception e) {}
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        if(userInfo == null || userInfo.length() == 0){
                            userInfo = new ESONObject()
                                    .putValue("fullName"      ,user.name)
                                    .putValue("idCard"        ,user.id)
                                    .putValue("idType"        ,"1")
                                    .putValue("mobile"        ,user.phone)
                                    ;
                        }
                        String newTubNo = log.tubNo.substring(0,2)+"1"+log.tubNo.substring(2);

                        boolean bIsDelete = false;
                        for(int j=0;j<3;++j){
                            try {
                                if(bIsDelete = apiProvider.deleteUserInTubNo(log.delId)) break;
                            } catch (Exception e) {
                            }
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                        boolean bSuccess = false;
                        for(int j=0;j<3;++j){
                            try {
                                bSuccess = apiProvider.samplingUser(newTubNo,userInfo);
                                if(bSuccess) break;
                            } catch (Exception e) {
                            }
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                        if(bSuccess){
                            writeUser(townName,writer,user,writedUser.size()+1,true);
                        }
                        else{
                            writeUser(townName,writer,user,writedUser.size()+1,false);
                        }
                        break;
                    }
                }

            }
        }

        writer.save("C:\\Users\\Isidore\\Desktop\\"+townName+"自动化更改结果2.xlsx");
    }

    @Test
    public void fixLJZBarcode() throws Exception {
        apiProvider = new ApiProvider(
            "ljzcyd",
            "Qwer@1324"
        );
        mLog = ExportExcel.readLog(new int[]{1,2,3,4,6},null,"C:\\Users\\Isidore\\Desktop\\2022_05_18__15_01_09.xlsx");
        mSrc = ExportExcel.readUser(new int[]{1,2,5},null,"C:\\Users\\Isidore\\Desktop\\ljz2.xlsx");
//        excludeSuccess("C:\\Users\\Isidore\\Desktop\\冷家庄自动化更改结果.xlsx");
        doFixInternal("冷家庄");
    }

}
