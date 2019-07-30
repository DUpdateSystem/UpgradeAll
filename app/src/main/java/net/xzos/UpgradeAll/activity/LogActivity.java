package net.xzos.UpgradeAll.activity;

import android.annotation.SuppressLint;
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
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import net.xzos.UpgradeAll.R;
import net.xzos.UpgradeAll.application.MyApplication;
import net.xzos.UpgradeAll.ui.viewmodels.log.SectionsPagerAdapter;
import net.xzos.UpgradeAll.utils.FileUtil;
import net.xzos.UpgradeAll.utils.log.LogUtil;

import java.io.File;
import java.util.List;

import io.github.kobakei.materialfabspeeddial.FabSpeedDial;
import io.github.kobakei.materialfabspeeddial.FabSpeedDialMenu;

public class LogActivity extends AppCompatActivity {
    private static final String TAG = "LogActivity";
    protected static final LogUtil Log = MyApplication.getLog();

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

    private void setFab() {
        FabSpeedDial fab = findViewById(R.id.sortFab);
        List<String> logSortList = MyApplication.getLog().getLogSort();
        FabSpeedDialMenu menu = new FabSpeedDialMenu(this);
        for (String logSort : logSortList) {
            if (logSort.equals("Core"))
                menu.add("Core").setIcon(R.drawable.ic_core);
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
                    switch (popItem.getItemId()) {
                        // 清空当前分类的日志
                        case R.id.log_del_sort:
                            Log.clearLogSort(logSort);
                            setFab();
                            break;
                        // 清空全部日志
                        case R.id.log_del_all:
                            Log.clearLogAll();
                            setFab();
                            break;
                    }
                    return true;
                });
                return true;
            case R.id.log_share:
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
                    switch (popItem.getItemId()) {
                        // 导出当前分类日志
                        case R.id.log_share_sort:
                            logString = Log.getLogStringBySort(logSort);
                            break;
                        // 导出全部日志
                        case R.id.log_share_all:
                            logString = Log.getLogAllToString();
                            break;
                    }
                    if (logString != null) {
                        File path = new File(logFilePath);
                        if (!FileUtil.fileIsExistsByPath(logFilePath))
                            path.mkdirs();
                        File logFile = new File(logFilePath + logFileName);
                        if (FileUtil.writeTextFromUri(Uri.fromFile(logFile), logString))
                            Toast.makeText(this, "已导出日志至: " + logFilePath + logFileName, Toast.LENGTH_LONG).show();
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