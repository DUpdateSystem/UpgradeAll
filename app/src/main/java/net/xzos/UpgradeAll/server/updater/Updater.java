package net.xzos.UpgradeAll.server.updater;

import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import net.xzos.UpgradeAll.R;
import net.xzos.UpgradeAll.application.MyApplication;
import net.xzos.UpgradeAll.database.HubDatabase;
import net.xzos.UpgradeAll.database.RepoDatabase;
import net.xzos.UpgradeAll.server.JSEngine.JSEngineDataProxy;
import net.xzos.UpgradeAll.server.JSEngine.JavaScriptJEngine;
import net.xzos.UpgradeAll.server.JSEngine.api.Api;
import net.xzos.UpgradeAll.utils.LogUtil;
import net.xzos.UpgradeAll.utils.VersionChecker;

import org.json.JSONException;
import org.json.JSONObject;
import org.litepal.LitePal;

import java.util.Calendar;
import java.util.List;
import java.util.Objects;


public class Updater {

    private static final LogUtil Log = MyApplication.getLog();
    private static final String TAG = "Updater";
    private static final String[] LogObjectTag = {"Core", TAG};

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
        Integer defaultDataExpirationTime = MyApplication.getContext().getResources().getInteger(R.integer.default_data_expiration_time);
        int autoRefreshMinute = Integer.parseInt(Objects.requireNonNull(sharedPref.getString("sync_time", String.valueOf(defaultDataExpirationTime))));
        // 默认自动刷新时间 10min
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
                Log.e(LogObjectTag, TAG, String.format("autoRefresh:  updateJsonData缺少 %s 项", databaseId));
            }
            if (updateItemJson.length() != 0) {
                Calendar updateTime = null;
                try {
                    updateTime = (Calendar) updateItemJson.get("update_time");
                    updateTime.add(Calendar.MINUTE, autoRefreshMinute);
                } catch (JSONException e) {
                    Log.v(LogObjectTag, TAG, String.format("autoRefresh:  初始化数据刷新: %s", databaseId));
                }
                if (Calendar.getInstance().before(updateTime)) {
                    Log.v(LogObjectTag, TAG, String.format("autoRefreshAll: %s NoUp", databaseId));
                    startRefresh = false;
                }
            }
        }
        if (startRefresh)
            refresh(databaseId);
    }

    public void refresh(int databaseId) {
        /* 强制刷新数据
         * 如果没有该子项，创建一个
         * 如果有，则使用
         */
        boolean refreshSuccess;
        JSONObject updateItemJson;
        try {
            updateItemJson = (JSONObject) updateJsonData.get(String.valueOf(databaseId));
        } catch (JSONException e) {
            Log.v(LogObjectTag, TAG, "refresh:  更新对象初始化");
            updateItemJson = renewUpdateItem(databaseId);  //  创建更新对象
        }
        // 数据刷新
        Api api = new Api();
        try {
            api = (Api) updateItemJson.get("http_api");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        api.refreshData();  // 调用刷新
        // 检查刷新
        refreshSuccess = api.isSuccessFlash();
        if (refreshSuccess) {
            try {
                Log.v(LogObjectTag, TAG, "refresh:  刷新成功");
                updateItemJson.put("update_time", Calendar.getInstance());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public JSONObject renewUpdateItem(int databaseId) {
        // 添加一个 更新检查器追踪子项
        RepoDatabase repoDatabase = LitePal.find(RepoDatabase.class, databaseId);
        String apiUuid = repoDatabase.getApiUuid();
        Log.d(LogObjectTag, TAG, "renewUpdateItem: uuid: " + apiUuid);
        String url = repoDatabase.getUrl();
        String apiName = repoDatabase.getApi();
        Api httpApi = new Api();
        // 新建 Api 对象
        List<HubDatabase> hubDatabase = LitePal.findAll(HubDatabase.class);
        String jsCode = null;
        for (HubDatabase hubItem : hubDatabase) {
            if (hubItem.getUuid().equals(apiUuid)) {
                try {
                    jsCode = hubItem.getExtraData().getString("javascript");
                } catch (NullPointerException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            String[] logObjectTag = {apiName, String.valueOf(databaseId)};
            JavaScriptJEngine javaScriptJEngine = new JavaScriptJEngine(logObjectTag, url, jsCode);
            httpApi = new JSEngineDataProxy(javaScriptJEngine);
        }
        // 组装一个更新子项
        JSONObject updateItemJson = new JSONObject();
        try {
            updateItemJson.put("http_api", httpApi);
            updateItemJson.put("database", repoDatabase);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d(LogObjectTag, TAG, "renewUpdateItem:  json: " + updateItemJson);
        updateJsonData.remove(String.valueOf(databaseId));
        // 添加一个更新子项
        try {
            updateJsonData.put(String.valueOf(databaseId), updateItemJson);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return updateItemJson;
    }

    public String getLatestVersion(int databaseId) {
        // 获取最新版本号
        JSONObject updateItemJson;
        Api api = new Api();
        try {
            updateItemJson = (JSONObject) updateJsonData.get(String.valueOf(databaseId));
            api = (Api) updateItemJson.get("http_api");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return api.getVersionNumber(0);
    }

    public JSONObject getLatestDownloadUrl(int databaseId) {
        // 获取最新下载链接
        JSONObject updateItemJson;
        Api api = new Api();
        try {
            updateItemJson = (JSONObject) updateJsonData.get(String.valueOf(databaseId));
            api = (Api) updateItemJson.get("http_api");
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
        JSONObject updateItemJson = new JSONObject();
        RepoDatabase repoDatabase = new RepoDatabase();
        try {
            updateItemJson = (JSONObject) updateJsonData.get(String.valueOf(databaseId));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            repoDatabase = (RepoDatabase) updateItemJson.get("database");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JSONObject versionCheckerJsonObject = repoDatabase.getVersionChecker();
        // 获取数据库 VersionChecker 数据
        return new VersionChecker(versionCheckerJsonObject);
    }
}
