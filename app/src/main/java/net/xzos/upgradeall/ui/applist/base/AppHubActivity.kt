package net.xzos.upgradeall.ui.applist.base

import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.data.ANDROID_APP_TYPE
import net.xzos.upgradeall.databinding.ActivityAppHubBinding
import net.xzos.upgradeall.ui.base.AppBarActivity

const val TAB_UPDATE = 0
const val TAB_ALL = 1
const val TAB_IGNORED = 2

abstract class AppHubActivity(private val appType: String) : AppBarActivity() {

    protected lateinit var binding: ActivityAppHubBinding
    private val viewModel by viewModels<AppHubViewModel>()

    override fun initBinding(): View {
        binding = ActivityAppHubBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun getAppBar(): Toolbar = binding.appbar.toolbar

    override fun initView() {
        viewModel.setAppType(ANDROID_APP_TYPE)
        val types = listOf(
                TAB_UPDATE, TAB_ALL, TAB_IGNORED
        )
        val tabTitles = listOf(
                getText(R.string.hub_tab_updates),
                getText(R.string.user_star),
                getText(R.string.hub_tab_all),
        )

        binding.viewpager.apply {
            adapter = object : FragmentStateAdapter(this@AppHubActivity) {
                override fun getItemCount(): Int {
                    return types.size
                }

                override fun createFragment(position: Int): Fragment {
                    return getAppHubListFragment(position, appType)
                }
            }
            offscreenPageLimit = 1
        }

        val mediator = TabLayoutMediator(binding.tabLayout, binding.viewpager) { tab, position ->
            tab.text = tabTitles[position]
        }
        mediator.attach()

        viewModel.itemCountLiveData.observe(this, {
            binding.tvAppUpdateTip.text = String.format(getString(R.string.hub_format_app_update_tip), it)
        })
    }
}