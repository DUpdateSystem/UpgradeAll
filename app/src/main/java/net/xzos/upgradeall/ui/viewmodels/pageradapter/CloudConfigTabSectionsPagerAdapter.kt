package net.xzos.upgradeall.ui.viewmodels.pageradapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.PagerAdapter
import net.xzos.upgradeall.R
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.ui.fragment.cloud_config.CloudConfigPlaceholderFragment


class CloudConfigTabSectionsPagerAdapter(fm: FragmentManager) :
        FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private val context = MyApplication.context

    private var appTabTitle = context.getString(R.string.app)
    private var appHubTabTitle = context.getString(R.string.app_hub)
    private val tabTitles = listOf(appTabTitle, appHubTabTitle)

    override fun getItem(position: Int): Fragment {
        return when (tabTitles[position]) {
            appTabTitle -> CloudConfigPlaceholderFragment.newInstance(
                    CloudConfigPlaceholderFragment.CLOUD_APP_CONFIG
            )
            appHubTabTitle -> CloudConfigPlaceholderFragment.newInstance(
                    CloudConfigPlaceholderFragment.CLOUD_HUB_CONFIG
            )
            else -> Fragment()
        }
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return tabTitles[position]
    }

    override fun getItemPosition(`object`: Any): Int {
        return PagerAdapter.POSITION_NONE
    }

    override fun getCount(): Int {
        return tabTitles.size
    }
}