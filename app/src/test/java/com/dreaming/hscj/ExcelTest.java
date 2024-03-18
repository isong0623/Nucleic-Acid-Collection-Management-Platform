package com.dreaming.hscj;

import com.dreaming.hscj.utils.CheckUtils;
import com.dreaming.hscj.utils.ExcelUtils;
import com.dreaming.hscj.utils.FileUtils;
import com.dreaming.hscj.utils.JsonUtils;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.Region;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex2.Matcher;
import java.util.regex2.Pattern;

import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

public class ExcelTest {
    @Test
    public void testReadNumber() throws Exception {
        ExcelUtils.Reader reader = new ExcelUtils.Reader(new FileInputStream(new File("C:\\Users\\Isidore\\Desktop\\copy4.xls")));
        reader.readAtSync(0, 2, new ExcelUtils.IExcelReadListener() {
            @Override
            public void onRead(int row, int column, int rowCount, int columnCount, String value) {
                System.out.println(value);
            }
        });
    }

    @Test
    public void exportTJExcelByApi() throws FileNotFoundException {
//        String harPath = "C:\\Users\\Isidore\\Desktop\\tj.har";
        String harPath = "C:\\Users\\Isidore\\Desktop\\ns.har";
//        String harPath = "src\\test\\local\\54.har";
        String suffix = harPath.substring(harPath.lastIndexOf("\\")+1,harPath.lastIndexOf("."));

        String contents = FileUtils.readAll(harPath).replaceAll("\n","");
        ESONObject eHar = new ESONObject(contents);
        ESONObject eLog = eHar.getJSONValue("log",new ESONObject());
        List<ESONObject> lstEntries = JsonUtils.parseToList(eLog.getJSONValue("entries",new ESONArray()));
        Set<String> sIds = new HashSet<>();
        Set<String> sTubs= new HashSet<>();
        Map<String,Integer> mTubCounter = new LinkedHashMap<>();

        ExcelUtils.Writer writer = new ExcelUtils.Writer();

        String sExcelTitle[] = new String[]{"序号"    ,"姓名"     ,"身份号" ,"电话"  ,"地址"    ,"试管编号" ,"采样时间"        };
        String sExcelField[] = new String[]{"__index","fullName","idCard","mobile","address","testNum","__sampling_time"};
        int    sExcelWidth[] = new int   []{6        ,8         ,20      ,13      ,30       ,20       ,30};
        for(int i=0,ni=sExcelTitle.length;i<ni;++i){
            writer.write(0,0,i,sExcelTitle[i]);
            writer.setColumnWidth(0,i,sExcelWidth[i]*256);
        }

        for(int i=lstEntries.size()-1;i>-1;--i){
            ESONObject eEntry = lstEntries.get(i);
            ESONObject eRequest = eEntry.getJSONValue("request", new ESONObject());
            String url = eRequest.getJSONValue("url","");
            if(!url.endsWith("confirmed")) continue;

            ESONObject eResponse = eEntry.getJSONValue("response",new ESONObject());
            int code = eResponse.getJSONValue("status",0);
            if(code != 200) continue;
            ESONObject eResponseResultData = eResponse.getJSONValue("content",new ESONObject());
            ESONObject eResponseResult = eResponseResultData.getJSONValue("text",new ESONObject());
            if(eResponseResult.getJSONValue("code",-1)!=0) continue;
            eResponseResult = eResponseResult.getJSONValue("data",new ESONObject());
            if(eResponseResult.length() == 0) continue;

            ESONObject eParamData = eRequest.getJSONValue("postData",new ESONObject());
            ESONObject eParam = eParamData.getJSONValue("text",new ESONObject());

            String id = eParam.getJSONValue(sExcelField[2],"").toUpperCase().trim();
            if(sIds.contains(id)){
                lstEntries.remove(i);
                ++i;
                continue;
            }
            sIds.add(id);
        }

        sIds.clear();

        SimpleDateFormat format1 =new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        SimpleDateFormat format2 =new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for(ESONObject eEntry : lstEntries){
            ESONObject eRequest = eEntry.getJSONValue("request", new ESONObject());
            String url = eRequest.getJSONValue("url","");
            if(!url.endsWith("confirmed")) continue;

            ESONObject eResponse = eEntry.getJSONValue("response",new ESONObject());
            int code = eResponse.getJSONValue("status",0);
            if(code != 200) continue;
            ESONObject eResponseResultData = eResponse.getJSONValue("content",new ESONObject());
            ESONObject eResponseResult = eResponseResultData.getJSONValue("text",new ESONObject());
            if(eResponseResult.getJSONValue("code",-1)!=0) continue;
            eResponseResult = eResponseResult.getJSONValue("data",new ESONObject());
            if(eResponseResult.length() == 0) continue;

            ESONObject eParamData = eRequest.getJSONValue("postData",new ESONObject());
            ESONObject eParam     = eParamData.getJSONValue("text",new ESONObject());

            try {
                String time = eEntry.getJSONValue("startedDateTime","");
                Long timestamp = format1.parse(time).getTime()+8*3600*1000L;
                eParam.putValue(sExcelField[6],format2.format(timestamp));
            } catch (ParseException e) {
                e.printStackTrace();
            }

            String id = eParam.getJSONValue(sExcelField[2],"").toUpperCase().trim();
            sIds.add(id);
            String tub = eParam.getJSONValue(sExcelField[5],"").toUpperCase().trim();
            sTubs.add(tub);
            eParam.putValue(sExcelField[0],sIds.size());

            Integer tubCount = mTubCounter.get(tub);
            if(tubCount==null) tubCount = 0;
            ++tubCount;
            mTubCounter.put(tub,tubCount);

            for(int i=0,ni=sExcelField.length;i<ni;++i){
                writer.write(0,sIds.size(),i,eParam.getJSONValue(sExcelField[i],""));
            }
        }

        writer.write(0,sIds.size()+2,2,String.format("共%d人",sIds.size()));
        writer.write(0,sIds.size()+2,5,String.format("共%d管",sTubs.size()));

        int i=sIds.size() +4;
        for (Map.Entry<String, Integer> entry : mTubCounter.entrySet()) {
            writer.write(0,i,5,entry.getKey());
            writer.write(0,i,6,String.format("共%d人",entry.getValue()));
            ++i;
        }

        writer.save(new FileOutputStream(new File("C:\\Users\\Isidore\\Desktop", suffix+".xlsx")));

        System.out.println();
        System.out.println(String.format("共%d人",sIds.size()));
        System.out.println();
        System.out.println(String.format("共%d管",sTubs.size()));
        System.out.println();
        System.out.println("all done!");
    }

    @Test
    public void testCopySheet() throws Exception{
        HSSFWorkbook wbOutput = new HSSFWorkbook();
        HSSFSheet sheetOutput = wbOutput.createSheet();

        HSSFWorkbook wbInput = new HSSFWorkbook(new FileInputStream("C:\\Users\\Isidore\\Desktop\\表格（二）（人工登记）新冠肺炎核酸20合一混采检测登记表.xls"));
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
                cellOutput.setCellStyle(wbOutput.createCellStyle());
                cellOutput.getCellStyle().cloneStyleFrom(styleInput);
                HSSFCellStyle styleOutput = cellOutput.getCellStyle();

                //设置文本内容
                String strVal = ExcelUtils.getCellAsString(rowInput, j, wbInput.getCreationHelper().createFormulaEvaluator()).trim();
                cellOutput.setCellValue(strVal);

                //设置宽度
                sheetOutput.setColumnWidth(j,sheetInput.getColumnWidth(j)+256);
            }
        }

        wbOutput.write(new FileOutputStream("C:\\Users\\Isidore\\Desktop\\out.xls"));
    }

}
