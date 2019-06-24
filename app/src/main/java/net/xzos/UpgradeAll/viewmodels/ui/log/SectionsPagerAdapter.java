package net.xzos.UpgradeAll.viewmodels.ui.log;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import net.xzos.UpgradeAll.R;
import net.xzos.UpgradeAll.data.MyApplication;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
public class SectionsPagerAdapter extends FragmentPagerAdapter {

    private static ArrayList<String> TAB_TITLES = new ArrayList<>();
    private final Context mContext;

    public SectionsPagerAdapter(Context context, FragmentManager fm) {
        super(fm);
        TAB_TITLES.clear();
        mContext = context;
        JSONObject logMessage = MyApplication.getLog().getLogMessage();
        Iterator it = logMessage.keys();
        while (it.hasNext()) {
            TAB_TITLES.add((String) it.next());
        }
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        // getItem is called to instantiate the fragment for the given page.
        // Return a PlaceholderFragment (defined as a static inner class below).
        return PlaceholderFragment.newInstance(TAB_TITLES.get(position));
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return TAB_TITLES.get(position);
    }

    @Override
    public int getCount() {
        // Show 2 total pages.
        return TAB_TITLES.size();
    }
}