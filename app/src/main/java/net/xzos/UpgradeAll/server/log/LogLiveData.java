package net.xzos.UpgradeAll.server.log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import net.xzos.UpgradeAll.application.MyApplication;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class LogLiveData {
    private MutableLiveData<JSONObject> mLogJSONObject = new MutableLiveData<>(new JSONObject());


    void setLogJSONObject(JSONObject logJSONObject) {
        mLogJSONObject.postValue(logJSONObject);
    }

    LiveData<List<String>> getLogLiveDataList(String[] logObjectTag) {
        return Transformations.map(mLogJSONObject, logJsonObject -> {
            LogUtil logUtil = MyApplication.getServerContainer().getLog();
            LogDataProxy logDataProxy = new LogDataProxy(logUtil);
            try {
                return logDataProxy.getLogMessageList(logObjectTag);
            } catch (JSONException e) {
                e.printStackTrace();
                return new ArrayList<>();
            }
        });
    }

}
