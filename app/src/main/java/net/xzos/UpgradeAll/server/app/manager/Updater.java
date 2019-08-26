package net.xzos.UpgradeAll.server.app.manager;

import net.xzos.UpgradeAll.R;
import net.xzos.UpgradeAll.application.MyApplication;
import net.xzos.UpgradeAll.database.HubDatabase;
import net.xzos.UpgradeAll.database.RepoDatabase;
import net.xzos.UpgradeAll.server.app.engine.api.EngineApi;
import net.xzos.UpgradeAll.server.app.engine.js.JavaScriptEngine;
import net.xzos.UpgradeAll.server.log.LogUtil;
import net.xzos.UpgradeAll.ui.viewmodels.componnent.EditIntPreference;

import org.json.JSONException;
import org.json.JSONObject;
import org.litepal.LitePal;

import java.util.Calendar;
import java.util.List;


public class Updater {

    private LogUtil Log;
    private static final String TAG = "Updater";
    private static final String[] LogObjectTag = {"Core", TAG};

    Updater(LogUtil Log) {
        this.Log = Log;
    }

    private JSONObject updateJsonData = new JSONObject(); // 存储 Updater Engine 数据

    /*
     * updateJsonData  = [
     * databaseId = EngineApi, ...
     * ]
     */

    public void refreshAll(boolean isAuto) {
        // TODO: 多线程刷新
        // 强制刷新整个数据库
        List<RepoDatabase> repoDatabase = LitePal.findAll(RepoDatabase.class);
        for (RepoDatabase updateItem : repoDatabase) {
            int databaseId = updateItem.getId();
            if (isAuto)
                autoRenew(databaseId);
            else
                refresh(databaseId);
        }
    }

    public Thread autoRenew(int databaseId) {
        // 检查更新时间更新数据
        Thread renewThread = null;
        boolean startRefresh = true;
        int defaultDataExpirationTime = MyApplication.getContext().getResources().getInteger(R.integer.default_data_expiration_time);  // 默认自动刷新时间 10min
        int autoRefreshMinute = EditIntPreference.getInt("sync_time", defaultDataExpirationTime);
        if (updateJsonData.has(String.valueOf(databaseId))) {
            /* 如果存在数据，
             * 检查刷新时间，
             * 如果时间未到，
             * 则停止刷新
             */
            EngineApi engine = getEngine(databaseId);
            Calendar updateTime;
            updateTime = engine.getRenewTime();
            updateTime.add(Calendar.MINUTE, autoRefreshMinute);
            if (Calendar.getInstance().before(updateTime)) {
                Log.v(LogObjectTag, TAG, String.format("autoRefreshAll: %s NoUp", databaseId));
                startRefresh = false;
            }
        }
        if (startRefresh)
            renewThread = refresh(databaseId);
        return renewThread;
    }

    public Thread refresh(int databaseId) {
        Thread thread = new Thread(() -> refreshThread(databaseId));
        thread.start();
        return thread;
    }

    private void refreshThread(int databaseId) {
        /* 强制刷新数据
         * 如果没有该子项，创建一个
         * 如果有，则使用
         */
        if (!updateJsonData.has(String.valueOf(databaseId)))
            renewUpdateItem(databaseId);
        EngineApi javaScriptJEngine = getEngine(databaseId);
        javaScriptJEngine.refreshData();
        boolean refreshSuccess = javaScriptJEngine.isSuccessFlash();
        // 检查刷新
        if (refreshSuccess) {
            javaScriptJEngine.setRenewTime();
            Log.v(LogObjectTag, TAG, "refresh: 刷新成功");
        }
    }

    public void renewUpdateItem(int databaseId) {
        // 添加一个 更新检查器追踪子项
        RepoDatabase repoDatabase = LitePal.find(RepoDatabase.class, databaseId);
        String apiUuid = repoDatabase.getApiUuid();
        Log.d(LogObjectTag, TAG, "renewUpdateItem: uuid: " + apiUuid);
        String url = repoDatabase.getUrl();
        String apiName = repoDatabase.getApi();
        // 新建 EngineApi 对象
        List<HubDatabase> hubDatabase = LitePal.findAll(HubDatabase.class);
        EngineApi javaScriptEngine = null;
        for (HubDatabase hubItem : hubDatabase) {
            if (hubItem.getUuid().equals(apiUuid)) {
                String jsCode = hubItem.getExtraData().getJavascript();
                String[] logObjectTag = {apiName, String.valueOf(databaseId)};
                javaScriptEngine = new JavaScriptEngine.Builder(logObjectTag, url, jsCode).build();
                break;
            }
        }
        // 添加一个更新子项
        setEngine(databaseId, javaScriptEngine);
    }

    public String getLatestVersion(int databaseId) {
        // 获取最新版本号
        EngineApi engine = getEngine(databaseId);
        return engine.getVersionNumber(0);
    }

    public JSONObject getLatestDownloadUrl(int databaseId) {
        // 获取最新下载链接
        EngineApi engine = getEngine(databaseId);
        return engine.getReleaseDownload(0);
    }

    private void setEngine(int databaseId, EngineApi engine) {
        try {
            updateJsonData.put(String.valueOf(databaseId), engine);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private EngineApi getEngine(int databaseId) {
        EngineApi engine = EngineApi.getEmptyEngine();
        try {
            engine = (EngineApi) updateJsonData.get(String.valueOf(databaseId));
        } catch (JSONException e) {
            Log.e(LogObjectTag, TAG, String.format("getEngine: updateJsonData缺少 %s 项", databaseId));
        }
        return engine;
    }
}
