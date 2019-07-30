package net.xzos.UpgradeAll.ui.viewmodels.log;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import net.xzos.UpgradeAll.application.MyApplication;
import net.xzos.UpgradeAll.utils.log.LogMessageProxy;

import java.util.ArrayList;
import java.util.List;

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

    private static ArrayList<String> TAB_TITLES = new ArrayList<>();
    private String logSort;
    private List<String> logObjectIdList;
    private final Context mContext;

    public SectionsPagerAdapter(Context context, FragmentManager fm, String logSort) {
        super(fm);
        this.logSort = logSort;
        TAB_TITLES.clear();
        mContext = context;
        logObjectIdList = MyApplication.getLog().getLogObjectId(logSort);
        for (String databaseIdString : logObjectIdList) {
            String name = LogMessageProxy.getNameFromId(databaseIdString);
            TAB_TITLES.add(name);
        }
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        // getItem is called to instantiate the fragment for the given page.
        // Return a PlaceholderFragment (defined as a static inner class below).
        String[] logObjectTag = {logSort, logObjectIdList.get(position)};
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
        // Show 2 total pages.
        return TAB_TITLES.size();
    }

    public void setLogSort(String sort) {
        this.logSort = sort;
    }
}