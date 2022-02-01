package net.xzos.upgradeall.ui.applist.base

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.manager.AppManager
import net.xzos.upgradeall.core.manager.HubManager
import net.xzos.upgradeall.databinding.ActivityAppHubBinding
import net.xzos.upgradeall.ui.base.AppBarActivity
import net.xzos.upgradeall.ui.detail.setting.AppSettingActivity

enum class TabIndex(val tag: String) {
    TAB_UPDATE("TAB_UPDATE"),
    TAB_STAR("TAB_STAR"),
    TAB_ALL("TAB_ALL"),
    TAB_APPLICATIONS_APP("TAB_APPLICATIONS_APP"),
}

abstract class AppHubActivity(private val mAppType: String) : AppBarActivity() {

    protected lateinit var binding: ActivityAppHubBinding

    override fun initBinding(): View {
        binding = ActivityAppHubBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun getAppBar(): Toolbar = binding.appbar.toolbar

    override fun initView() {
        val types = mutableListOf(TabIndex.TAB_UPDATE, TabIndex.TAB_ALL)
        val tabTitles = mutableListOf(
            getText(R.string.hub_tab_updates),
            getText(R.string.hub_tab_all),
        )
        if (AppManager.getAppList().any { it.star }) {
            types.add(1, TabIndex.TAB_STAR)
            tabTitles.add(1, getText(R.string.user_star))
        }

        if (HubManager.isEnableApplicationsMode()) {
            types.add(TabIndex.TAB_APPLICATIONS_APP)
            tabTitles.add(getText(R.string.applications))
        }

        binding.viewpager.apply {
            adapter = object : FragmentStateAdapter(this@AppHubActivity) {
                override fun getItemCount(): Int {
                    return types.size
                }

                override fun createFragment(position: Int): Fragment {
                    return getAppHubListFragment(mAppType, types[position])
                }
            }
        }

        val mediator = TabLayoutMediator(binding.tabLayout, binding.viewpager) { tab, position ->
            tab.text = tabTitles[position]
        }
        mediator.attach()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_app_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.add_app -> {
                AppSettingActivity.startActivity(this)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}