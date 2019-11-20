package net.xzos.upgradeAll.ui.viewmodels.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.fragment_app_list.*
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.ui.activity.MainActivity
import net.xzos.upgradeAll.ui.viewmodels.pageradapter.CloudConfigTabSectionsPagerAdapter

class CloudConfigFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_cloud_config, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        MainActivity.actionBarDrawerToggle.isDrawerIndicatorEnabled = true  // 默认允许侧滑
        activity?.apply {
            this as AppCompatActivity
            this.findViewById<FloatingActionButton>(R.id.floatingActionButton)?.visibility = View.GONE
            this.findViewById<FloatingActionButton>(R.id.addFloatingActionButton)?.visibility = View.GONE
        }
        viewPager.adapter = CloudConfigTabSectionsPagerAdapter(childFragmentManager)
        groupTabs.setupWithViewPager(viewPager)
    }

    override fun onResume() {
        super.onResume()
        activity?.findViewById<NavigationView>(R.id.navView)?.setCheckedItem(R.id.cloud_hub_list)
    }
}