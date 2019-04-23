package net.xzos.UpgradeAll;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.litepal.LitePal;

import java.util.List;
import java.util.Objects;

import static org.litepal.LitePalBase.TAG;

public class UpgradeService extends IntentService {
    public UpgradeService() {
        super("UpgradeService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        refreshData();
    }

    static void refreshData() {
        // 刷新整个数据库
        // TODO: 多线程刷新
        List<RepoDatabase> repoDatabase = LitePal.findAll(RepoDatabase.class);
        for (RepoDatabase upgraadeItem : repoDatabase) {
            int id = upgraadeItem.getId();
            String api = upgraadeItem.getApi();
            String owner = upgraadeItem.getOwner();
            String repo = upgraadeItem.getRepo();
            String databaseLatestTag = upgraadeItem.getLatestTag();
            switch (api) {
                case "github":
                    JSONObject latestReleaseJson;
                    JSONArray apiReturnJsonArray;
                    // 预设返回数据
                    String api_url = "https://api.github.com/repos/"
                            + owner + "/" + repo + "/releases";
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
}
