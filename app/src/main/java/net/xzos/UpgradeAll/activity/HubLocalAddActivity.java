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
import net.xzos.UpgradeAll.data.MyApplication;
import net.xzos.UpgradeAll.database.HubDatabase;
import net.xzos.UpgradeAll.gson.HubConfig;
import net.xzos.UpgradeAll.utils.LogUtil;

import org.litepal.LitePal;

public class HubLocalAddActivity extends Activity {

    private static final LogUtil Log = MyApplication.getLog();
    private static final String TAG = "HubLocalAddActivity";

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
            String hubConfig = configEditText.getText().toString();

            // 字符串转 Gson
            Gson gson = new Gson();
            HubConfig hubConfigGson = null;
            try {
                hubConfigGson = gson.fromJson(hubConfig, HubConfig.class);
            } catch (JsonSyntaxException e) {
                Log.e(TAG, TAG, "addHubDatabase:  hubConfig 不符合 JsonObject 格式 hubConfig: " + hubConfig);
            }

            // hubConfig 符合 JsonObject 格式，做进一步数据处理
            boolean addHubSuccess;
            if (hubConfigGson != null)
                addHubSuccess = addHubDatabase(databaseId, hubConfigGson);
            else
                addHubSuccess = false;

            if (addHubSuccess) {
                onBackPressed();
                Toast.makeText(HubLocalAddActivity.this, "数据添加成功", Toast.LENGTH_LONG).show();
            } else
                Toast.makeText(HubLocalAddActivity.this, "什么？数据库添加失败！", Toast.LENGTH_LONG).show();
        });
    }

    public static boolean addHubDatabase(int databaseId, HubConfig hubConfigGson) {
        if (hubConfigGson != null) {
            String name = null;
            String uuid = null;
            try {
                name = hubConfigGson.getInfo().getConfigName();
                uuid = hubConfigGson.getUuid();
            } catch (NullPointerException e) {
                Gson gson = new Gson();
                Log.e(TAG, TAG, "addHubDatabase: 请确认 hubConfig 包含各个必须元素 hubConfigGson: " + gson.toJson(hubConfigGson));
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
                hubDatabase.setRepoConfig(hubConfigGson);
                hubDatabase.save();
                // 将数据存入 HubDatabase 数据库
                return true;
            }
        }
        return false;
    }
}
