package net.xzos.UpgradeAll;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import org.json.JSONException;
import org.json.JSONObject;
import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private List<UpgradeItemCard> upgradeItemCardList = new ArrayList<>();
    private UpgradeItemCardAdapter adapter;

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;

    final private Updater updater = new Updater();

    @Override
    protected void onPostResume() {
        super.onPostResume();
        refreshUpgrade();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
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
        upgradeItemCardList.clear();
        for (RepoDatabase upgradeItem : repoDatabase) {
            int id = upgradeItem.getId();
            String name = upgradeItem.getName();
            String api = upgradeItem.getApi();
            String url = upgradeItem.getUrl();
            String latestVersion = updater.getLatestVersion(id);
            String installedVersion = updater.getInstalledVersion(id);
            upgradeItemCardList.add(new UpgradeItemCard(name, installedVersion + " -> " + latestVersion, url, api));
        }
        setRecyclerView();
    }

    private void setRecyclerView() {
        GridLayoutManager layoutManager = new GridLayoutManager(this, 1);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new UpgradeItemCardAdapter(upgradeItemCardList);
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
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
