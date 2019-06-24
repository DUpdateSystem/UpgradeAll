package net.xzos.UpgradeAll.viewmodels.ui.log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import net.xzos.UpgradeAll.data.MyApplication;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class PageViewModel extends ViewModel {

    private MutableLiveData<String> mURL = new MutableLiveData<>();
    private LiveData<ArrayList> mLogList = Transformations.map(mURL, URL -> {
        JSONObject logMessageJson = MyApplication.getLog().getLogMessage();
        JSONArray logMessageList;
        ArrayList<String> arrayList = new ArrayList<>();
        try {
            logMessageList = logMessageJson.getJSONArray(URL);
            if (logMessageList != null) {
                for (int i = 0; i < logMessageList.length(); i++) {
                    arrayList.add(logMessageList.getString(i));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return arrayList;
    });

    void setURL(String URL) {
        mURL.setValue(URL);
    }

    LiveData<ArrayList> getLog() {
        return mLogList;
    }
}