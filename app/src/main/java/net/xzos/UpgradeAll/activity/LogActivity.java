package net.xzos.UpgradeAll.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import net.xzos.UpgradeAll.R;
import net.xzos.UpgradeAll.application.MyApplication;
import net.xzos.UpgradeAll.ui.viewmodels.log.SectionsPagerAdapter;
import net.xzos.UpgradeAll.utils.FileUtil;
import net.xzos.UpgradeAll.server.log.LogDataProxy;
import net.xzos.UpgradeAll.server.log.LogUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;

import io.github.kobakei.materialfabspeeddial.FabSpeedDial;
import io.github.kobakei.materialfabspeeddial.FabSpeedDialMenu;

public class LogActivity extends AppCompatActivity {
    private static final String TAG = "LogActivity";
    private static final String[] LogObjectTag = {"Core", TAG};
    protected static final LogUtil Log = MyApplication.getServerContainer().getLog();

    private static final int MY_PERMISSIONS_REQUEST_WRITE_CONTACTS = 3;

    private String logSort = "Core";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);
        // 设置 ActionBar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setElevation(0); // 关闭toolbar阴影
        }

        setFab();
        setViewPage(logSort);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_WRITE_CONTACTS) {
            if (grantResults.length <= 0
                    || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(LogActivity.this, "导出日志文件需要读写本地文件", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void requestPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Toast.makeText(LogActivity.this, "请给予本软件 读写存储空间权限", Toast.LENGTH_LONG).show();
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_WRITE_CONTACTS);
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_WRITE_CONTACTS);
            }
        }
    }


    private void setFab() {
        FabSpeedDial fab = findViewById(R.id.sortFab);
        List<String> logSortList = new LogDataProxy(Log).getLogSort();
        FabSpeedDialMenu menu = new FabSpeedDialMenu(this);
        for (String logSort : logSortList) {
            if (logSort.equals("Core"))
                menu.add(getResources().getString(R.string.main_program)).setIcon(R.drawable.ic_core);
            else
                menu.add(logSort).setIcon(R.drawable.ic_cloud);
        }
        fab.setMenu(menu);
        fab.addOnMenuItemClickListener((floatingActionButton, textView, integer) -> {
            logSort = logSortList.get(integer - 1);
            setViewPage(logSort);
            return null;
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_actionbar_log, menu);
        return true;
    }

    @SuppressLint("SdCardPath")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        View vItem;
        PopupMenu popupMenu;
        MenuInflater menuInflater;
        switch (id) {
            case R.id.log_clear:
                vItem = findViewById(R.id.log_clear);
                popupMenu = new PopupMenu(this, vItem);
                menuInflater = popupMenu.getMenuInflater();
                menuInflater.inflate(R.menu.menu_del_button, popupMenu.getMenu());
                popupMenu.show();
                //设置item的点击事件
                popupMenu.setOnMenuItemClickListener(popItem -> {
                    LogDataProxy logDataProxy = new LogDataProxy(Log);
                    switch (popItem.getItemId()) {
                        // 清空当前分类的日志
                        case R.id.log_del_sort:
                            logDataProxy.clearLogSort(logSort);
                            setViewPage(logSort);
                            setFab();
                            break;
                        // 清空全部日志
                        case R.id.log_del_all:
                            logDataProxy.clearLogAll();
                            setViewPage(logSort);
                            setFab();
                            break;
                    }
                    return true;
                });
                return true;
            case R.id.log_share:
                requestPermission();
                vItem = findViewById(R.id.log_share);
                popupMenu = new PopupMenu(this, vItem);
                menuInflater = popupMenu.getMenuInflater();
                menuInflater.inflate(R.menu.menu_share_button, popupMenu.getMenu());
                popupMenu.show();
                //设置item的点击事件
                String logFilePath = "/sdcard/Download/UpgradeAll/";
                String logFileName = "Log.txt";
                popupMenu.setOnMenuItemClickListener(popItem -> {
                    String logString = null;
                    LogDataProxy logDataProxy = new LogDataProxy(Log);
                    switch (popItem.getItemId()) {
                        // 导出当前分类日志
                        case R.id.log_share_sort:
                            logString = logDataProxy.getLogStringBySort(logSort);
                            break;
                        // 导出全部日志
                        case R.id.log_share_all:
                            logString = logDataProxy.getLogAllToString();
                            break;
                    }
                    if (logString != null) {
                        Log.d(LogObjectTag, TAG, "已获取日志");
                        File logFile = new File(logFilePath + logFileName);
                        File dir = new File(logFilePath);
                        if (!FileUtil.fileIsExistsByPath(logFilePath)) {
                            Log.d(LogObjectTag, TAG, "创建日志目录");
                            if (dir.mkdirs())
                                Log.d(LogObjectTag, TAG, "已创建日志目录");
                        }
                        if (!FileUtil.fileIsExistsByPath(logFilePath + logFileName)) {
                            try {
                                logFile.createNewFile();
                            } catch (IOException e) {
                                Log.e(LogObjectTag, TAG, "创建文件异常: ERROR_MESSAGE: " + e.toString());
                            }
                        }
                        if (FileUtil.writeTextFromUri(Uri.fromFile(logFile), logString))
                            Toast.makeText(this, "已导出日志至: " + logFilePath + logFileName, Toast.LENGTH_LONG).show();
                        else
                            Toast.makeText(this, "日志导出失败", Toast.LENGTH_LONG).show();
                    }
                    return true;
                });
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void setViewPage(String sort) {
        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager(), sort);
        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(sectionsPagerAdapter);
        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);
    }
}