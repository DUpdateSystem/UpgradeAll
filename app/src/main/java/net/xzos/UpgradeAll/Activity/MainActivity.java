package net.xzos.UpgradeAll.Activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import net.xzos.UpgradeAll.R;
import net.xzos.UpgradeAll.data.RepoDatabase;
import net.xzos.UpgradeAll.viewmodels.UpdateCard;
import net.xzos.UpgradeAll.adapters.UpdateItemCardAdapter;

import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private List<UpdateCard> updateCardList = new ArrayList<>();
    private UpdateItemCardAdapter adapter;

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;

    @Override
    protected void onStart() {
        super.onStart();
        refreshCardView();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // toolbar 点击事件
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
                case R.id.app_setting:
                    Intent intent1 = new Intent(MainActivity.this, SettingsActivity.class);
                    startActivity(intent1);
                    return true;
                default:
                    return false;
            }
        });
        // tab添加事件
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, UpdateItemSettingActivity.class);
            startActivity(intent);
        });

        swipeRefresh = findViewById(R.id.swipe_refresh);
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);
        swipeRefresh.setOnRefreshListener(this::refreshUpdate);

        recyclerView = findViewById(R.id.item_list_view);

        setRecyclerView();
    }

    private void refreshUpdate() {
        new Thread(() -> runOnUiThread(() -> {
            swipeRefresh.setRefreshing(true);
            refreshCardView();
            adapter.notifyDataSetChanged();
            swipeRefresh.setRefreshing(false);
        })).start();
    }

    private void refreshCardView() {
        List<RepoDatabase> repoDatabase = LitePal.findAll(RepoDatabase.class);
        updateCardList.clear();
        for (RepoDatabase updateItem : repoDatabase) {
            int databaseId = updateItem.getId();
            String name = updateItem.getName();
            String api = updateItem.getApi();
            String url = updateItem.getUrl();
            updateCardList.add(new UpdateCard(databaseId, name, url, api));
        }
        setRecyclerView();
    }

    private void setRecyclerView() {
        GridLayoutManager layoutManager = new GridLayoutManager(this, 1);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new UpdateItemCardAdapter(updateCardList);
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
        int id = item.getItemId();

        switch (id) {
            case R.id.app_help:
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://xzos.net/upgradeall-readme/"));
                intent = Intent.createChooser(intent, "请选择浏览器");
                startActivity(intent);
                return true;
            case R.id.app_setting:
                Intent intent1 = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent1);
                return true;
            case android.R.id.home:
                Log.d(TAG, "onOptionsItemSelected:  Back");
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
