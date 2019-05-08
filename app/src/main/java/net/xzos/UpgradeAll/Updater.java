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

    void refreshAll(boolean isAuto) {
        // TODO: 多线程刷新
        // 强制刷新整个数据库
        List<RepoDatabase> repoDatabase = LitePal.findAll(RepoDatabase.class);
        for (RepoDatabase upgradeItem : repoDatabase) {
            int databaseId = upgradeItem.getId();
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
                e.printStackTrace();
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
                    Log.d(TAG, "autoRefreshAll: NoUp");
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
            JSONObject upgradeItemJson = new JSONObject();
            try {
                upgradeItemJson = (JSONObject) updateJsonData.get(String.valueOf(databaseId));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            HttpApi httpApi = new HttpApi();
            try {
                httpApi = (HttpApi) upgradeItemJson.get("httpApi");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            refreshSuccess = httpApi.flashData();  // 调用刷新
            if (refreshSuccess) {
                try {
                    upgradeItemJson.put("UpdateTime", Calendar.getInstance());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } else {
            refreshSuccess = newUpgradeItem(databaseId);  //  创建 HttpApi 时会自动刷新初始数据
        }
        return refreshSuccess;
    }

    private boolean newUpgradeItem(int databaseId) {
        // 添加一个 更新检查器追踪子项
        RepoDatabase upgradeItemDatabase = LitePal.find(RepoDatabase.class, databaseId);
        String api = upgradeItemDatabase.getApi();
        String apiUrl = upgradeItemDatabase.getApiUrl();
        HttpApi httpApi = new HttpApi();
        switch (api.toLowerCase()) {
            case "github":
                httpApi = new GithubApi(apiUrl);
                // 发达 API 请求
                break;
        }
        JSONObject upgradeItemJson = new JSONObject();
        try {
            upgradeItemJson.put("httpApi", httpApi);
            upgradeItemJson.put("database", upgradeItemDatabase);
            upgradeItemJson.put("updateTime", Calendar.getInstance());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            updateJsonData.put(String.valueOf(databaseId), upgradeItemJson);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return true;
    }

    String getLatestVersion(int id) {
        // 获取最新版本号
        JSONObject upgradeItem;
        HttpApi httpApi = new HttpApi();
        try {
            upgradeItem = (JSONObject) updateJsonData.get(String.valueOf(id));
            httpApi = (HttpApi) upgradeItem.get("httpApi");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return httpApi.getVersion(0);
    }

    JSONObject getLatestDownloadUrl(int id) {
        // 获取最新下载链接
        JSONObject upgradeItem;
        HttpApi httpApi = new HttpApi();
        try {
            upgradeItem = (JSONObject) updateJsonData.get(String.valueOf(id));
            httpApi = (HttpApi) upgradeItem.get("httpApi");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return httpApi.getReleaseDownloadUrl(0);
    }

    String getInstalledVersion(int id) {
        // 获取已安装版本号
        JSONObject upgradeItem = new JSONObject();
        RepoDatabase upgradeItemDatabase = new RepoDatabase();
        try {
            upgradeItem = (JSONObject) updateJsonData.get(String.valueOf(id));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            upgradeItemDatabase = (RepoDatabase) upgradeItem.get("database");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JSONObject versionCheckerJsonObject = upgradeItemDatabase.getVersionChecker();
        // 获取数据库 VersionChecker 数据
        VersionChecker versionChecker = new VersionChecker(versionCheckerJsonObject);
        return versionChecker.getVersion();
    }
}
