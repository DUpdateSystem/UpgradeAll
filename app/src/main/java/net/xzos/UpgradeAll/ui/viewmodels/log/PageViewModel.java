package net.xzos.UpgradeAll.ui.viewmodels.log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import net.xzos.UpgradeAll.application.MyApplication;

import org.json.JSONException;

import java.util.ArrayList;

public class PageViewModel extends ViewModel {

    private MutableLiveData<String[]> mLogObjectTag = new MutableLiveData<>();
    private LiveData<ArrayList> mLogList = Transformations.map(mLogObjectTag, logObjectTag -> {
        ArrayList<String> arrayList;
        try {
            arrayList = (ArrayList<String>) MyApplication.getLog().getLogMessageList(logObjectTag);
        } catch (JSONException e) {
            arrayList = new ArrayList<>();
            e.printStackTrace();
        }
        return arrayList;
    });

    void setLogObjectTag(String[] logObjectTag) {
        mLogObjectTag.setValue(logObjectTag);
    }

    LiveData<ArrayList> getLogList() {
        return mLogList;
    }
}