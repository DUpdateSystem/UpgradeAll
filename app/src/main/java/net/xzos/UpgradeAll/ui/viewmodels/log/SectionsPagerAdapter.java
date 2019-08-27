package net.xzos.UpgradeAll.ui.viewmodels.log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;

import net.xzos.UpgradeAll.server.ServerContainer;
import net.xzos.UpgradeAll.server.log.LogDataProxy;

import java.util.ArrayList;
import java.util.List;

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

    private static ArrayList<String> TAB_TITLES = new ArrayList<>();
    private String logSort;
    private List<String> mLogObjectIdList;

    public SectionsPagerAdapter(LifecycleOwner owner, FragmentManager fm, String logSort) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        this.logSort = logSort;
        LogDataProxy logDataProxy = new LogDataProxy(ServerContainer.AppServer.getLog());
        LiveData<List<String>> liveDataLogObjectIdList = logDataProxy.getLiveDataLogObjectIdList(logSort);
        liveDataLogObjectIdList.observe(owner, logObjectIdList -> {
            TAB_TITLES.clear();
            for (String databaseIdString : logObjectIdList) {
                String name = LogDataProxy.getNameFromId(databaseIdString);
                TAB_TITLES.add(name);
            }
            mLogObjectIdList = logObjectIdList;
            notifyDataSetChanged();
        });
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        String[] logObjectTag = {logSort, mLogObjectIdList.get(position)};
        return PlaceholderFragment.newInstance(logObjectTag);
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return TAB_TITLES.get(position);
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        return POSITION_NONE;
    }

    @Override
    public int getCount() {
        return TAB_TITLES.size();
    }
}