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

const val TAB_UPDATE = 0
const val TAB_STAR = 1
const val TAB_ALL = 2
const val TAB_APPLICATIONS_APP = 3

abstract class AppHubActivity(private val mAppType: String) : AppBarActivity() {

    protected lateinit var binding: ActivityAppHubBinding

    override fun initBinding(): View {
        binding = ActivityAppHubBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun getAppBar(): Toolbar = binding.appbar.toolbar

    override fun initView() {
        val types = mutableListOf(TAB_UPDATE, TAB_ALL)
        val tabTitles = mutableListOf(
            getText(R.string.hub_tab_updates),
            getText(R.string.hub_tab_all),
        )
        if (AppManager.getAppList().any { it.star }) {
            types.add(1, TAB_STAR)
            tabTitles.add(1, getText(R.string.user_star))
        }

        if (HubManager.isEnableApplicationsMode()) {
            types.add(TAB_APPLICATIONS_APP)
            tabTitles.add(getText(R.string.applications))
        }

        binding.viewpager.apply {
            adapter = object : FragmentStateAdapter(this@AppHubActivity) {
                override fun getItemCount(): Int {
                    return types.size
                }

                override fun createFragment(position: Int): Fragment {
                    return getAppHubListFragment(mAppType, position)
                }
            }
        }

        val mediator = TabLayoutMediator(binding.tabLayout, binding.viewpager) { tab, position ->
            tab.text = tabTitles[position]
        }
        mediator.attach()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
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