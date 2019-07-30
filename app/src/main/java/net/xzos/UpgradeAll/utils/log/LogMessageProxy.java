package net.xzos.UpgradeAll.utils.log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import net.xzos.UpgradeAll.application.MyApplication;
import net.xzos.UpgradeAll.database.RepoDatabase;

import org.json.JSONException;
import org.json.JSONObject;
import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.List;

public class LogMessageProxy {
    private static final String TAG = "LogMessageProxy";
    private MutableLiveData<JSONObject> mLogJSONObject = new MutableLiveData<>();


    LogMessageProxy(JSONObject logJSONObject) {
        mLogJSONObject.setValue(logJSONObject);
    }

    void setLogJSONObject(JSONObject logJSONObject) {
        mLogJSONObject.postValue(logJSONObject);
    }

    LiveData<List<String>> getLogList(String[] logObjectTag) {
        LiveData<List<String>> mLogList = Transformations.map(mLogJSONObject, logJsonObject -> {
            try {
                return MyApplication.getLog().getLogMessageList(logObjectTag);
            } catch (JSONException e) {
                e.printStackTrace();
                return new ArrayList<>();
            }
        });
        return mLogList;
    }

    public static String getNameFromId(@NonNull String databaseIdString) {
        String name;
        try {
            RepoDatabase repoDatabase = LitePal.find(RepoDatabase.class, Integer.parseInt(databaseIdString));
            name = repoDatabase.getName();
        } catch (Throwable e) {
            name = databaseIdString;
        }
        return name;
    }
}
