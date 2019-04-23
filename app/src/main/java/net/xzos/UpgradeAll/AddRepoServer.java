package net.xzos.UpgradeAll;

import android.app.IntentService;
import android.content.Intent;

public class AddRepoServer extends IntentService {

    private boolean addRepoSecess; // 是否完成添加操作

    public AddRepoServer() {
        super("AddRepoServer");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            String name = intent.getStringExtra("name");
            String api = intent.getStringExtra("api");
            String url = intent.getStringExtra("url");
            addRepoSecess = Repo.addRepoDatabase(name, api, url);
        }
    }
}
