package com.dreaming.hscj.entity;

import com.dreaming.hscj.App;
import com.dreaming.hscj.Constants;
import com.dreaming.hscj.R;
import com.dreaming.hscj.core.ThreadPoolProvider;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.utils.ExcelUtils;
import com.dreaming.hscj.utils.FileUtils;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.util.Region;
import org.apache.xmlbeans.impl.piccolo.util.DuplicateKeyException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.NotActiveException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import priv.songxusheng.easyjson.ESONObject;

public class OfflineExcel {
    public static OfflineExcel create(Config cfg) throws IOException {
        return new OfflineExcel(cfg);
    }

    public static OfflineExcel load(String path) throws IOException {
        return new OfflineExcel(path);
    }

    HSSFWorkbook wb;
    Config cfg;
    HashMap<String,List<ESONObject>> mAllSamplings = new HashMap<>();
    HashMap<String,HSSFSheet> mSheetMapping = new HashMap<>();
    HashMap<String,String> mIdMapping = new HashMap<>();
    private OfflineExcel(String path) throws IOException {
        wb = new HSSFWorkbook(new FileInputStream(path));
        this.cfg = readConfig();
        cfg.path = path;
        readAllSampling();
    }

    private OfflineExcel(Config cfg) throws IOException {
        this.cfg = cfg;
        File f = new File(cfg.path);
        if(!f.getParentFile().exists()){
            if(!f.getParentFile().mkdirs()) throw new IOException("离线文件夹创建失败！");
        }
        else if(f.exists()){
            if(!f.delete()) throw new IOException("离线文件删除失败！");
        }
        if(!f.createNewFile()) throw new IOException("离线文件创建失败！");
        wb = new HSSFWorkbook();

        copySheet(wb.createSheet("Sheet1"));

        setSamplingAddress(cfg.samplingAddress.value);
        setSamplingTime   (System.currentTimeMillis());
        setSamplingPeople (cfg.samplingPeople .value);
        setSenderPeople   (cfg.senderPeople   .value);
        setSenderPhoneNo  (cfg.senderPhoneNo  .value);
        setSendTime       (cfg.sendTime       .value);
        setReceiverPeople (cfg.receiverPeople .value);
        setReceiveTime    (cfg.receiveTime    .value);

        FileOutputStream fos = new FileOutputStream(f);
        wb.write(fos);
        fos.close();
    }

    private Config readConfig(){
        Config config = Config.getDefault();
        HSSFSheet sheet = wb.getSheetAt(0);
        config.samplingAddress.value = readInternal(sheet,config.samplingAddress.rowIndex,config.samplingAddress.columnIndex);
        config.samplingTime   .value = readInternal(sheet,config.samplingTime   .rowIndex,config.samplingTime   .columnIndex);
        config.samplingPeople .value = readInternal(sheet,config.samplingPeople .rowIndex,config.samplingPeople .columnIndex);
        config.senderPeople   .value = readInternal(sheet,config.senderPeople   .rowIndex,config.senderPeople   .columnIndex);
        config.senderPhoneNo  .value = readInternal(sheet,config.senderPhoneNo  .rowIndex,config.senderPhoneNo  .columnIndex);
        config.sendTime       .value = readInternal(sheet,config.sendTime       .rowIndex,config.sendTime       .columnIndex);
        config.receiverPeople .value = readInternal(sheet,config.receiverPeople .rowIndex,config.receiverPeople .columnIndex);
        config.receiveTime    .value = readInternal(sheet,config.receiveTime    .rowIndex,config.receiveTime    .columnIndex);
        return config;
    }

    public Config getConfig(){
        return cfg;
    }

    private void readAllSampling(){
        mAllSamplings.clear();
        mSheetMapping.clear();
        for (int i=0,ni=wb.getNumberOfSheets();i<ni;++i){
            HSSFSheet sheet = wb.getSheetAt(i);
            mSheetMapping.put(sheet.getSheetName(),sheet);
            List<ESONObject> lst = readSampling(sheet);
            if(lst == null || lst.isEmpty()) continue;

            mAllSamplings.put(sheet.getSheetName(),lst);
        }
    }

    public Map<String,List<ESONObject>> getAllSampling(){
        return mAllSamplings;
    }

    private List<ESONObject> readSampling(HSSFSheet sheet){
        List<ESONObject> lst = new ArrayList<>();
        for(int i=7;i<27;++i){
            String name = readInternal(sheet,i,2);

            if(name.trim().isEmpty()) continue;

            String id = "";
            for(int j=3;j<21;++j){
                id += readInternal(sheet,i,j).trim();
            }
            mIdMapping.put(id,sheet.getSheetName());

            String phone = readInternal(sheet,i,'v'-'a');

            lst.add(new ESONObject().putValue("name",name).putValue("id",id).putValue("phone",phone));
        }

        return lst;
    }

    public void deleteSampling(String tubNo, String id){
        if(tubNo==null || tubNo.trim().isEmpty()){
            throw new InvalidParameterException("条码不能为空！");
        }
        tubNo = tubNo.trim();

        if(id == null || id.trim().isEmpty()){
            throw new InvalidParameterException("采样身份号不能为空！");
        }
        id = id.trim();

        String find = mIdMapping.get(id);
        if(find == null) return;
        mIdMapping.remove(id);

        List<ESONObject> lst = mAllSamplings.get(tubNo);
        if(lst.size() <1) return;

        if(lst.size() == 1){
            deleteInternal(tubNo,id,0);
            lst.clear();
            mAllSamplings.remove(tubNo);
            if(wb.getNumberOfSheets()>1){
                for (int i=0,ni=wb.getNumberOfSheets();i<ni;++i) {
                    HSSFSheet sheet = wb.getSheetAt(i);
                    if(!sheet.getSheetName().equalsIgnoreCase(tubNo)) continue;
                    wb.removeSheetAt(i);
                    mSheetMapping.remove(tubNo);
                    break;
                }
            }
            return;
        }

        for(int i=0,ni=lst.size();i<ni;++i){
            ESONObject item = lst.get(i);
            String idComp = item.getJSONValue("id","").trim();
            if(idComp.equalsIgnoreCase(id)){
                deleteInternal(tubNo,id,i);
                for(int j=i+i;j<ni;++j){
                    int lj = j - 1;
                    ESONObject assign = lst.get(j);
                    String name = assign.getJSONValue("name","");
                    String idNo = assign.getJSONValue("id","");
                    String phone= assign.getJSONValue("phone","");
                    assignInternal(tubNo,name,idNo,phone,lj);
                }
                lst.remove(i);
                return;
            }
        }
    }

    private void assignInternal(String tubNo, String name, String id, String phone, int index){
        HSSFSheet sheet = wb.getSheet(tubNo);
        if(sheet == null) return;
        writeInternal(sheet,7+index,2,name);
        for(int j=3;j<21;++j){
            if(j-2>id.length()) break;
            writeInternal(sheet,7+index,j,id.charAt(j-3)+"");
        }
        writeInternal(sheet,7+index,'v'-'a',phone);
    }

    private void deleteInternal(String tubNo, String id, int index){
        assignInternal(tubNo,"","","",index);
    }

    AtomicLong lLastAutoSaveTimestamp = new AtomicLong(0);
    public void sampling(String tubNo, String name, String id, String phone) throws IOException, DuplicateKeyException {
        if(tubNo==null || tubNo.trim().isEmpty()){
            throw new InvalidParameterException("条码不能为空！");
        }
        tubNo = tubNo.trim();

        if(name == null || name.trim().isEmpty()){
            throw new InvalidParameterException("采样姓名不能为空！");
        }
        name = name.trim();

        if(id == null || id.trim().isEmpty()){
            throw new InvalidParameterException("采样身份号不能为空！");
        }
        id = id.trim();

        if(phone == null || phone.trim().isEmpty()){
            throw new InvalidParameterException("采样联系方式不能为空！");
        }
        phone = phone.trim();

        if(mIdMapping.containsKey(id)){
            throw new DuplicateKeyException(String.format("%s%s已经在本地离线试管%s录入",name,id.substring(14),tubNo));
        }

        HSSFSheet sheet = mSheetMapping.get(tubNo);
        if(sheet == null){
            sheet = wb.getSheet(tubNo);
            if(sheet == null){
                for (int i=0,ni=wb.getNumberOfSheets();i<ni;++i){
                    HSSFSheet tmp = wb.getSheetAt(i);
                    List<ESONObject> lst = mAllSamplings.get(tmp.getSheetName());
                    if(lst == null || lst.size() == 0){
                        sheet = tmp;
                        wb.setSheetName(i,tubNo);
                    }
                }

            }
            if(sheet == null) sheet = wb.createSheet(tubNo);

            copySheet(sheet);
            setSamplingAddress(cfg.samplingAddress.value);
            setSamplingTime   (System.currentTimeMillis());
            setSamplingPeople (cfg.samplingPeople .value);
            setSenderPeople   (cfg.senderPeople   .value);
            setSenderPhoneNo  (cfg.senderPhoneNo  .value);
            setSendTime       (cfg.sendTime       .value);
            setReceiverPeople (cfg.receiverPeople .value);
            setReceiveTime    (cfg.receiveTime    .value);
            mSheetMapping.put(tubNo,sheet);
        }

        List<ESONObject> lst = mAllSamplings.get(tubNo);
        if(lst == null){
            lst = new ArrayList<>();
            mAllSamplings.put(tubNo,lst);
        }

        if(lst.size()>= Template.getCurrentTemplate().getDatabaseSetting().getGroupMemberNum()){
            throw new NotActiveException(String.format("试管%s已经录满，不能继续添加！",tubNo));
        }

        if(lst.size() == 0){
            writeInternal(sheet,7,0,tubNo);
        }

        writeInternal(sheet,7+lst.size(),2,name);
        for(int j=3;j<21;++j){
            if(j-2>id.length()) break;
            writeInternal(sheet,7+lst.size(),j,id.charAt(j-3)+"");
        }
        writeInternal(sheet,7+ lst.size(),'v'-'a',phone);
        lst.add(new ESONObject().putValue("name",name).putValue("id",id).putValue("phone",phone));

        mIdMapping.put(id,tubNo);

        long now = System.currentTimeMillis();
        if(now - lLastAutoSaveTimestamp.get() < 300000L) return;
        lLastAutoSaveTimestamp.set(now);
        ThreadPoolProvider.getFixedThreadPool().execute(()->{
            File fBackupDir = new File(String.format("%s/hscj/offline_sampling/%s/%s/backup/", FileUtils.getExternalDir(), Template.getCurrentTemplate().getDatabaseSetting().getSPUnify(), Constants.DBConfig.getSelectedDatabase().getJSONValue("id",0)+""));
            if(!fBackupDir.exists()) fBackupDir.mkdirs();
            if(fBackupDir.listFiles() != null){
                for(File fBackup:fBackupDir.listFiles()){
                    try {
                        long createTime = Long.parseLong(fBackup.getName().replaceAll(".xls","").replaceAll(".xlsx",""));
                        if(now - createTime>3600000L){
                            fBackup.delete();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            try {
                File fCreate = new File(String.format("%s/%d.xls",fBackupDir.getAbsolutePath(),lLastAutoSaveTimestamp.get()));
                fCreate.delete();
                fCreate.createNewFile();
                FileOutputStream fos = new FileOutputStream(fCreate);
                wb.write(fos);
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void copySheet(HSSFSheet dstSheet) throws IOException {

        HSSFSheet sheetOutput = dstSheet;

        HSSFWorkbook wbInput = new HSSFWorkbook(App.sInstance.getResources().openRawResource(R.raw.offline_sampling_export));
        HSSFSheet sheetInput = wbInput.getSheetAt(0);

        // 复制源表中的合并单元格
        int sheetMergerCount = sheetInput.getNumMergedRegions();
        for (int i = 0; i < sheetMergerCount; i++) {
            Region mergedRegionAt = sheetInput.getMergedRegionAt(i);
            sheetOutput.addMergedRegion(mergedRegionAt);
        }

        for (int i = sheetInput.getFirstRowNum(),ni = sheetInput.getLastRowNum()+1; i < ni; ++i) {
            HSSFRow rowInput = sheetInput.getRow(i);
            if(rowInput == null) continue;
            HSSFRow rowOutput = sheetOutput.createRow(i);

            //设置高度
            rowOutput.setHeight(rowInput.getHeight());

            for (int j = 0,nj = rowInput.getLastCellNum()+1; j < nj; ++j) {
                HSSFCell cellInput = rowInput.getCell(j);
                if(cellInput == null) continue;
                HSSFCell cellOutput = rowOutput.createCell(j);

                //设置字体
                HSSFCellStyle styleInput = cellInput.getCellStyle();
                cellOutput.setCellStyle(wb.createCellStyle());
                cellOutput.getCellStyle().cloneStyleFrom(styleInput);

                //设置文本内容
                String strVal = ExcelUtils.getCellAsString(rowInput, j, wbInput.getCreationHelper().createFormulaEvaluator()).trim();
                cellOutput.setCellValue(strVal);

                //设置宽度
                sheetOutput.setColumnWidth(j,sheetInput.getColumnWidth(j)+256);
            }
        }

        wbInput.close();
    }

    //region 全局配置

    public void setPath(String text) throws IOException{
        if(text.equalsIgnoreCase(cfg.path)) return;
        close();
        if(new File(cfg.path).renameTo(new File(text))){
            cfg.path = text;
            wb = new HSSFWorkbook(new FileInputStream(text));
            return;
        }
        throw new IOException();
    }

    public void setSamplingAddress(String text){
        CellConfig c = cfg.samplingAddress;
        c.value = text;
        writeAll(c.rowIndex,c.columnIndex,text);
    }

    public void setSamplingTime(long timestamp){
        try {
            String text = cfg.samplingTime.value;
            text = text.substring("采集日期：".length());
            text = text.substring(0,text.indexOf("-")-1);
            String month = text.substring(0,text.indexOf("月")).trim();
            text = text.substring(text.indexOf("月")+1);
            String day   = text.substring(0,text.indexOf("日")).trim();
            text = text.substring(text.indexOf("日")+1);
            String hour  = text.substring(0,text.indexOf("时")).trim();

            long now = System.currentTimeMillis();
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(now);
            int year = c.get(Calendar.YEAR);
            int m = c.get(Calendar.MONTH)+1;

            if(m == 1 && Integer.valueOf(month) == 12){
                --year;
                c.set(Calendar.YEAR,year);
            }

            int d2 = Integer.valueOf(day);
            if(c.getTimeInMillis() < timestamp){
                d2 += (int) ((timestamp - c.getTimeInMillis())/3600000L);
            }

            setSamplingTime(String.format("%02d月%02d日%02d时-%02d时",Integer.valueOf(month),Integer.valueOf(day),Integer.valueOf(hour),d2));
        } catch (Exception e) {
            long now = System.currentTimeMillis();
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(now);

            int m = c.get(Calendar.MONTH)+1;
            int d = c.get(Calendar.DAY_OF_MONTH);
            int h = c.get(Calendar.HOUR_OF_DAY);

            int d2 = d;
            if(now < timestamp){
                d2 = (int) (d + (timestamp - now)/3600000L);
            }

            setSamplingTime(String.format("%02d月%02d日%02d时-%02d时",m,d,h,d2));
        }
    }

    public void setSamplingTime(String text){
        CellConfig c = cfg.samplingTime;
        c.value = text;
        writeAll(c.rowIndex,c.columnIndex,text);
    }

    public void setSamplingPeople(String text){
        CellConfig c = cfg.samplingPeople;
        c.value = text;
        writeAll(c.rowIndex,c.columnIndex,text);
    }

    public void setSenderPeople(String text){
        CellConfig c = cfg.senderPeople;
        c.value = text;
        writeAll(c.rowIndex,c.columnIndex,text);
    }

    public void setSenderPhoneNo(String text){
        CellConfig c = cfg.senderPhoneNo;
        c.value = text;
        writeAll(c.rowIndex,c.columnIndex,text);
    }

    public void setSendTime(long timestamp){
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timestamp);
        int m = c.get(Calendar.MONTH)+1;
        int d = c.get(Calendar.DAY_OF_MONTH);
        int h = c.get(Calendar.HOUR_OF_DAY);
        setSendTime(m,d,h);
    }

    public void setSendTime(int m, int d, int h){
        setSendTime(String.format("%02d年%02d月%02d时",m,d,h));
    }

    public void setSendTime(String text){
        CellConfig c = cfg.sendTime;
        c.value = text;
        writeAll(c.rowIndex,c.columnIndex,text);
    }

    public void setReceiverPeople(String text){
        CellConfig c = cfg.receiverPeople;
        c.value = text;
        writeAll(c.rowIndex,c.columnIndex,text);
    }

    public void setReceiveTime(long timestamp){
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timestamp);
        int m = c.get(Calendar.MONTH)+1;
        int d = c.get(Calendar.DAY_OF_MONTH);
        int h = c.get(Calendar.HOUR_OF_DAY);
        setReceiveTime(m,d,h);
    }

    public void setReceiveTime(int m, int d, int h){
        setReceiveTime(String.format("%02d年%02d月%02d时",m,d,h));
    }

    public void setReceiveTime(String text){
        CellConfig c = cfg.receiveTime;
        c.value = text;
        writeAll(c.rowIndex,c.columnIndex,text);
    }
    //endregion

    private void writeAll(int row, int column, String text){
        for (int i=0,ni=wb.getNumberOfSheets();i<ni;++i){
            writeInternal(wb.getSheetAt(i),row,column,text);
        }
    }

    private void writeInternal(HSSFSheet sheet, int row, int column, String text){
        if(sheet == null) return;
        HSSFRow r = sheet.getRow(row);
        if(r == null){
            r = sheet.createRow(row);
        }
        HSSFCell c = r.getCell(column);
        if(c == null){
            c = r.createCell(column);
        }
        c.setCellValue(text);
    }

    private String readInternal(HSSFSheet sheet, int row, int column){
        if(sheet == null) return "";
        HSSFRow r = sheet.getRow(row);
        if(r == null) return "";
        HSSFCell c = r.getCell(column);
        if(c == null) return "";
        return ExcelUtils.getCellAsString(r,column,wb.getCreationHelper().createFormulaEvaluator());
    }

    public void close() throws IOException{
        File cur = new File(cfg.path);
        boolean bShouldBackup = !cur.exists();
        if(bShouldBackup){
            File tmp = new File(cfg.path+".tmp");
            if(tmp.exists()) {
                if(!tmp.delete()) throw new IOException();
            }
            if(!cur.renameTo(tmp)) throw new IOException();
        }

        FileOutputStream fos = new FileOutputStream(cur);
        try {
            wb.write(fos);
        } finally {
            try { fos.close(); } catch (Exception e) { }
        }

        wb.close();

        if(bShouldBackup){
            File tmp = new File(cfg.path+".tmp");
            for(int i=0;i<10;++i){
                if(tmp.delete()) break;
            }
        }
    }

    public static class CellConfig{
        public String value;
        public int rowIndex;
        public int columnIndex;

        public CellConfig(int rowIndex, int columnIndex){
            this.rowIndex = rowIndex;
            this.columnIndex = columnIndex;
        }
    }

    public static class Config{
        public String path;
        public CellConfig samplingAddress;
        public CellConfig samplingTime;
        public CellConfig samplingPeople;
        public CellConfig senderPeople;
        public CellConfig senderPhoneNo;
        public CellConfig sendTime;
        public CellConfig receiverPeople;
        public CellConfig receiveTime;

        public static Config getDefault(){
            Config config = new Config();

            config.samplingAddress = new CellConfig(3,1);
            config.samplingTime    = new CellConfig(3,'g'-'a');
            config.samplingPeople  = new CellConfig(3,'v'-'a');
            config.senderPeople    = new CellConfig(4,1);
            config.senderPhoneNo   = new CellConfig(4,'n'-'a');
            config.sendTime        = new CellConfig(5,1);
            config.receiverPeople  = new CellConfig(5,'j'-'a');
            config.receiveTime     = new CellConfig(5,'u'-'a');

            return config;
        }
    }
}
