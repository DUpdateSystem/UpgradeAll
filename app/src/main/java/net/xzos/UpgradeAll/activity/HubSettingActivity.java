package net.xzos.UpgradeAll.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import net.xzos.UpgradeAll.R;
import net.xzos.UpgradeAll.database.HubDatabase;
import net.xzos.UpgradeAll.gson.HubConfig;
import net.xzos.UpgradeAll.utils.LogUtil;

import org.litepal.LitePal;

public class HubSettingActivity extends Activity {

    private static final String TAG = "HubSettingActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hub_setting);
        setFinishOnTouchOutside(true);
        Intent intent = getIntent();
        int databaseId = intent.getIntExtra("database_id", 0);
        Button addButton = findViewById(R.id.addButton);
        EditText configEditText = findViewById(R.id.configEditText);
        addButton.setOnClickListener(v -> {
            String repoConfig = configEditText.getText().toString();
            boolean addHubSuccess = addHubDatabase(databaseId, repoConfig);
            if (addHubSuccess) {
                onBackPressed();
                Toast.makeText(HubSettingActivity.this, "数据添加成功", Toast.LENGTH_LONG).show();
            } else
                Toast.makeText(HubSettingActivity.this, "什么？数据库添加失败！", Toast.LENGTH_LONG).show();
        });
    }

    boolean addHubDatabase(int databaseId, String hubConfig) {
        // TODO: 可被忽略的参数
        Gson gson = new Gson();
        HubConfig repoConfigGson = null;
        try {
            repoConfigGson = gson.fromJson(hubConfig, HubConfig.class);
        } catch (JsonSyntaxException e) {
            LogUtil.e(TAG, "addHubDatabase:  hubConfig 不符合 JsonObject 格式 hubConfig: " + hubConfig);
        }

        // hubConfig 符合 JsonObject 格式，做进一步数据处理
        if (repoConfigGson != null) {
            String name = null;
            String uuid = null;
            try {
                name = repoConfigGson.getInfo().getConfigName();
                uuid = repoConfigGson.getUuid();
            } catch (NullPointerException e) {
                LogUtil.e(TAG, "addHubDatabase: 请确认 hubConfig 包含各个必须元素 hubConfigGson: " + hubConfig);
            }
            // 如果设置了名字与 UUID，则存入数据库
            if (name != null && uuid != null) {
                // 修改数据库
                HubDatabase hubDatabase = LitePal.find(HubDatabase.class, databaseId);
                if (hubDatabase == null) {
                    LitePal.deleteAll(HubDatabase.class, "uuid = ?", uuid);
                    hubDatabase = new HubDatabase();
                }
                // 开启数据库
                hubDatabase.setName(name);
                hubDatabase.setUuid(uuid);
                hubDatabase.setRepoConfig(repoConfigGson);
                hubDatabase.save();
                // 将数据存入 HubDatabase 数据库
                return true;
            }
        }
        return false;
    }
}
