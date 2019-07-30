package net.xzos.UpgradeAll.ui.viewmodels.log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import net.xzos.UpgradeAll.application.MyApplication;

import java.util.List;

public class PageViewModel extends ViewModel {

    private MutableLiveData<String[]> mLogObjectTag = new MutableLiveData<>();
    private LiveData<LiveData<List<String>>> mLogList = Transformations.map(mLogObjectTag, logObjectTag -> {
        return MyApplication.getLog().getLogMessageListLiveData(logObjectTag);
    });

    void setLogObjectTag(String[] logObjectTag) {
        mLogObjectTag.setValue(logObjectTag);
    }

    LiveData<LiveData<List<String>>> getLogList() {
        return mLogList;
    }
}