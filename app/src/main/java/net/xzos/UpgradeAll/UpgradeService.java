package net.xzos.UpgradeAll;

import android.app.IntentService;
import android.content.Intent;
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
        Repo.refreshData();
    }

}
