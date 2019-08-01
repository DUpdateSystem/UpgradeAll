package net.xzos.UpgradeAll.server.log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import net.xzos.UpgradeAll.database.RepoDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LogDataProxy {
    private LogUtil Log;
    private JSONObject logJSONObject;
    private LogLiveData logLiveData;

    public LogDataProxy(@NonNull LogUtil logUtil) {
        Log = logUtil;
        logJSONObject = logUtil.getLogJSONObject();
        logLiveData = logUtil.getLogLiveData();
    }

    public List<String> getLogSort() {
        ArrayList<String> logSortList = new ArrayList<>();
        Iterator it = logJSONObject.keys();
        while (it.hasNext()) {
            logSortList.add((String) it.next());
        }
        return logSortList;
    }

    public List<String> getLogObjectId(String logSort) {
        ArrayList<String> logObjectId = new ArrayList<>();
        JSONObject logSortJson;
        try {
            logSortJson = logJSONObject.getJSONObject(logSort);
        } catch (JSONException e) {
            logSortJson = new JSONObject();
        }
        Iterator it = logSortJson.keys();
        while (it.hasNext()) {
            logObjectId.add((String) it.next());
        }
        return logObjectId;
    }

    List<String> getLogMessageList(String[] logObjectTag) throws JSONException {
        ArrayList<String> logMessageArray = new ArrayList<>();
        String logSortString = logObjectTag[0];
        String logObjectId = logObjectTag[1];
        JSONArray logMessageJSONArray = logJSONObject.getJSONObject(logSortString).getJSONArray(logObjectId);
        int len = logMessageJSONArray.length();
        for (int i = 0; i < len; i++) {
            logMessageArray.add(logMessageJSONArray.get(i).toString());
        }
        return logMessageArray;
    }

    public LiveData<List<String>> getLogMessageListLiveData(String[] logObjectTag) {
        return logLiveData.getLogLiveDataList(logObjectTag);
    }

    public void clearLogAll() {
        logJSONObject = new JSONObject();
        Log.setLogJSONObject(logJSONObject);
        logLiveData.setLogJSONObject(logJSONObject);
    }

    public void clearLogSort(String logSort) {
        logJSONObject.remove(logSort);
        Log.setLogJSONObject(logJSONObject);
        logLiveData.setLogJSONObject(logJSONObject);
    }

    public void clearLogMessage(String[] logObjectTag) throws JSONException {
        String logSortString = logObjectTag[0];
        String logObjectId = logObjectTag[1];
        logJSONObject.getJSONObject(logSortString).remove(logObjectId);
        Log.setLogJSONObject(logJSONObject);
        logLiveData.setLogJSONObject(logJSONObject);
    }

    public String getLogAllToString() {
        List<String> sortList = getLogSort();
        StringBuilder fullLogString = new StringBuilder();
        for (String logSort : sortList) {
            fullLogString.append(getLogStringBySort(logSort));
        }
        return fullLogString.toString();
    }

    public String getLogStringBySort(String logSort) {
        StringBuilder fullLogString = new StringBuilder(logSort + "\n");
        List<String> sortDatabaseIdList = getLogObjectId(logSort);
        for (String databaseIdString : sortDatabaseIdList) {
            String logString = getLogMessageToString(new String[]{logSort, databaseIdString});
            if (logString != null)
                logString = logString.split("\n", 2)[1];
            else logString = "";
            fullLogString.append(logString).append("\n");
        }
        return fullLogString.toString();
    }

    public String getLogMessageToString(String[] logObjectTag) {
        String sort = logObjectTag[0];
        String name = "    " + getNameFromId(logObjectTag[1]);
        StringBuilder logMessageString = new StringBuilder();
        List<String> logMessageArray;
        try {
            logMessageArray = getLogMessageList(logObjectTag);
        } catch (JSONException e) {
            return null;
        }
        for (String logMessage : logMessageArray)
            logMessageString.append("        ").append(logMessage).append("\n");
        return sort + "\n" + name + "\n" + logMessageString;
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
