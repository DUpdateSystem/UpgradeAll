package net.xzos.UpgradeAll.server.log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import net.xzos.UpgradeAll.server.ServerContainer;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

class LogLiveData {
    private MutableLiveData<JSONObject> mLogJSONObject = new MutableLiveData<>(new JSONObject());

    void setLogJSONObject(JSONObject logJSONObject) {
        mLogJSONObject.postValue(logJSONObject);
    }

    LiveData<List<String>> getLiveDataLogSortList() {
        return Transformations.map(mLogJSONObject, logJsonObject -> {
            LogUtil logUtil = ServerContainer.AppServer.getLog();
            LogDataProxy logDataProxy = new LogDataProxy(logUtil);
            return logDataProxy.getLogSort();
        });
    }

    LiveData<List<String>> getLiveDataLogObjectIdList(String logSort) {
        return Transformations.map(mLogJSONObject, logJsonObject -> {
            LogUtil logUtil = ServerContainer.AppServer.getLog();
            LogDataProxy logDataProxy = new LogDataProxy(logUtil);
            return logDataProxy.getLogObjectId(logSort);
        });
    }

    LiveData<List<String>> getLiveDataLogList(String[] logObjectTag) {
        return Transformations.map(mLogJSONObject, logJsonObject -> {
            LogUtil logUtil = ServerContainer.AppServer.getLog();
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
