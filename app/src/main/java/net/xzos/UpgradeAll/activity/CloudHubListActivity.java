package net.xzos.UpgradeAll.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import net.xzos.UpgradeAll.R;
import net.xzos.UpgradeAll.gson.CloudConfig;
import net.xzos.UpgradeAll.gson.ItemCardViewExtraData;
import net.xzos.UpgradeAll.server.hub.CloudHub;
import net.xzos.UpgradeAll.ui.viewmodels.ItemCardView;
import net.xzos.UpgradeAll.ui.viewmodels.adapters.CloudHubItemAdapter;

import java.util.ArrayList;
import java.util.List;

import io.github.kobakei.materialfabspeeddial.FabSpeedDial;

public class CloudHubListActivity extends AppCompatActivity {
    private List<ItemCardView> itemCardViewList = new ArrayList<>();
    private CloudHubItemAdapter adapter;

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private CloudHub cloudHub = new CloudHub();

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
            actionBar.setTitle(getApplicationContext().getString(R.string.cloud_hub));
        }
        // 隐藏 tab
        FabSpeedDial fab = findViewById(R.id.add_fab);
        fab.setVisibility(View.GONE);

        swipeRefresh = findViewById(R.id.swipeRefresh);
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);
        swipeRefresh.setOnRefreshListener(this::refreshCardView);

        recyclerView = findViewById(R.id.update_item_recycler_view);

        setRecyclerView();
    }

    private void refreshCardView() {
        new Thread(() -> runOnUiThread(() -> {
            swipeRefresh.setRefreshing(true);
            refreshCloudHubList();
            swipeRefresh.setRefreshing(false);
        })).start();
    }

    private void refreshCloudHubList() {
        new Thread(() -> {
            boolean isSuccess = cloudHub.flashCloudConfigList();
            new Handler(Looper.getMainLooper()).post(() -> {
                if (!isSuccess) Toast.makeText(this, "网络错误", Toast.LENGTH_SHORT).show();
                List<CloudConfig.HubListBean> hubList = cloudHub.getHubList();
                itemCardViewList.clear();
                for (CloudConfig.HubListBean hubItem : hubList) {
                    String name = hubItem.getHubConfigName();
                    String configUuid = hubItem.getHubConfigUuid();
                    String configFileName = hubItem.getHubConfigFileName();
                    ItemCardViewExtraData extraData = new ItemCardViewExtraData();
                    extraData.setConfigFileName(configFileName);
                    extraData.setUuid(configUuid);
                    itemCardViewList.add(new ItemCardView.Builder(name, configUuid, configFileName).extraData(extraData).build());
                }
                itemCardViewList.add(new ItemCardView.Builder(null, null, null).build());
                setRecyclerView();
                adapter.notifyDataSetChanged();
            });
        }).start();
    }

    private void setRecyclerView() {
        GridLayoutManager layoutManager = new GridLayoutManager(this, 1);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new CloudHubItemAdapter(itemCardViewList, cloudHub);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
