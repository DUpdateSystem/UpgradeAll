package net.xzos.UpgradeAll;

import org.json.JSONException;
import org.json.JSONObject;
import org.litepal.LitePal;

import java.util.List;


class Updater {
    private JSONObject updateJsonData = new JSONObject(); // 存储 Updater 数据

    void refresh() {
        // TODO: 多线程刷新
        List<RepoDatabase> repoDatabase = LitePal.findAll(RepoDatabase.class);
        for (RepoDatabase upgradeItem : repoDatabase) {
            int id = upgradeItem.getId();
            String api = upgradeItem.getApi().toLowerCase();
            String api_url = upgradeItem.getApiUrl();
            HttpApi httpApi = new HttpApi();
            switch (api) {
                case "github":
                    httpApi = new GithubApi(api_url);
                    // 发达 API 请求
                    break;
            }
            JSONObject upgradeItemJson = new JSONObject();
            try {
                upgradeItemJson.put("id", id);
                upgradeItemJson.put("api", api);
                upgradeItemJson.put("api_url", api_url);
                upgradeItemJson.put("httpApi", httpApi);
                upgradeItemJson.put("database", upgradeItem);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                updateJsonData.put(String.valueOf(id), upgradeItemJson);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
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
