package net.xzos.UpgradeAll.server.app.manager;

import net.xzos.UpgradeAll.database.RepoDatabase;
import net.xzos.UpgradeAll.server.ServerContainer;
import net.xzos.UpgradeAll.server.app.manager.api.App;
import net.xzos.UpgradeAll.server.app.manager.api.Updater;
import net.xzos.UpgradeAll.server.log.LogUtil;

import org.json.JSONException;
import org.json.JSONObject;
import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AppManager {

    private LogUtil Log = ServerContainer.AppServer.getLog();
    private static final String TAG = "AppManager";
    private static final String[] LogObjectTag = {"Core", TAG};
    // TODO: 对 Core 日志页做同一父类

    private JSONObject appJson = new JSONObject(); // 存储 Updater Engine 数据

    public void refreshAll(boolean isAuto) {
        if (getAppList().isEmpty())
            initApp();
        List<App> appList = getAppList();
        for (App app : appList) {
            Updater updater = app.getUpdater();
            updater.renew(isAuto);
        }
    }

    private void initApp() {
        List<RepoDatabase> repoDatabase = LitePal.findAll(RepoDatabase.class);
        for (RepoDatabase updateItem : repoDatabase) {
            int appDatabaseId = updateItem.getId();
            setApp(appDatabaseId);
        }
    }

    public App getApp(int appDatabaseId) {
        App app = null;
        try {
            app = (App) appJson.get(String.valueOf(appDatabaseId));
        } catch (JSONException e) {
            Log.e(LogObjectTag, TAG, String.format("getEngine: updateJsonData缺少 %s 项", appDatabaseId));
        }
        return app;
    }

    public void setApp(int appDatabaseId) {
        try {
            appJson.put(String.valueOf(appDatabaseId), new App(appDatabaseId));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void delApp(int appDatabaseId) {
        // TODO: 自initApp维护，数据来源：独立 UI 数据
        appJson.remove(String.valueOf(appDatabaseId));
    }


    private List<App> getAppList() {
        ArrayList<App> appArrayList = new ArrayList<>();
        Iterator<String> keys = appJson.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            appArrayList.add(getApp(Integer.parseInt(key)));
        }
        return appArrayList;
    }
}
