package net.xzos.UpgradeAll.activity;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import net.xzos.UpgradeAll.R;
import net.xzos.UpgradeAll.application.MyApplication;
import net.xzos.UpgradeAll.ui.viewmodels.log.SectionsPagerAdapter;

import java.util.List;

import io.github.kobakei.materialfabspeeddial.FabSpeedDial;
import io.github.kobakei.materialfabspeeddial.FabSpeedDialMenu;

public class LogActivity extends AppCompatActivity {
    private static final String TAG = "LogActivity";

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
            String sort = logSortList.get(integer - 1);
            setViewPage(sort);
            return null;
        });
        setViewPage("Core");
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setViewPage(String sort) {
        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager(), sort);
        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(sectionsPagerAdapter);
        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);
    }
}