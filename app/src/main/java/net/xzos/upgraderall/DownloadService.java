package net.xzos.upgraderall;

import android.app.IntentService;
import android.content.Intent;

public class DownloadService extends IntentService {

    public DownloadService() {
        super("DownloadService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            String api = intent.getStringExtra("api");
            String api_url = intent.getStringExtra("api_url");
            switch (api) {
                case "github":
                    githubApi httpApi = new httpApi().GithubApi(api_url);
                    httpApi.getLatestRelease();
            }
        }
    }
}
