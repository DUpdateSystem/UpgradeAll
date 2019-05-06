package net.xzos.UpgradeAll;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private List<UpgradeCard> upgradeCardList = new ArrayList<>();
    private UpgradeItemCardAdapter adapter;

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;

    final private Updater updater = MyApplication.getUpdater();

    @Override
    protected void onStart() {
        super.onStart();
        refreshUpgrade();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.app_help:
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("https://xzos.net/upgradeall-readme/"));
                    intent = Intent.createChooser(intent, "请选择浏览器");
                    startActivity(intent);
                    return true;
                default:
                    return false;
            }
        });
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, UpgradeItemSettingActivity.class);
            startActivity(intent);
        });

        swipeRefresh = findViewById(R.id.swipe_refresh);
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);
        swipeRefresh.setOnRefreshListener(this::refreshUpgrade);

        recyclerView = findViewById(R.id.item_list_view);

        setRecyclerView();
    }

    private void refreshUpgrade() {
        new Thread(() -> {
            runOnUiThread(() -> {
                swipeRefresh.setRefreshing(true);
            });
            updater.refresh();
            runOnUiThread(() -> {
                refreshCardView();
                adapter.notifyDataSetChanged();
                swipeRefresh.setRefreshing(false);
            });
        }).start();
    }

    private void refreshCardView() {
        List<RepoDatabase> repoDatabase = LitePal.findAll(RepoDatabase.class);
        upgradeCardList.clear();
        for (RepoDatabase upgradeItem : repoDatabase) {
            int databaseId = upgradeItem.getId();
            String name = upgradeItem.getName();
            String api = upgradeItem.getApi();
            String url = upgradeItem.getUrl();
            String latestVersion = updater.getLatestVersion(databaseId);
            String installedVersion = updater.getInstalledVersion(databaseId);
            upgradeCardList.add(new UpgradeCard(databaseId, name, installedVersion + " -> " + latestVersion, url, api));
        }
        setRecyclerView();
    }

    private void setRecyclerView() {
        GridLayoutManager layoutManager = new GridLayoutManager(this, 1);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new UpgradeItemCardAdapter(upgradeCardList);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.app_help) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
