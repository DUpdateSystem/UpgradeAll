package net.xzos.upgraderall;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

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
                        break;
                }
                // 获取数据
                if (latestReleaseJson != null) {
                    String tag_name = null;
                    String release_name = null;
                    try {
                        tag_name = latestReleaseJson.getString("tag_name");
                        release_name = latestReleaseJson.getString("name");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    RepoDatabaseHelper dbHelper = new DatabaseHelper().repoDatabaseHelper(this, 1);
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    ContentValues values = new ContentValues();
                    if (name.length() != 0) {
                        values.put("name", name);
                    } else {
                        values.put("name", repo);
                    }
                    values.put("repo", repo);
                    values.put("owner", owner);
                    values.put("api", api);
                    values.put("tag_name", tag_name);
                    values.put("latest_release", release_name);
                    db.insert("Repo", null, values);
                }
                // 将数据存入 Repo 数据库
            }
        }
    }
}
