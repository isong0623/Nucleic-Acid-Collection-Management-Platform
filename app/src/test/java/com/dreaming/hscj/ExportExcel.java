package com.dreaming.hscj;

import com.dreaming.hscj.utils.CheckUtils;
import com.dreaming.hscj.utils.ExcelUtils;

import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExportExcel {

    public static class User{
        final String name;
        final String id;
        final String phone;
        final String sex;
        String address = "";
        String time = new SimpleDateFormat("yyyy-MM-dd").format(System.currentTimeMillis());
        String tubNo = "?";
        public User(
                String name,
                String id,
                String phone,
                String sex,
                String address
        ){
            this.name = name .trim();
            this.id   = id   .trim().toUpperCase();
            this.phone= phone.trim();
            if(sex == null){
                sex = (id.substring(16,17).charAt(0)-'0')%2 == 1 ? "男" : "女";
            }
            this.sex  = sex  .trim();
            this.address = address;
            if(this.address == null){
                this.address = "";
            }
            else{
                this.address = address.trim();
            }
            if(!CheckUtils.isValidIdCard(this.id)) throw new InvalidParameterException();
        }

        public void setAddress(String address){
            if(address == null) return;
            address = address.trim();
            if(this.address == null || this.address.length() < address.length()){
                this.address = address;
            }
        }

        public void setTime(String time){
            if(time == null || time.trim().isEmpty()) return;
            this.time = time.trim();
        }
        public void setTubNo(String tubNo){
            if(tubNo == null || tubNo.isEmpty()) return;
            this.tubNo = tubNo;
        }
    }

    public static class Log{
        final String name;
        final String id;
        final String time;
        final String tubNo;
        final String delId;
        public Log(
             String name,
             String id,
             String time,
             String tubNo,
             String delId
        ){
            this.name = name.trim();
            this.id   = id.trim();
            this.time = time.trim();
            this.tubNo= tubNo.trim();
            this.delId= delId.trim();

            if(this.id.replaceAll("[\\d]{6}[*]{8}[\\d]{3}[\\dxX]","").length()!=0) throw new InvalidParameterException();
            if(this.tubNo.replaceAll("QD[0-9]{9}","").length() != 0) throw new InvalidParameterException();
        }

        boolean isMatchUser(User user){
            if(!isIdMatch(user)) return false;

            return user.name.equalsIgnoreCase(name);
        }

        boolean isIdMatch(User user){
            if(!user.id.startsWith(id.substring(0,5))) return false;
            if(!user.id.endsWith(id.substring(14))) return false;
            return true;
        }
    }

    public static Map<String, List<User>> readUser(int[] iColumns,Map<String,List<User>> mSrc, String fPath)throws Exception{
        if(mSrc == null) mSrc = new HashMap<>();
        ExcelUtils.Reader readerSource = new ExcelUtils.Reader(new FileInputStream(new File(fPath)));
        for(int i=0,ni= readerSource.getRowCount(0);i<ni;++i){
            List<String> lst = new ArrayList<>();
            for(int j=0,nj=iColumns.length;j<nj;++j){
                readerSource.readAtSync(i, iColumns[j], new ExcelUtils.IExcelReadListener() {
                    @Override
                    public void onRead(int row, int column, int rowCount, int columnCount, String value) {
                        lst.add(value);
                    }
                });
            }
            User user = null;
            try {
                user = new User(lst.get(0),lst.get(1),lst.get(2),lst.size()>3?lst.get(3):"",lst.size()>4?lst.get(4):"");
            } catch (Exception e) {
                continue;
            }
            List<User> userList = mSrc.get(user.name);
            if(userList == null) userList = new ArrayList<>();
            boolean bFinded = false;
            for(User comp : userList){
                if(!comp.name.equalsIgnoreCase(user.name)) continue;
                if(!comp.id.equalsIgnoreCase(user.id)) continue;
                bFinded = true;
                comp.setAddress(user.address);
                break;
            }
            if(!bFinded){
                userList.add(user);
            }

            mSrc.put(user.name,userList);
        }
        return mSrc;
    }

    public static Map<String,List<Log>> readLog(int[] iColumns,Map<String, List<Log>> mSrc, String fPath)throws Exception{
        if(mSrc == null) mSrc = new HashMap<>();

        ExcelUtils.Reader readerSource = new ExcelUtils.Reader(new FileInputStream(new File(fPath)));
        for(int i=0,ni= readerSource.getRowCount(0);i<ni;++i){
            List<String> lst = new ArrayList<>();
            for(int j=0,nj=iColumns.length;j<nj;++j){
                readerSource.readAtSync(i+1, iColumns[j], new ExcelUtils.IExcelReadListener() {
                    @Override
                    public void onRead(int row, int column, int rowCount, int columnCount, String value) {
                        lst.add(value.trim().toUpperCase());
                    }
                });
            }

            Log log = null;
            try {
                log = new Log(lst.get(0),lst.get(1),lst.get(2),lst.get(3),lst.size()>4?lst.get(4):"");
            } catch (Exception e) {
                continue;
            }
            List<Log> logList = mSrc.get(log.name);
            if(logList == null) logList = new ArrayList<>();
            logList.add(log);
            mSrc.put(log.name,logList);
        }

        return mSrc;
    }

    void writeUser(String townName,ExcelUtils.Writer writer, User user, int row, boolean bIsSampling){
        writer.write(0,row,0,townName);
        writer.write(0,row,1,user.name);
        writer.write(0,row,2,user.id);
        writer.write(0,row,3,user.sex);
        writer.write(0,row,4,bIsSampling?"已于"+user.time+"采集于试管："+user.tubNo:"不在村中或其他原因未参与集中采集");
    }

    @Test
    public void exportNS() throws Exception{
        Map<String, List<User> > mSrc = readUser(new int[]{0,2,4,1},null,"src\\test\\local\\batch_member.xlsx");
        readUser(new int[]{1,2,3,7,4},mSrc,"src\\test\\local\\ns0503.xlsx");
        readUser(new int[]{1,2,3,7,4},mSrc,"src\\test\\local\\ns0514.xlsx");
        Map<String, List<Log>>   mLog = readLog(new int[]{1,2,3,4},null,"C:\\Users\\Isidore\\Desktop\\2022_05_25__08_56_38.xlsx");
        doExportInternal(mSrc,mLog,"南宋家村");
    }

    @Test
    public void exportTJ() throws Exception{
        Map<String, List<User> > mSrc = readUser(new int[]{1,2,3,7},null,"src\\test\\local\\52.xlsx");
        readUser(new int[]{1,2,3,7,4},mSrc,"src\\test\\local\\54.xlsx");
        readUser(new int[]{1,2,3,7,4},mSrc,"src\\test\\local\\tj0503.xlsx");
        Map<String, List<Log>> mLog = readLog(new int[]{1,2,3,4},null,"C:\\Users\\Isidore\\Desktop\\2022_05_25__08_56_38.xlsx");
        doExportInternal(mSrc,mLog,"唐家村");
    }

    @Test
    public void exportLJZ() throws Exception{
        Map<String, List<User> > mSrc = readUser(new int[]{1,2,3},null,"src\\test\\local\\ljz.xls");
        Map<String, List<Log>> mLog = readLog(new int[]{1,2,3,4},null,"C:\\Users\\Isidore\\Desktop\\2022_05_18__09_20_16.xlsx");
        doExportInternal(mSrc,mLog,"冷家庄");
    }

    void doExportInternal(Map<String, List<User> > mSrc,Map<String, List<Log>>   mLog, String townName){
        List<User> writedUser = new ArrayList<>();

        ExcelUtils.Writer writer = new ExcelUtils.Writer();
        for (Map.Entry<String, List<Log>> entry : mLog.entrySet()) {
            String name = entry.getKey();
            List<User> userList = mSrc.get(name);
            if(userList == null) continue;
            List<Log> logList = entry.getValue();
            for(int i=0,ni=userList.size();i<ni;++i){
                User user = userList.get(i);
                for(Log log : logList){
                    if(log.isMatchUser(user)){
                        userList.remove(i);
                        --i;
                        --ni;
                        writedUser.add(user);
                        user.setTime(log.time);
                        user.setTubNo(log.tubNo);
                        writeUser(townName,writer,user,writedUser.size()+1,true);
                        break;
                    }
                }

            }
        }

        for (Map.Entry<String, List<User>> entry : mSrc.entrySet()) {
            List<User> userList = entry.getValue();
            if(userList == null) continue;
            for(User user:userList){
                writedUser.add(user);
                writeUser(townName,writer,user,writedUser.size()+1,false);
            }
        }

        writer.save("C:\\Users\\Isidore\\Desktop\\全员核酸检测数据分析人员明细表（"+townName+"）.xlsx");
    }
}

