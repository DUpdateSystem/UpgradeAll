package net.xzos.UpgradeAll;

import org.json.JSONException;
import org.json.JSONObject;
import org.litepal.LitePal;

import java.util.List;


class Updater {
    private JSONObject updateJsonData = new JSONObject(); // 存储 Updater 数据

    void refreshAll() {
        // TODO: 多线程刷新
        // 强制刷新整个数据库
        List<RepoDatabase> repoDatabase = LitePal.findAll(RepoDatabase.class);
        for (RepoDatabase upgradeItem : repoDatabase) {
            int databaseId = upgradeItem.getId();
            refresh(databaseId);
        }
    }

    boolean refresh(int databaseId) {
        /* 如果没有该子项，创建一个
          如果有，则使用
         */
        boolean refreshSuccess;
        if (updateJsonData.has(String.valueOf(databaseId))) {
            JSONObject upgradeItem = null;
            try {
                upgradeItem = (JSONObject) updateJsonData.get(String.valueOf(databaseId));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            HttpApi httpApi = new HttpApi();
            try {
                httpApi = (HttpApi) upgradeItem.get("httpApi");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            refreshSuccess = httpApi.flashData();  // 调用刷新
        } else {
            refreshSuccess = newUpgradeItem(databaseId);  //  创建 HttpApi 时会自动刷新初始数据
        }
        return refreshSuccess;
    }

    private boolean newUpgradeItem(int databaseId) {
        // 添加一个 更新检查器追踪子项
        RepoDatabase upgradeItemDatabase = LitePal.find(RepoDatabase.class, databaseId);
        String api = upgradeItemDatabase.getApi();
        String api_url = upgradeItemDatabase.getApiUrl();
        HttpApi httpApi = new HttpApi();
        switch (api.toLowerCase()) {
            case "github":
                httpApi = new GithubApi(api_url);
                // 发达 API 请求
                break;
        }
        JSONObject upgradeItemJson = new JSONObject();
        try {
            upgradeItemJson.put("id", databaseId);
            upgradeItemJson.put("api", api);
            upgradeItemJson.put("api_url", api_url);
            upgradeItemJson.put("httpApi", httpApi);
            upgradeItemJson.put("database", upgradeItemDatabase);
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
