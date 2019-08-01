package net.xzos.UpgradeAll.ui.viewmodels.log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import net.xzos.UpgradeAll.application.MyApplication;
import net.xzos.UpgradeAll.server.log.LogDataProxy;

import java.util.List;

public class PageViewModel extends ViewModel {

    private MutableLiveData<String[]> mLogObjectTag = new MutableLiveData<>();
    private LiveData<LiveData<List<String>>> mLogList = Transformations.map(mLogObjectTag, logObjectTag -> new LogDataProxy(MyApplication.getServerContainer().getLog()).getLogMessageListLiveData(logObjectTag));

    void setLogObjectTag(String[] logObjectTag) {
        mLogObjectTag.setValue(logObjectTag);
    }

    LiveData<LiveData<List<String>>> getLogList() {
        return mLogList;
    }
}