package com.dreaming.hscj.activity.community.recovery;

import android.util.Pair;

import com.dreaming.hscj.base.contract.BasePresenter;
import com.dreaming.hscj.base.contract.BaseView;
import com.dreaming.hscj.template.database.wrapper.ISetterListener;

import java.util.List;

import priv.songxusheng.easyjson.ESON;
import priv.songxusheng.easyjson.ESONObject;

public interface ICommunityRecoveryContract {
    interface View extends BaseView{
        void onExtracting(int progress);
        void onExtractSuccess();
        void onExtractFailure(String err);
        void onVerifying(int progress);
        void onVerifySuccess();
        void onVerifyFailure(String err);
        void onReading(int progress);
        void onReadingMemberLocalCount(int count);
        void onReadingMemberRecoveryCount(int count);
        void onReadingMemberFailureCount(int count);
        void onReadingGroupLocalCount(int count);
        void onReadingGroupRecoveryCount(int count);
        void onReadingGroupFailureCount(int count);
        void onReadSuccess();
        void onReadError(String err);
        void onSaving(int progress);
        void onSavingMemberSuccessCount(int count);
        void onSavingMemberFailureCount(int count);
        void onSavingGroupSuccessCount(int count);
        void onSavingGroupFailureCount(int count);
        void onSaveSuccess();
        void onSaveFailure(String err);

        void onProcess(String msg);
    }

    abstract class Presenter extends BasePresenter<View>{
        abstract void setTarget  (int target  );
        abstract void setMode    (int mode    );
        abstract void setPriority(int priority);
        abstract void setPath    (int path    );
        abstract int getTarget  ();
        abstract int getMode    ();
        abstract int getPriority();
        abstract int getPath    ();
        abstract int getProgress();

        abstract void start(String path);
        abstract void read();
        abstract List<ESONObject> getReadMemberLocal();
        abstract List<ESONObject> getReadMemberRecovery();
        abstract List<Pair<ESONObject,ESONObject>> getReadMemberDuplicated();
        abstract List<ESONObject> getReadGroupLocal();
        abstract List<ESONObject> getReadGroupRecovery();
        abstract List<Pair<ESONObject,ESONObject>> getReadGroupDuplicated();
        abstract List<ESONObject> getSaveMemberSuccess();
        abstract List<ESONObject> getSaveMemberFailure();
        abstract List<ESONObject> getSaveGroupSuccess();
        abstract List<ESONObject> getSaveGroupFailure();
        abstract void saveMember(ESONObject data, ISetterListener listener);
        abstract void saveGroup(ESONObject data, ISetterListener listener);

        abstract String getDefaultVillageName();
        abstract String getDefaultTownName();
        abstract String getSelectedVillageName();
        abstract String getSelectedTownName();
        abstract String getDiyVillageName();
        abstract String getDiyTownName();
        abstract void setSelectedDatabaseId(int id);
        abstract int getSelectedDatabaseId();
        abstract void setSelectedVillageName(String name);
        abstract void setSelectedTownName(String name);
        abstract void setDiyVillageName(String name);
        abstract void setDiyTownName(String name);

        abstract List<ESONObject> getSelectDatabaseList();
        abstract int createDatabase(String townName,String villageName);

        abstract void markTransactionSuccess();
        abstract void markTransactionFailure();
        abstract void release();

    }
}
