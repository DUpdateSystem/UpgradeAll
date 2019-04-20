package net.xzos.UpgradeAll;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DownloaderService extends IntentService {

    private static final String TAG = "DownloaderService";

    public DownloaderService() {
        super("DownloaderService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            JSONObject latestReleaseJson = null;
            JSONArray apiReturnJsonArray = null;
            String name = intent.getStringExtra("name");
            String api = intent.getStringExtra("api");
            String url = intent.getStringExtra("url");

            if (api.length() != 0 && url.length() != 0) {
                String[] temp = url.split("github\\.com");
                temp = temp[temp.length - 1].split("/");
                List<String> list = new ArrayList<>(Arrays.asList(temp));
                list.removeAll(Arrays.asList("", null));
                String owner = list.get(0);
                String repo = list.get(1);
                switch (api) {
                    case "github":
                        String api_url = "https://api.github.com/repos/"
                                + owner + "/" + repo + "/releases";
                        Log.d(TAG, "api_url: " + api_url);
                        githubApi httpApi = new HttpApi().GithubApi(api_url);
                        latestReleaseJson = httpApi.getLatestRelease();
                        apiReturnJsonArray = httpApi.getReturnJsonArray();
                        break;
                }
                // 获取数据
                if (latestReleaseJson != null) {
                    // 返回数据不为空
                    String tag_release = null;
                    String release_name = null;
                    RepoDatabase repoDatabase = new RepoDatabase();
                    try {
                        tag_release = latestReleaseJson.getString("tag_name");
                        release_name = latestReleaseJson.getString("name");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if (name.length() == 0) {
                        name = repo;
                        // 如果未自定义名称，则使用仓库名
                    }
                    repoDatabase.setName(name);
                    repoDatabase.setRepo(repo);
                    repoDatabase.setOwner(owner);
                    repoDatabase.setApi(api);
                    repoDatabase.setLatestTag(tag_release);
                    repoDatabase.setLatestRelease(release_name);
                    repoDatabase.setApiReturnData(apiReturnJsonArray.toString());
                    repoDatabase.save();
                    // 将数据存入 RepoDatabase 数据库
                }
            }
        }
    }
}
