package net.xzos.UpgradeAll.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import net.xzos.UpgradeAll.R;
import net.xzos.UpgradeAll.adapters.HubItemAdapter;
import net.xzos.UpgradeAll.database.HubDatabase;
import net.xzos.UpgradeAll.viewmodels.ItemCardView;

import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.List;

public class HubListActivity extends AppCompatActivity {
    private List<ItemCardView> itemCardViewList = new ArrayList<>();
    private HubItemAdapter adapter;

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;

    @Override
    protected void onPostResume() {
        super.onPostResume();
        refreshCardView();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hub);
        // toolbar 点击事件
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        // tab添加事件
        FloatingActionButton fab = findViewById(R.id.addFab);
        fab.setOnClickListener(view -> startActivity(new Intent(HubListActivity.this, HubSettingActivity.class)));

        swipeRefresh = findViewById(R.id.swipeRefresh);
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);
        swipeRefresh.setOnRefreshListener(this::refreshUpdate);

        recyclerView = findViewById(R.id.updateItemRecyclerView);

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
        List<HubDatabase> hubDatabase = LitePal.findAll(HubDatabase.class);
        itemCardViewList.clear();
        for (HubDatabase updateItem : hubDatabase) {
            int databaseId = updateItem.getId();
            String name = updateItem.getName();
            itemCardViewList.add(new ItemCardView(databaseId, name, "", ""));
        }
        setRecyclerView();
    }

    private void setRecyclerView() {
        GridLayoutManager layoutManager = new GridLayoutManager(this, 1);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new HubItemAdapter(itemCardViewList);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_repo, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.app_help:
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://xzos.net/upgradeall-readme/"));  //TODO: 自定义源帮助文档
                intent = Intent.createChooser(intent, "请选择浏览器以查看帮助文档");
                startActivity(intent);
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
