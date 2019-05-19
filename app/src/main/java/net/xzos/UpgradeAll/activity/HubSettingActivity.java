package net.xzos.UpgradeAll.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import net.xzos.UpgradeAll.R;
import net.xzos.UpgradeAll.database.HubDatabase;

import org.json.JSONException;
import org.json.JSONObject;
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
            if (addHubSuccess)
                onBackPressed();
            else
                Toast.makeText(HubSettingActivity.this, "什么？数据库添加失败！", Toast.LENGTH_LONG).show();
        });
    }

    boolean addHubDatabase(int databaseId, String hubConfig) {
        // TODO: 可被忽略的参数
        JSONObject repoConfigJsonObject = null;
        try {
            repoConfigJsonObject = new JSONObject(hubConfig);
        } catch (JSONException e) {
            Log.e(TAG, "addHubDatabase:  hubConfig 不符合 JsonObject 格式 hubConfig: " + hubConfig);
        }

        // hubConfig 符合 JsonObject 格式，做进一步数据处理
        if (repoConfigJsonObject != null) {
            String name = null;
            try {
                name = repoConfigJsonObject.getString("name");
            } catch (JSONException e) {
                Log.e(TAG, "addHubDatabase: 请确认 hubConfig 包含各个必须元素 repoConfigJsonObject: " + repoConfigJsonObject);
            }
            // 如果设置了名字，则存入数据库
            if (name != null) {

                // 修改数据库
                HubDatabase hubDatabase = LitePal.find(HubDatabase.class, databaseId);
                if (hubDatabase == null) hubDatabase = new HubDatabase();
                // 开启数据库
                hubDatabase.setName(name);
                hubDatabase.setRepoConfig(repoConfigJsonObject);
                hubDatabase.save();
                // 将数据存入 HubDatabase 数据库
                return true;
            }
        }
        return false;
    }
}
