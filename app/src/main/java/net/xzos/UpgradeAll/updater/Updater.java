package net.xzos.UpgradeAll.updater;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import net.xzos.UpgradeAll.R;
import net.xzos.UpgradeAll.database.HubDatabase;
import net.xzos.UpgradeAll.updater.api.Api;
import net.xzos.UpgradeAll.updater.api.GithubApi;
import net.xzos.UpgradeAll.database.RepoDatabase;
import net.xzos.UpgradeAll.updater.api.WebCrawlerApi;
import net.xzos.UpgradeAll.utils.VersionChecker;
import net.xzos.UpgradeAll.data.MyApplication;

import org.json.JSONException;
import org.json.JSONObject;
import org.litepal.LitePal;

import java.util.Calendar;
import java.util.List;
import java.util.Objects;


public class Updater {
    private static final String TAG = "Updater";

    private JSONObject updateJsonData = new JSONObject(); // 存储 Updater 数据

    /*
     * updateJsonData  = {
     * databaseId = { "http_api": httpApi (Class),
     *                              "database": updateItemDatabase,
     *                              "update_time": 更新时间戳
     * }
     */

    public boolean isLatest(int databaseId) {
        VersionChecker versionChecker = getVersionChecker(databaseId);
        String latestVersion = versionChecker.getRegexMatchVersion(getLatestVersion(databaseId));
        String installedVersion = versionChecker.getRegexMatchVersion(getInstalledVersion(databaseId));
        return VersionChecker.compareVersionNumber(installedVersion, latestVersion);
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

    public void autoRefresh(int databaseId) {
        // 检查更新时间更新数据
        boolean startRefresh = true;
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext());
        int autoRefreshMinute = Integer.parseInt(Objects.requireNonNull(sharedPref.getString("sync_time", "5")));
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
                Calendar updateTime = null;
                try {
                    updateTime = (Calendar) updateItemJson.get("update_time");
                    updateTime.add(Calendar.MINUTE, autoRefreshMinute);
                } catch (JSONException e) {
                    Log.d(TAG, String.format("autoRefresh:  初始化数据刷新: %s", databaseId));
                }
                if (Calendar.getInstance().before(updateTime)) {
                    Log.d(TAG, String.format("autoRefreshAll: %s NoUp", databaseId));
                    startRefresh = false;
                }
            }
        }
        if (startRefresh)
            refresh(databaseId);
    }

    public boolean refresh(int databaseId) {
        /* 强制刷新数据
         * 如果没有该子项，创建一个
         * 如果有，则使用
         */
        boolean refreshSuccess;
        JSONObject updateItemJson;
        try {
            updateItemJson = (JSONObject) updateJsonData.get(String.valueOf(databaseId));
        } catch (JSONException e) {
            Log.d(TAG, "refresh:  更新对象初始化");
            updateItemJson = renewUpdateItem(databaseId);  //  创建更新对象
        }
        // 数据刷新
        Api api = new Api();
        try {
            api = (Api) updateItemJson.get("http_api");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        api.flashData();  // 调用刷新
        // 检查刷新
        refreshSuccess = api.isSuccessFlash();
        if (refreshSuccess) {
            try {
                Log.d(TAG, "refresh:  刷新成功");
                updateItemJson.put("update_time", Calendar.getInstance());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return refreshSuccess;
    }

    public JSONObject renewUpdateItem(int databaseId) {
        // 添加一个 更新检查器追踪子项
        RepoDatabase repoDatabase = LitePal.find(RepoDatabase.class, databaseId);
        String apiUuid = repoDatabase.getApiUuid();
        Log.d(TAG, "renewUpdateItem:  uuid: " + apiUuid);
        if (apiUuid == null) {
            repoDatabase.setApiUuid(MyApplication.getContext().getString(R.string.github_uuid));
            repoDatabase.save();
        }
        String url = repoDatabase.getUrl();
        Api httpApi = new Api();
        // 新建 Api 对象
        if (MyApplication.getContext().getString(R.string.github_uuid).equals(apiUuid)) {
            httpApi = new GithubApi(url);
        } else {
            List<HubDatabase> hubDatabase = LitePal.findAll(HubDatabase.class);
            JSONObject hubConfig = null;
            for (HubDatabase hubItem : hubDatabase) {
                if (hubItem.getUuid().equals(apiUuid)) {
                    hubConfig = hubItem.getRepoConfig();
                }
                if (hubConfig != null)
                    httpApi = new WebCrawlerApi(url, hubConfig);
            }
        }
        JSONObject updateItemJson = new JSONObject();
        try {
            updateItemJson.put("http_api", httpApi);
            updateItemJson.put("database", repoDatabase);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "autoRefresh:  json" + updateItemJson);
        updateJsonData.remove(String.valueOf(databaseId));
        try {
            updateJsonData.put(String.valueOf(databaseId), updateItemJson);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return updateItemJson;
    }

    public String getLatestVersion(int databaseId) {
        // 获取最新版本号
        JSONObject updateItem;
        Api api = new Api();
        try {
            updateItem = (JSONObject) updateJsonData.get(String.valueOf(databaseId));
            api = (Api) updateItem.get("http_api");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return api.getVersionNumber(0);
    }

    public JSONObject getLatestDownloadUrl(int databaseId) {
        // 获取最新下载链接
        JSONObject updateItem;
        Api api = new Api();
        try {
            updateItem = (JSONObject) updateJsonData.get(String.valueOf(databaseId));
            api = (Api) updateItem.get("http_api");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return api.getReleaseDownload(0);
    }

    public String getInstalledVersion(int databaseId) {
        // 获取已安装版本号
        VersionChecker versionChecker = getVersionChecker(databaseId);
        return versionChecker.getVersion();
    }

    private VersionChecker getVersionChecker(int databaseId) {
        JSONObject updateItem = new JSONObject();
        RepoDatabase repoDatabase = new RepoDatabase();
        try {
            updateItem = (JSONObject) updateJsonData.get(String.valueOf(databaseId));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            repoDatabase = (RepoDatabase) updateItem.get("database");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JSONObject versionCheckerJsonObject = repoDatabase.getVersionChecker();
        // 获取数据库 VersionChecker 数据
        return new VersionChecker(versionCheckerJsonObject);
    }
}
