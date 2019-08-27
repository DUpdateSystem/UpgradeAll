package net.xzos.UpgradeAll.server.app.manager.api;

import androidx.lifecycle.MutableLiveData;

import net.xzos.UpgradeAll.R;
import net.xzos.UpgradeAll.application.MyApplication;
import net.xzos.UpgradeAll.database.RepoDatabase;
import net.xzos.UpgradeAll.server.ServerContainer;
import net.xzos.UpgradeAll.server.app.engine.api.EngineApi;
import net.xzos.UpgradeAll.server.app.engine.js.JavaScriptEngine;
import net.xzos.UpgradeAll.server.hub.HubManager;
import net.xzos.UpgradeAll.server.log.LogUtil;
import net.xzos.UpgradeAll.ui.viewmodels.componnent.EditIntPreference;

import org.json.JSONObject;
import org.litepal.LitePal;

import java.util.Calendar;


public class Updater {

    protected static final LogUtil Log = ServerContainer.AppServer.getLog();
    private static final String TAG = "Updater";
    private static final String[] LogObjectTag = {"Core", TAG};

    private int appDatabaseId;
    private EngineApi engine = EngineApi.getEmptyEngine();
    private MutableLiveData<Boolean> renewing = new MutableLiveData<>(false);

    Updater(int appDatabaseId) {
        this.appDatabaseId = appDatabaseId;
    }

    public MutableLiveData<Boolean> getRenewing() {
        return renewing;
    }

    public void renew(boolean isAuto) {
        if (isAuto) {
            autoRenew();
        } else {
            forcedRenew();
        }
    }

    /**
     * 检查刷新时间，
     * 如果时间未到，
     * 则停止刷新
     */
    private void autoRenew() {
        // 检查更新时间更新数据
        boolean startRefresh = true;
        int defaultDataExpirationTime = MyApplication.getContext().getResources().getInteger(R.integer.default_data_expiration_time);  // 默认自动刷新时间 10min
        int autoRefreshMinute = EditIntPreference.getInt("sync_time", defaultDataExpirationTime);
        Calendar updateTime;
        updateTime = engine.getRenewTime();
        if (updateTime != null) {
            updateTime.add(Calendar.MINUTE, autoRefreshMinute);
            if (Calendar.getInstance().before(updateTime)) {
                Log.v(LogObjectTag, TAG, String.format("autoRefreshAll: %s NoUp", appDatabaseId));
                startRefresh = false;
            }
        }
        if (startRefresh)
            forcedRenew();
    }

    private void forcedRenew() {
        new Thread(this::refreshThread).start();
    }

    /**
     * 刷新数据
     */
    private void refreshThread() {
        renewing.postValue(true);
        newEngine(appDatabaseId);
        engine.refreshData();
        boolean refreshSuccess = engine.isSuccessFlash();
        // 检查刷新
        if (refreshSuccess) {
            engine.setRenewTime();
            Log.v(LogObjectTag, TAG, "refreshThread: 刷新成功");
        }
        renewing.postValue(false);
    }

    private void newEngine(int databaseId) {
        // 添加一个 更新检查器追踪子项
        RepoDatabase repoDatabase = LitePal.find(RepoDatabase.class, databaseId);
        String apiUuid = repoDatabase.getApiUuid();
        Log.d(LogObjectTag, TAG, "renewUpdateItem: uuid: " + apiUuid);
        String url = repoDatabase.getUrl();
        String apiName = repoDatabase.getApi();
        String[] logObjectTag = {apiName, String.valueOf(databaseId)};
        // 查找软件源数据库
        String jsCode = HubManager.getJsCode(apiUuid);
        engine = new JavaScriptEngine.Builder(logObjectTag, url, jsCode).build();
    }

    public String getLatestVersion() {
        // 获取最新版本号
        return engine.getVersionNumber(0);
    }

    public JSONObject getLatestDownloadUrl() {
        // 获取最新下载链接
        return engine.getReleaseDownload(0);
    }
}
