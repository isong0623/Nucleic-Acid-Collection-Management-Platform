package com.dreaming.hscj.activity.nucleic_acid.batch_grouping;

import android.util.Pair;

import com.dreaming.hscj.base.contract.BasePresenter;
import com.dreaming.hscj.base.contract.BaseView;
import com.dreaming.hscj.template.database.wrapper.ISetterListener;

import java.util.List;

import priv.songxusheng.easyjson.ESONObject;

public interface INABatchGroupingContract {
    interface View extends BaseView{
        void onReading(int row, int column, int progress, int count, String value);
        void onReadingGroupExcelCount(int count);
        void onReadingGroupErrorCount(int count);
        void onReadingGroupLimitErrorCount(int count);
        void onReadingGroupDuplicatedCount(int count);
        void onReadingGroupMemberEmpty(int count);
        void onReadSuccess();
        void onReadError(String err);
        void onSaving(int progress);
        void onSavingGroupSuccessCount(int count);
        void onSavingGroupFailureCount(int count);
        void onSaveSuccess();
        void onSaveFailure(String err);

        void onProcess(String msg);
        void onProgress(int progress);
    }

    abstract class Presenter extends BasePresenter<View>{

        abstract void setPath(int path);
        abstract int getPath();
        abstract int getProgress();

        abstract void read(String path, int sheet, int startLine) throws Exception;
        abstract List<Pair<Integer, ESONObject>> getReadGroupExcelSuccess();
        abstract List<Pair<Integer, ESONObject>> getReadGroupExcelFailure();
        abstract List<Pair<Integer, ESONObject>> getReadGroupExcelDuplicated();
        abstract List<Pair<Integer, ESONObject>> getReadGroupLimitError();
        abstract List<Pair<Integer, ESONObject>> getReadGroupMemberEmpty();
        abstract List<Pair<Integer, ESONObject>> getSaveGroupSuccess();
        abstract List<Pair<Integer, ESONObject>> getSaveGroupFailure();
        abstract void autoSave();
        abstract boolean isEndAutoSave();
        abstract void saveGroup(int index, ESONObject data, ISetterListener listener);

        abstract void markTransactionSuccess();
        abstract void markTransactionFailure();
    }
}
