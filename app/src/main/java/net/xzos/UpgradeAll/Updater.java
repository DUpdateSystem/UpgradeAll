package net.xzos.UpgradeAll;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.litepal.LitePal;

import java.util.Calendar;
import java.util.List;
import java.util.Objects;


class Updater {
    private static final String TAG = "Updater";

    private JSONObject updateJsonData = new JSONObject(); // 存储 Updater 数据

    /*
     * updateJsonData  = {
     * databaseId = { "httpApi": httpApi (Class),
     *                              "database": updateItemDatabase,
     *                              "updateTime": 更新时间戳
     * }
     */

    boolean isLatest(int databaseId) {
        VersionChecker versionChecker = getVersionChecker(databaseId);
        String installedVersion = versionChecker.getRegexMatchVersion(getInstalledVersion(databaseId));
        String latestVersion = versionChecker.getRegexMatchVersion(getLatestVersion(databaseId));
        if (installedVersion.length() != 0 && latestVersion.length() != 0) {
            return installedVersion.equals(latestVersion);
        }
        return false;
    }

    void refreshAll(boolean isAuto) {
        // TODO: 多线程刷新
        // 强制刷新整个数据库
        List<RepoDatabase> repoDatabase = LitePal.findAll(RepoDatabase.class);
        for (RepoDatabase updateItem : repoDatabase) {
            int databaseId = updateItem.getId();
            if (isAuto)
                autoRefresh(databaseId);
            else
                refresh(databaseId);
        }
    }

    void autoRefresh(int databaseId) {
        // 检查更新时间更新数据
        boolean startRefresh = true;
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext());
        int autoRefreshMinute = Integer.parseInt(Objects.requireNonNull(sharedPref.getString("sync_time", "10")));
        // 默认自动刷新时间 5min
        if (updateJsonData.has(String.valueOf(databaseId))) {
            /* 如果存在数据，
             * 检查刷新时间，
             * 如果时间未到，
             * 则停止刷新
             */
            JSONObject updateItemJson = new JSONObject();
            try {
                updateItemJson = (JSONObject) updateJsonData.get(String.valueOf(databaseId));
            } catch (JSONException e) {
                Log.d(TAG, String.format("autoRefresh:  updateJsonData缺少 %s 项", databaseId));
            }
            if (updateItemJson.length() != 0) {
                Calendar updateTime = Calendar.getInstance();
                try {
                    updateTime = (Calendar) updateItemJson.get("updateTime");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                updateTime.add(Calendar.MINUTE, autoRefreshMinute);
                if (Calendar.getInstance().before(updateTime)) {
                    Log.d(TAG, String.format("autoRefreshAll: %s NoUp", databaseId));
                    startRefresh = false;
                }
            }
        }
        if (startRefresh)
            refresh(databaseId);
    }

    boolean refresh(int databaseId) {
        /* 强制刷新数据
         * 如果没有该子项，创建一个
         * 如果有，则使用
         */
        boolean refreshSuccess;
        if (updateJsonData.has(String.valueOf(databaseId))) {
            JSONObject updateItemJson = new JSONObject();
            try {
                updateItemJson = (JSONObject) updateJsonData.get(String.valueOf(databaseId));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            HttpApi httpApi = new HttpApi();
            try {
                httpApi = (HttpApi) updateItemJson.get("httpApi");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            refreshSuccess = httpApi.flashData();  // 调用刷新
            if (refreshSuccess) {
                try {
                    updateItemJson.put("UpdateTime", Calendar.getInstance());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } else {
            refreshSuccess = newUpdateItem(databaseId);  //  创建 HttpApi 时会自动刷新初始数据
        }
        return refreshSuccess;
    }

    private boolean newUpdateItem(int databaseId) {
        // 添加一个 更新检查器追踪子项
        RepoDatabase updateItemDatabase = LitePal.find(RepoDatabase.class, databaseId);
        String api = updateItemDatabase.getApi();
        String apiUrl = updateItemDatabase.getApiUrl();
        HttpApi httpApi = new HttpApi();
        switch (api.toLowerCase()) {
            case "github":
                httpApi = new GithubApi(apiUrl);
                // 发达 API 请求
                break;
        }
        JSONObject updateItemJson = new JSONObject();
        try {
            updateItemJson.put("httpApi", httpApi);
            updateItemJson.put("database", updateItemDatabase);
            updateItemJson.put("updateTime", Calendar.getInstance());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            updateJsonData.put(String.valueOf(databaseId), updateItemJson);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return true;
    }

    String getLatestVersion(int databaseId) {
        // 获取最新版本号
        JSONObject updateItem;
        HttpApi httpApi = new HttpApi();
        try {
            updateItem = (JSONObject) updateJsonData.get(String.valueOf(databaseId));
            httpApi = (HttpApi) updateItem.get("httpApi");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return httpApi.getVersion(0);
    }

    JSONObject getLatestDownloadUrl(int databaseId) {
        // 获取最新下载链接
        JSONObject updateItem;
        HttpApi httpApi = new HttpApi();
        try {
            updateItem = (JSONObject) updateJsonData.get(String.valueOf(databaseId));
            httpApi = (HttpApi) updateItem.get("httpApi");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return httpApi.getReleaseDownloadUrl(0);
    }

    String getInstalledVersion(int databaseId) {
        // 获取已安装版本号
        VersionChecker versionChecker = getVersionChecker(databaseId);
        return versionChecker.getVersion();
    }

    private VersionChecker getVersionChecker(int databaseId) {
        JSONObject updateItem = new JSONObject();
        RepoDatabase updateItemDatabase = new RepoDatabase();
        try {
            updateItem = (JSONObject) updateJsonData.get(String.valueOf(databaseId));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            updateItemDatabase = (RepoDatabase) updateItem.get("database");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JSONObject versionCheckerJsonObject = updateItemDatabase.getVersionChecker();
        // 获取数据库 VersionChecker 数据
        return new VersionChecker(versionCheckerJsonObject);
    }
}
