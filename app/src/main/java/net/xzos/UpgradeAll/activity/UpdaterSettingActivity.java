package net.xzos.UpgradeAll.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import net.xzos.UpgradeAll.R;
import net.xzos.UpgradeAll.data.MyApplication;
import net.xzos.UpgradeAll.database.RepoDatabase;
import net.xzos.UpgradeAll.updater.HttpApi.GithubApi;
import net.xzos.UpgradeAll.utils.VersionChecker;

import org.json.JSONException;
import org.json.JSONObject;
import org.litepal.LitePal;

public class UpdaterSettingActivity extends AppCompatActivity {

    private static final String TAG = "UpdateItemSetting";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_item_setting);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.add);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        // 获取可能来自修改设置项的请求
        Intent intentGetData = getIntent();
        int databaseId = intentGetData.getIntExtra("database_id", 0);
        if (databaseId != 0) {
            RepoDatabase database = LitePal.find(RepoDatabase.class, databaseId);
            EditText editName = findViewById(R.id.editName);
            editName.setText(database.getName());
            EditText editUrl = findViewById(R.id.editUrl);
            editUrl.setText(database.getUrl());
            Spinner apiSpinner = findViewById(R.id.api_spinner);
            switch (database.getApi().toLowerCase()) {
                case "github":
                    apiSpinner.setSelection(0);
                    break;
            }
            Spinner versionCheckSpinner = findViewById(R.id.versionCheckSpinner);
            JSONObject versionChecker = database.getVersionChecker();
            String versionCheckerApi = "";
            String versionCheckerText = "";
            String versionCheckRegular = "";
            try {
                versionCheckerApi = versionChecker.getString("api");
                versionCheckerText = versionChecker.getString("text");
                versionCheckRegular = versionChecker.getString("regular");
            } catch (JSONException e) {
                Log.e(TAG, String.format("onCreate: 数据库损坏！  versionChecker: %s", versionChecker));
            }
            switch (versionCheckerApi.toLowerCase()) {
                case "app":
                    versionCheckSpinner.setSelection(0);
                    break;
                case "magisk":
                    versionCheckSpinner.setSelection(1);
                    break;
            }
            EditText editVersionCheckText = findViewById(R.id.editVersionCheckText);
            editVersionCheckText.setText(versionCheckerText);
            EditText editVersionCheckRegular = findViewById(R.id.editVersionCheckRegular);
            if (editVersionCheckRegular.length() == 0)
                // TODO: 3个版本后删除该专门转换 (0.0.6)
                editVersionCheckRegular.setText("\\d+(\\.\\d+)*");
            else
                editVersionCheckRegular.setText(versionCheckRegular);
        }
        // 以下是按键事件
        Button versionCheckButton = findViewById(R.id.statusCheckButton);
        versionCheckButton.setOnClickListener(v -> {
            // 版本检查设置
            JSONObject versionCheckerJsonObject = getVersionChecker();
            VersionChecker versionChecker = new VersionChecker(versionCheckerJsonObject);
            String appVersion = versionChecker.getVersion();
            if (appVersion != null) {
                Toast.makeText(UpdaterSettingActivity.this, "version: " + appVersion, Toast.LENGTH_SHORT).show();
            }
        });
        ImageView helpImageView = findViewById(R.id.helpImageView);
        helpImageView.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://xzos.net/regular-expression"));
            intent = Intent.createChooser(intent, "请选择浏览器以查看帮助文档");
            startActivity(intent);
        });
        Button addButton = findViewById(R.id.addButton);
        addButton.setOnClickListener(v -> {
            EditText editName = findViewById(R.id.editName);
            EditText editUrl = findViewById(R.id.editUrl);
            String name = editName.getText().toString();
            String url = editUrl.getText().toString();
            Spinner apiSpinner = findViewById(R.id.api_spinner);
            String api = apiSpinner.getSelectedItem().toString();
            JSONObject versionChecker = getVersionChecker();
            boolean addRepoSuccess = addRepoDatabase(databaseId, name, api, url, versionChecker);
            if (addRepoSuccess) {
                MyApplication.getUpdater().renewUpdateItem(databaseId);
                // 强行刷新被修改的子项
                onBackPressed();
                // 跳转主页面
            } else {
                Toast.makeText(UpdaterSettingActivity.this, "什么？数据库添加失败！", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private JSONObject getVersionChecker() {
        // 获取versionChecker
        JSONObject versionChecker = new JSONObject();
        Spinner versionCheckSpinner = findViewById(R.id.versionCheckSpinner);
        EditText editVersionCheckText = findViewById(R.id.editVersionCheckText);
        EditText editVersionCheckRegular = findViewById(R.id.editVersionCheckRegular);
        String versionCheckerText = editVersionCheckText.getText().toString();
        String versionCheckerApi = versionCheckSpinner.getSelectedItem().toString();
        String versionCheckerRegular = editVersionCheckRegular.getText().toString();
        Log.d(TAG, "getRepoConfig:  " + versionCheckerRegular);
        switch (versionCheckerApi) {
            case "APP 版本":
                versionCheckerApi = "APP";
                break;
            case "Magisk 模块":
                versionCheckerApi = "Magisk";
                break;
        }
        try {
            versionChecker.put("api", versionCheckerApi);
            versionChecker.put("text", versionCheckerText);
            versionChecker.put("regular", versionCheckerRegular);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return versionChecker;
    }

    boolean addRepoDatabase(int databaseId, String name, String api, String url, JSONObject versionChecker) {
        // TODO: 可被忽略的参数
        if (api.length() != 0 && url.length() != 0) {

            // 数据处理
            // 判断url是否多余
            if (url.substring(url.length() - 1).equals("/")) {
                url = url.substring(0, url.length() - 1);
            }
            switch (api.toLowerCase()) {
                case "github":
                    if (name.length() == 0) name = new GithubApi(url).getDefaultName();
                    break;
            }
            // 如果未自定义名称，则使用仓库名

            // 修改数据库
            RepoDatabase repoDatabase = LitePal.find(RepoDatabase.class, databaseId);
            if (repoDatabase == null) repoDatabase = new RepoDatabase();
            // 开启数据库
            repoDatabase.setName(name);
            repoDatabase.setApi(api);
            repoDatabase.setUrl(url);
            repoDatabase.setVersionChecker(versionChecker);
            repoDatabase.save();
            // 将数据存入 RepoDatabase 数据库
            return true;
        }
        return false;
    }
}
