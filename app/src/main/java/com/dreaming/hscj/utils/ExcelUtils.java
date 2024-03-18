package com.dreaming.hscj.utils;

import android.util.Log;

import com.dreaming.hscj.App;
import com.dreaming.hscj.core.ThreadPoolProvider;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.Region;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

public class ExcelUtils {
    private static final String TAG = ExcelUtils.class.getSimpleName();
    public interface IExcelReadListener{
        default void onStart(){};
        void onRead(int row, int column, int rowCount, int columnCount, String value);
        default void onError(Exception e){}
        default void onFinished(){}
    }

    public static class Reader{
        private final Workbook workbook;

        public Reader(InputStream is) throws IOException, InvalidFormatException {
            workbook = WorkbookFactory.create(is);
        }

        private int getRowCount(Sheet sheet){
            if(sheet == null) return 0;
            return sheet.getPhysicalNumberOfRows();
        }

        public Workbook getWorkbook(){
            return workbook;
        }

        public int getRowCount(int sheet){
            return getRowCount(workbook.getSheetAt(sheet));
        }

        public int getRowCount(String sheet){
            return getRowCount(workbook.getSheet(sheet));
        }

        private void readAtSync(Sheet sheet, int row, int column, IExcelReadListener listener){
            try {
                final int rowsCount = sheet.getPhysicalNumberOfRows();
                if(row>=rowsCount){
                    listener.onFinished();
                    return;
                }
                final FormulaEvaluator formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();
                final Row rRow = sheet.getRow(row);
                final int cellsCount = rRow.getPhysicalNumberOfCells();
                if(column>=cellsCount){
                    listener.onRead(row,column,rowsCount,cellsCount,null);
                    return;
                }
                listener.onRead(row,column,rowsCount,cellsCount,getCellAsString(rRow,column,formulaEvaluator));
            } catch (Exception e) {
                listener.onError(e);
            }
        }

        private void readAt(Sheet sheet, int row, int column, IExcelReadListener listener){
            ThreadPoolProvider.getFixedThreadPool().execute(()->{
                try {
                    final int rowsCount = sheet.getPhysicalNumberOfRows();
                    if(row>=rowsCount){
                        App.Post(()->listener.onFinished());
                        return;
                    }
                    final FormulaEvaluator formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();
                    final Row rRow = sheet.getRow(row);
                    final int cellsCount = rRow.getPhysicalNumberOfCells();
                    if(column>=cellsCount){
                        App.Post(()->listener.onRead(row,column,rowsCount,cellsCount,null));
                        return;
                    }
                    App.Post(()->listener.onRead(row,column,rowsCount,cellsCount,getCellAsString(rRow,column,formulaEvaluator)));
                } catch (Exception e) {
                    App.Post(()->listener.onError(e));
                }
            });
        }

        public void readAtSync(int row, int column, IExcelReadListener listener){
            readAtSync(0,row,column,listener);
        }
        public void readAt(int row, int column, IExcelReadListener listener){
            readAt(0,row,column,listener);
        }
        public void readAtSync(String sheet, int row, int column, IExcelReadListener listener){
            readAtSync(workbook.getSheet(sheet),row,column,listener);
        }
        public void readAt(String sheet, int row, int column, IExcelReadListener listener){
            readAt(workbook.getSheet(sheet),row,column,listener);
        }
        public void readAtSync(int sheet, int row, int column, IExcelReadListener listener){
            readAtSync(workbook.getSheetAt(sheet),row,column,listener);
        }
        public void readAt(int sheet, int row, int column, IExcelReadListener listener){
            readAt(workbook.getSheetAt(sheet),row,column,listener);
        }

        private void readAll(Sheet sheet, int iStartRow, IExcelReadListener listener){
            ThreadPoolProvider.getFixedThreadPool().execute(()->{
                try {
                    App.Post(()->listener.onStart());
                    final int rowsCount = sheet.getPhysicalNumberOfRows();
                    final FormulaEvaluator formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();
                    for (int r = iStartRow; r<rowsCount; r++) {
                        final Row row = sheet.getRow(r);
                        final int cellsCount = row.getPhysicalNumberOfCells();
                        for (int c = 0; c<cellsCount; c++) {
                            final int iR = r, iC = c;
                            App.Post(()->listener.onRead(iR,iC,rowsCount,cellsCount,getCellAsString(row,iC,formulaEvaluator)));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    App.Post(()->listener.onError(e));
                }

                App.Post(()->listener.onFinished());
            });
        }

        public void readAll(IExcelReadListener listener){
            readAll(workbook.getSheetAt(0),0,listener);
        }

        public void readAll(int sheet, int startRow, IExcelReadListener listener){
            readAll(workbook.getSheetAt(sheet),startRow,listener);
        }

        public void readAll(String sheet, int startRow, IExcelReadListener listener){
            readAll(workbook.getSheet(sheet),startRow,listener);
        }
    }

    public static class Writer{
        private final Workbook workbook;
        private final String strTempFilePath;
        private static Workbook create(InputStream is, String path){
            try {
                OutputStream output = null;
                try {
                    File f = new File(path);
                    if(f.exists()) f.delete();
                    else{
                        if(!f.getParentFile().exists()) f.getParentFile().mkdirs();
                    }
                    f.createNewFile();

                    output = new FileOutputStream(path);
                    byte[] buf = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = is.read(buf)) != -1) {
                        output.write(buf, 0, bytesRead);
                        output.flush();
                    }
                } finally {
                    is.close();
                    output.close();
                }
                return WorkbookFactory.create(new File(path));
            } catch (Exception e) {
                return null;
            }
        }

        public Workbook getWorkbook(){
            return workbook;
        }

        public Writer(InputStream is){
            strTempFilePath = App.sInstance.getCacheDir().getAbsolutePath()+"/excel_temp_"+System.currentTimeMillis()+".xls";
            workbook = create(is,strTempFilePath);
        }

        public Writer(){
            strTempFilePath = null;
            workbook = new XSSFWorkbook();
        }

        private static XSSFWorkbook parsePath(String path){
            try {
                File f = new File(path);
                if(!f.getParentFile().exists()) {
                    f.getParentFile().mkdirs();
                }
                if(!f.exists()){
                    f.createNewFile();
                }
                return new XSSFWorkbook(path);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        public Writer(String outPath) {
            strTempFilePath = outPath;
            workbook = new XSSFWorkbook();
        }

        public void setColumnWidth(int sheet, int column, int width){
            int iSheetNum = workbook.getNumberOfSheets();
            Sheet xlsSheet = sheet<iSheetNum ? workbook.getSheetAt(sheet) : null;

            if(xlsSheet == null){
                for(int i= iSheetNum;i<sheet;++i){
                    workbook.createSheet(WorkbookUtil.createSafeSheetName("sheet"+i));
                }
                xlsSheet = workbook.createSheet(WorkbookUtil.createSafeSheetName("sheet"+sheet));
            }
            xlsSheet.setColumnWidth(column,width);
        }

        public void write(String sheet, int row, int column, String value) {

            Sheet xlsSheet = workbook.getSheet(sheet);
            if(xlsSheet == null){
                xlsSheet = workbook.createSheet(WorkbookUtil.createSafeSheetName(sheet));
            }
            int rowsCount = xlsSheet.getPhysicalNumberOfRows();
            boolean bIsCreateRow = row>=rowsCount;
            Row xlsRow =  bIsCreateRow ? xlsSheet.getRow(row) : xlsSheet.createRow(rowsCount);

            int columnCount = xlsRow.getPhysicalNumberOfCells();
            boolean bIsCreateColumn = column>=columnCount;
            Cell xlsCell = bIsCreateColumn ? xlsRow.createCell(column) : xlsRow.getCell(column);

            xlsCell.setCellValue(value);
        }

        public void write(int sheet, int row, int column, String value) {
            int iSheetNum = workbook.getNumberOfSheets();
            Sheet xlsSheet = sheet<iSheetNum ? workbook.getSheetAt(sheet) : null;

            if(xlsSheet == null){
                for(int i= iSheetNum;i<sheet;++i){
                    workbook.createSheet(WorkbookUtil.createSafeSheetName("sheet"+i));
                }
                xlsSheet = workbook.createSheet(WorkbookUtil.createSafeSheetName("sheet"+sheet));
            }
            Row xlsRow = null;
            try {
                int rowsCount = xlsSheet.getPhysicalNumberOfRows();
                boolean bIsCreateRow = row>=rowsCount;
                if(bIsCreateRow){
                    for(int i=rowsCount;i<=row;++i){
                        xlsSheet.createRow(i);
                    }
                }
                xlsRow =  xlsSheet.getRow(row);
            } catch (Exception e) {
                e.printStackTrace();
                for(int i=0;i<=row;++i){
                    xlsSheet.createRow(i);
                }
                xlsRow =  xlsSheet.getRow(row);
            }

            Cell xlsCell = null;
            try {
                int columnCount = xlsRow.getPhysicalNumberOfCells();
                boolean bIsCreateColumn = column>=columnCount;
                if(bIsCreateColumn){
                    for(int i=columnCount;i<=column;++i){
                        xlsRow.createCell(i);
                    }
                }
                xlsCell = xlsRow.getCell(column);
            } catch (Exception e) {
                e.printStackTrace();
                for(int i=0;i<=column;++i){
                    xlsRow.createCell(i);
                }
            }

            xlsCell.setCellValue(value);
        }

        public void save(String outPath){
            FileOutputStream outputStream = null;
            try {
                outputStream = new FileOutputStream(new File(outPath));
                workbook.write(outputStream);
                outputStream.flush();
                outputStream.close();
            } catch (Exception e) {}
        }

        public void save(OutputStream outputStream){
            try {
                workbook.write(outputStream);
                outputStream.flush();
                outputStream.close();
            } catch (Exception e) {}
        }
    }

    public static String getCellAsString(Row row, int c, FormulaEvaluator formulaEvaluator) {
        String value = "";
        try {
            Cell cell = row.getCell(c);
            CellValue cellValue = formulaEvaluator.evaluate(cell);
            switch (cellValue.getCellType()) {
                case Cell.CELL_TYPE_BOOLEAN:
                    value = ""+cellValue.getBooleanValue();
                    break;
                case Cell.CELL_TYPE_NUMERIC:
                    DecimalFormat df = new DecimalFormat("#");
                    if(HSSFDateUtil.isCellDateFormatted(cell)) {
                        double date = cellValue.getNumberValue();
                        value = String.valueOf(HSSFDateUtil.getJavaDate(date).getTime());
                    } else {
                        value = df.format(cell.getNumericCellValue());
                    }
                    break;
                case Cell.CELL_TYPE_STRING:
                    value = ""+cellValue.getStringValue();
                    break;
                default:
            }
        } catch (NullPointerException e) {
            /* proper error handling should be here */
            log(e.toString());
        }
        return value;
    }

    private static void log(String str) {
        Log.e(TAG,str);
    }

    public static void copySheet(HSSFSheet resSheet,HSSFSheet desSheet) {
        int sheetMergerCount = resSheet.getNumMergedRegions();
        for (int i = 0; i < sheetMergerCount; i++) {
            Region mergedRegionAt = resSheet.getMergedRegionAt(i);
            desSheet.addMergedRegion(mergedRegionAt);
        }
    }

}
