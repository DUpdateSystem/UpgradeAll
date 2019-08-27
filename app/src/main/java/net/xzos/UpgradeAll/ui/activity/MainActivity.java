package net.xzos.UpgradeAll.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.navigation.NavigationView;
import com.yalantis.ucrop.UCrop;

import net.xzos.UpgradeAll.R;
import net.xzos.UpgradeAll.application.MyApplication;
import net.xzos.UpgradeAll.database.RepoDatabase;
import net.xzos.UpgradeAll.json.cache.ItemCardViewExtraData;
import net.xzos.UpgradeAll.server.ServerContainer;
import net.xzos.UpgradeAll.server.log.LogUtil;
import net.xzos.UpgradeAll.ui.viewmodels.adapters.AppItemAdapter;
import net.xzos.UpgradeAll.ui.viewmodels.view.ItemCardView;
import net.xzos.UpgradeAll.utils.FileUtil;

import org.litepal.LitePal;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final LogUtil Log = ServerContainer.AppServer.getLog();
    private static final String TAG = "MainActivity";
    private static final String[] LogObjectTag = {"Core", TAG};

    private static final String NAV_IMAGE_FILE_NAME = "nav_image.png";
    private static File NAV_IMAGE_FILE = new File(new File(MyApplication.getContext().getFilesDir(), "images"), NAV_IMAGE_FILE_NAME);

    private static final int PERMISSIONS_REQUEST_WRITE_CONTACTS = 1;
    private final int READ_PIC_REQUEST_CODE = 2;

    private boolean enableRenew = true;

    private List<ItemCardView> itemCardViewList = new ArrayList<>();

    private DrawerLayout mDrawerLayout;
    private NavigationView navView;
    private ImageView navViewHeaderImageView;
    private AppItemAdapter adapter;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case READ_PIC_REQUEST_CODE:
                    Uri uri = resultData.getData();
                    if (uri != null) {
                        File parent = NAV_IMAGE_FILE.getParentFile();
                        if (parent != null && !parent.exists()) {
                            parent.mkdirs();
                        }
                        Uri destinationUri = Uri.fromFile(NAV_IMAGE_FILE);
                        UCrop.of(uri, destinationUri)
                                .withAspectRatio(16, 9)
                                .start(this, UCrop.REQUEST_CROP);
                    }
                    break;
                case UCrop.REQUEST_CROP:
                    renewNavImage();
                    break;
                case UCrop.RESULT_ERROR:
                    Throwable cropError = UCrop.getError(resultData);
                    if (cropError != null)
                        Log.e(LogObjectTag, TAG, "onActivityResult: 图片裁剪错误: " + cropError.toString());
                    break;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_WRITE_CONTACTS) {
            if (grantResults.length <= 0
                    || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, "设置背景图片需要读写本地文件", Toast.LENGTH_LONG).show();
            } else
                setNavImage();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (enableRenew) {
            refreshCardView();
            enableRenew = false;
        }
        navView.setCheckedItem(R.id.app_list);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        recyclerView = findViewById(R.id.update_item_recycler_view);
        mDrawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.nav_view);
        LinearLayout headerView = (LinearLayout) navView.getHeaderView(0);
        navViewHeaderImageView = headerView.findViewById(R.id.nav_header_imageView);
        headerView.setOnClickListener(view -> {
            Toast.makeText(MainActivity.this, "长按侧滑栏图片可以删除图片", Toast.LENGTH_SHORT).show();
            setNavImage();
        });
        headerView.setOnLongClickListener(view -> {
            delNavImage();
            return true;
        });
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);
        swipeRefresh.setOnRefreshListener(this::refreshCardView);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mDrawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navView.setNavigationItemSelectedListener(this);
        navViewHeaderImageView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                navViewHeaderImageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) navViewHeaderImageView.getLayoutParams();
                layoutParams.height = navViewHeaderImageView.getWidth() / 16 * 9;
                navViewHeaderImageView.setLayoutParams(layoutParams);
                renewNavImage();
            }
        });
        setRecyclerView();
        deleteFile(NAV_IMAGE_FILE_NAME);
    }

    private void setNavImage() {
        if (FileUtil.requestPermission(this, PERMISSIONS_REQUEST_WRITE_CONTACTS)) {
            FileUtil.getPicFormGallery(this, READ_PIC_REQUEST_CODE);
        }
    }

    private void renewNavImage() {
        Glide.with(this)
                .load(NAV_IMAGE_FILE)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(navViewHeaderImageView);
    }

    private void delNavImage() {
        NAV_IMAGE_FILE.delete();
        navViewHeaderImageView.setImageDrawable(null);
    }

    private void refreshCardView() {
        new Thread(() -> runOnUiThread(() -> {
            swipeRefresh.setRefreshing(true);
            refreshAppList();
            ServerContainer.AppServer.getAppManager().refreshAll(true);
            swipeRefresh.setRefreshing(false);
        })).start();
    }

    private void refreshAppList() {
        List<RepoDatabase> repoDatabase = LitePal.findAll(RepoDatabase.class);
        itemCardViewList.clear();
        for (RepoDatabase updateItem : repoDatabase) {
            int databaseId = updateItem.getId();
            String name = updateItem.getName();
            String api = updateItem.getApi();
            String url = updateItem.getUrl();
            itemCardViewList.add(new ItemCardView.Builder(name, url, api)
                    .extraData(new ItemCardViewExtraData.Builder().databaseId(databaseId).build())
                    .build());
        }
        TextView guidelinesTextView = findViewById(R.id.guidelinesTextView);
        if (itemCardViewList.size() != 0) {
            itemCardViewList.add(new ItemCardView.Builder(null, null, null)
                    .extraData(new ItemCardViewExtraData.Builder().isEmpty(true).build())
                    .build());
            setRecyclerView();
            adapter.notifyDataSetChanged();
            guidelinesTextView.setVisibility(View.GONE);
        } else {
            guidelinesTextView.setVisibility(View.VISIBLE);
        }
    }

    private void setRecyclerView() {
        GridLayoutManager layoutManager = new GridLayoutManager(this, 1);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new AppItemAdapter(this, itemCardViewList);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_actionbar_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Intent intent;

        if (id == R.id.item_add) {
            enableRenew = true;
            intent = new Intent(MainActivity.this, AppSettingActivity.class);
            startActivity(intent);
            return true;
        } else
            return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Intent intent;

        switch (id) {
            case R.id.item_add:
                enableRenew = true;
                intent = new Intent(MainActivity.this, AppSettingActivity.class);
                startActivity(intent);
                break;
            case R.id.app_help:
                intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://xzos.net/upgradeall-readme/"));
                intent = Intent.createChooser(intent, "请选择浏览器");
                startActivity(intent);
                break;
            case R.id.hub_list:
                intent = new Intent(MainActivity.this, HubListActivity.class);
                startActivity(intent);
                break;
            case R.id.app_log:
                intent = new Intent(MainActivity.this, LogActivity.class);
                startActivity(intent);
                break;
            case R.id.app_setting:
                intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
                break;
        }
        mDrawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        mDrawerLayout = findViewById(R.id.drawer_layout);
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
