package net.xzos.UpgradeAll;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.litepal.LitePalBase.TAG;

class Repo {
    static void refreshData() {
        // 刷新整个数据库
        // TODO: 多线程刷新
        List<RepoDatabase> repoDatabase = LitePal.findAll(RepoDatabase.class);
        for (RepoDatabase upgradeItem : repoDatabase) {
            int id = upgradeItem.getId();
            String api = upgradeItem.getApi();
            String api_url = upgradeItem.getApiUrl();
            String owner = upgradeItem.getOwner();
            String repo = upgradeItem.getRepo();
            String databaseLatestTag = upgradeItem.getLatestTag();
            switch (api) {
                case "github":
                    JSONObject latestReleaseJson;
                    JSONArray apiReturnJsonArray;
                    // 预设返回数据
                    Log.d(TAG, "api_url: " + api_url);
                    GithubApi httpApi = new HttpApi().githubApi(api_url);
                    // 发达 API 请求
                    latestReleaseJson = httpApi.getLatestRelease();
                    apiReturnJsonArray = httpApi.getReturnJsonArray();
                    // 获取数据
                    String lastTag = null;
                    try {
                        lastTag = latestReleaseJson.getString("tag_name");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if (!Objects.equals(lastTag, databaseLatestTag)) {
                        RepoDatabase repoToUpdate = LitePal.find(RepoDatabase.class, id);
                        repoToUpdate.setLatestTag(lastTag);
                        repoToUpdate.setApiReturnData(apiReturnJsonArray.toString());
                    }
                    // 更新数据库数据
                    break;
            }
        }
    }
    static boolean addRepoDatabase(String name, String api, String url) {
        // TODO: 可被忽略的参数
            String api_url = null;

            if (api.length() != 0 && url.length() != 0) {
                String owner = null;
                String repo = null;
                switch (api) {
                    case "github":
                        String[] temp = url.split("github\\.com");
                        temp = temp[temp.length - 1].split("/");
                        List<String> list = new ArrayList<>(Arrays.asList(temp));
                        list.removeAll(Arrays.asList("", null));
                        owner = list.get(0);
                        repo = list.get(1);
                        // 分割网址
                        api_url = "https://api.github.com/repos/"
                                + owner + "/" + repo + "/releases";
                        Log.d(TAG, "api_url: " + api_url);
                        break;
                }
                RepoDatabase repoDatabase = new RepoDatabase();
                if (name.length() == 0) {
                    name = repo;
                    // 如果未自定义名称，则使用仓库名
                }
                LitePal.deleteAll(RepoDatabase.class, "api_url = ?", api_url);
                // 删除所有数据库重复项
                repoDatabase.setName(name);
                repoDatabase.setRepo(repo);
                repoDatabase.setOwner(owner);
                repoDatabase.setApi(api);
                repoDatabase.setApiUrl(api_url);
                repoDatabase.save();
                // 将数据存入 RepoDatabase 数据库
                return true;
            }
        return false;
    }
}
