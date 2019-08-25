package net.xzos.UpgradeAll.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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

import com.google.android.material.navigation.NavigationView;

import net.xzos.UpgradeAll.R;
import net.xzos.UpgradeAll.application.MyApplication;
import net.xzos.UpgradeAll.database.RepoDatabase;
import net.xzos.UpgradeAll.gson.ItemCardViewExtraData;
import net.xzos.UpgradeAll.ui.viewmodels.ItemCardView;
import net.xzos.UpgradeAll.ui.viewmodels.adapters.UpdateItemCardAdapter;
import net.xzos.UpgradeAll.utils.FileUtil;

import org.litepal.LitePal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String NAV_IMAGE_FILE_NAME = "nav_image.png";
    private static File NAV_IMAGE_FILE = new File(new File(MyApplication.getContext().getFilesDir(), "images"), NAV_IMAGE_FILE_NAME);

    private static final int PERMISSIONS_REQUEST_WRITE_CONTACTS = 0;
    final int READ_PIC_REQUEST_CODE = 1;
    final int PIC_CROP = 2;

    private boolean enableRenew = true;

    private List<ItemCardView> itemCardViewList = new ArrayList<>();

    private DrawerLayout mDrawerLayout;
    private NavigationView navView;
    private ImageView navViewHeaderImageView;
    private UpdateItemCardAdapter adapter;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (resultCode == Activity.RESULT_OK && resultData != null) {
            Uri uri = resultData.getData();
            if (uri != null) {
                switch (requestCode) {
                    case READ_PIC_REQUEST_CODE:
                        boolean haveCropApp = FileUtil.performCrop(this, PIC_CROP, uri, 16, 9);
                        if (!haveCropApp) {
                            saveImage(resultData, NAV_IMAGE_FILE);
                            initNavImage();
                        }
                        break;
                    case PIC_CROP:
                        initNavImage();
                        break;
                }
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
            delImage();
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
                initNavImage();
            }
        });
        setRecyclerView();
    }

    private void setNavImage() {
        if (FileUtil.requestPermission(this, PERMISSIONS_REQUEST_WRITE_CONTACTS)) {
            FileUtil.getPicFormGallery(this, READ_PIC_REQUEST_CODE);
        }
    }

    private void initNavImage() {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(NAV_IMAGE_FILE);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (fis != null) {
            Bitmap bitmap = BitmapFactory.decodeStream(fis);
            navViewHeaderImageView.setImageBitmap(bitmap);
        }
        Log.e("111", "initNavImage: " + fis);
    }

    private void saveImage(Intent intent, File imageFile) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        new Thread(() -> {
            new Handler(Looper.getMainLooper()).post(() -> {
                // 弹出等待框
                progressDialog.setTitle("正在处理，请稍等");
                progressDialog.setMessage("Loading...");
                progressDialog.setCancelable(false);
                progressDialog.show();
            });
            // 保存图片
            NAV_IMAGE_FILE.delete();
            Bitmap bitmap = FileUtil.getBitmapFromIntent(intent);
            if (bitmap != null) {
                String fileParent = imageFile.getParent();
                if (fileParent != null && !FileUtil.fileIsExistsByPath(fileParent)) {
                    File parent = new File(fileParent);
                    parent.mkdirs();
                }
                try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            // 取消等待框
            new Handler(Looper.getMainLooper()).post(progressDialog::cancel);
        }).start();
    }

    private void delImage() {
        NAV_IMAGE_FILE.delete();
        navViewHeaderImageView.setImageDrawable(null);
    }

    private void refreshCardView() {
        new Thread(() -> runOnUiThread(() -> {
            swipeRefresh.setRefreshing(true);
            refreshAppList();
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
            ItemCardViewExtraData extraData = new ItemCardViewExtraData();
            extraData.setDatabaseId(databaseId);
            itemCardViewList.add(new ItemCardView.Builder(name, url, api).extraData(extraData).build());
        }
        TextView guidelinesTextView = findViewById(R.id.guidelinesTextView);
        if (itemCardViewList.size() != 0) {
            ItemCardViewExtraData extraData = new ItemCardViewExtraData();
            extraData.setEmpty(true);
            itemCardViewList.add(new ItemCardView.Builder(null, null, null).extraData(extraData).build());
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
        adapter = new UpdateItemCardAdapter(itemCardViewList);
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
            intent = new Intent(MainActivity.this, UpdaterSettingActivity.class);
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
                intent = new Intent(MainActivity.this, UpdaterSettingActivity.class);
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
