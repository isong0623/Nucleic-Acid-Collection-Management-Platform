package com.dreaming.hscj.activity.community.batch_input;

import android.util.Pair;
import android.view.View;

import com.dreaming.hscj.base.contract.BasePresenter;
import com.dreaming.hscj.base.contract.BaseView;
import com.dreaming.hscj.template.api.wrapper.ApiParam;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import priv.songxusheng.easyjson.ESONObject;

public interface ICommunityBatchInputContract {
    interface View extends BaseView{
        void onReadExcelStarted();
        void onReadingExcel(int row, int column, int progress, int count, String value);
        void onReadCheckFailed(int failedCount);
        void onReadDuplicated(int duplicatedCount);
        void onReadError(String err);
        void onReadFinished();

        void onProcessStart();
        void onProcessSuccess(int successCount, int progress);
        void onProcessNetEmpty(int emptyCount);
        void onProcessDuplicated(int duplicatedCount);
        void onProcessNetError(int errCount);
        void onProcessLocalFailed(int failedCount);
        void onProcessLocalSuccess(int successCount);
        void onProcessFinished();

        void updateProcessMessage(String msg);
    }

    abstract class Presenter extends BasePresenter<View>{
        abstract void readExcel(String path, int sheet, int startLine) throws IOException, InvalidFormatException, Exception;
        abstract List<Pair<Integer, ESONObject>> getExcelPreviewData();
        abstract List<Pair<Integer, ESONObject>> getExcelSuccessData();
        abstract List<Pair<Integer, ESONObject>> getExcelDuplicatedData();
        abstract List<Pair<Integer, ESONObject>> getExcelCheckFailedData();

        abstract boolean canFinish();

        abstract void processExcel();
        abstract void setCovered(boolean b);
        abstract List<Pair<Integer, ESONObject>> getLocalDuplicatedData();
        abstract List<Pair<Integer, ESONObject>> getNetSuccessData();
        abstract List<Pair<Integer, ESONObject>> getNetEmptyData();
        abstract List<Pair<Integer, ESONObject>> getNetErrorData();
        abstract List<Pair<Integer, ESONObject>> getLocalSaveFailedData();
        abstract List<Pair<Integer, ESONObject>> getLocalSaveSuccessData();
        abstract ESONObject getNetShownData(String idCardNo);
        abstract ESONObject getLocalShownData(String idCardNo);
        abstract void saveToLocal(List<Pair<ApiParam,String>> data , CommunityBatchInputActivity.ISavingListener listener);
    }
}
