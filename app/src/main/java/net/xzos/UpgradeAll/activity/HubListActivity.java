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

import net.xzos.UpgradeAll.R;
import net.xzos.UpgradeAll.database.HubDatabase;
import net.xzos.UpgradeAll.gson.ItemCardViewExtraData;
import net.xzos.UpgradeAll.ui.viewmodels.ItemCardView;
import net.xzos.UpgradeAll.ui.viewmodels.adapters.HubItemAdapter;

import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.List;

import io.github.kobakei.materialfabspeeddial.FabSpeedDial;

public class HubListActivity extends AppCompatActivity {
    private List<ItemCardView> itemCardViewList = new ArrayList<>();
    private HubItemAdapter adapter;

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;

    @Override
    protected void onResume() {
        super.onResume();
        refreshHubList();
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
        FabSpeedDial fab = findViewById(R.id.addFab);
        fab.addOnMenuItemClickListener((floatingActionButton, textView, integer) -> {
            if (floatingActionButton == fab.getMiniFab(0)) {
                startActivity(new Intent(HubListActivity.this, HubLocalActivity.class));
            } else if (floatingActionButton == fab.getMiniFab(1)) {
                startActivity(new Intent(HubListActivity.this, CloudHubListActivity.class));
            }
            return null;
        });

        swipeRefresh = findViewById(R.id.swipeRefresh);
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);
        swipeRefresh.setOnRefreshListener(this::refreshCardView);

        recyclerView = findViewById(R.id.update_item_recycler_view);

        setRecyclerView();
    }

    private void refreshCardView() {
        new Thread(() -> runOnUiThread(() -> {
            swipeRefresh.setRefreshing(true);
            refreshHubList();
            swipeRefresh.setRefreshing(false);
        })).start();
    }

    private void refreshHubList() {
        List<HubDatabase> hubDatabase = LitePal.findAll(HubDatabase.class);
        itemCardViewList.clear();
        for (HubDatabase hubItem : hubDatabase) {
            int databaseId = hubItem.getId();
            String name = hubItem.getName();
            String uuid = hubItem.getUuid();
            ItemCardViewExtraData extraData = new ItemCardViewExtraData();
            extraData.setDatabaseId(databaseId);
            itemCardViewList.add(new ItemCardView.Builder(name, uuid, "").extraData(extraData).build());
        }
        itemCardViewList.add(new ItemCardView.Builder(null, null, null).build());
        setRecyclerView();
        adapter.notifyDataSetChanged();
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
        getMenuInflater().inflate(R.menu.menu_actionbar_hub, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.app_help:
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://xzos.net/the-customizing-configuration-rules-for-a-software-depot/"));
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
