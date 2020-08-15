package net.xzos.upgradeall.ui.fragment.cloud_config

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_app_list.*
import kotlinx.android.synthetic.main.layout_main.*
import net.xzos.upgradeall.R
import net.xzos.upgradeall.ui.activity.MainActivity
import net.xzos.upgradeall.ui.viewmodels.pageradapter.CloudConfigTabSectionsPagerAdapter

class CloudConfigFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_cloud_config, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewPager.adapter = CloudConfigTabSectionsPagerAdapter(childFragmentManager)
        groupTabs.setupWithViewPager(viewPager)
    }

    override fun onResume() {
        super.onResume()
        MainActivity.actionBarDrawerToggle.isDrawerIndicatorEnabled = true  // 默认允许侧滑
        activity?.apply {
            addFloatingActionButton.visibility = View.GONE
            navView.setCheckedItem(R.id.cloud_hub_list)
        }
    }
}