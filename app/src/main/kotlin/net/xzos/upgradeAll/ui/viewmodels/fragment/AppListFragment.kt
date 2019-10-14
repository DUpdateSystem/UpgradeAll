package net.xzos.upgradeAll.ui.viewmodels.fragment

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.fragment_app_list.*
import kotlinx.android.synthetic.main.group_item.view.*
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.ui.activity.AppSettingActivity
import net.xzos.upgradeAll.ui.viewmodels.pageradapter.AppTabSectionsPagerAdapter


class AppListFragment(private val navigationItemId: MutableLiveData<Int>) : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_app_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setViewPage()
        with(activity?.findViewById<FloatingActionButton>(R.id.addFloatingActionButton)) {
            this?.setOnClickListener {
                startActivity(Intent(activity, AppSettingActivity::class.java))
            }
            this?.visibility = View.VISIBLE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        with(activity?.findViewById<FloatingActionButton>(R.id.addFloatingActionButton)) {
            this?.setOnClickListener(null)
            this?.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.findViewById<NavigationView>(R.id.navView)?.setCheckedItem(R.id.app_list)
    }

    private fun setViewPage() {
        activity?.supportFragmentManager?.let {
            val sectionsPagerAdapter = AppTabSectionsPagerAdapter(it, navigationItemId)
            viewPager.adapter = sectionsPagerAdapter
            with(groupTabs) {
                this.setupWithViewPager(viewPager)
                for (i in 0 until this.tabCount) {
                    this.getTabAt(i)?.let { tab ->
                        tab.customView = sectionsPagerAdapter.getCustomTabView(i, this).apply {
                            if (i == 0) {
                                @Suppress("DEPRECATION")
                                this.groupNameTextView.setTextColor(
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            context.getColor(R.color.text_color)
                                        } else
                                            context.resources.getColor(R.color.text_color))
                            }
                        }
                    }
                }
                addTab(
                        this.newTab().apply {
                            customView = sectionsPagerAdapter.getCustomTabView(-1, this@with)
                        }
                )
                @Suppress("DEPRECATION")
                addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                    override fun onTabSelected(tab: TabLayout.Tab) {
                        tab.customView?.findViewById<TextView>(R.id.groupNameTextView)?.setTextColor(
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    context.getColor(R.color.text_color)
                                } else
                                    context.resources.getColor(R.color.text_color)
                        )
                    }

                    override fun onTabUnselected(tab: TabLayout.Tab) {
                        tab.customView?.findViewById<TextView>(R.id.groupNameTextView)?.setTextColor(
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    context.getColor(R.color.text_low_priority_color)
                                } else
                                    context.resources.getColor(R.color.text_low_priority_color)
                        )
                    }

                    override fun onTabReselected(tab: TabLayout.Tab) {
                    }
                })
            }
        }
    }
}