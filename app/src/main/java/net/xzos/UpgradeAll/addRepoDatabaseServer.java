package net.xzos.UpgradeAll;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class addRepoDatabaseServer extends IntentService {

    private boolean addRepoSecess; // 是否完成添加操作
    private static final String TAG = "addRepoDatabaseServer";

    public addRepoDatabaseServer() {
        super("addRepoDatabaseServer");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        addRepoSecess = addRepoDatabase(intent);
    }

    static boolean addRepoDatabase(Intent intent) {
        if (intent != null) {
            String name = intent.getStringExtra("name");
            String api = intent.getStringExtra("api");
            String url = intent.getStringExtra("url");
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
        }
        return false;
    }
}
