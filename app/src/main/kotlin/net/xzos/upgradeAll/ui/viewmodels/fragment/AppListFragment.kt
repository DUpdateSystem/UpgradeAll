package net.xzos.upgradeAll.ui.viewmodels.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.fragment_app_list.*
import kotlinx.android.synthetic.main.group_item.view.*
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.ui.activity.AppSettingActivity
import net.xzos.upgradeAll.ui.viewmodels.pageradapter.AppTabSectionsPagerAdapter
import net.xzos.upgradeAll.utils.IconPalette


class AppListFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_app_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setViewPage()
    }

    @SuppressLint("ResourceAsColor")
    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity).let {
            it.findViewById<FloatingActionButton>(R.id.floatingActionButton)?.visibility = View.GONE
            it.findViewById<NavigationView>(R.id.navView)?.setCheckedItem(R.id.app_list)
            it.findViewById<FloatingActionButton>(R.id.addFloatingActionButton)?.let { fab ->
                fab.setOnClickListener {
                    startActivity(Intent(activity, AppSettingActivity::class.java))
                }
                fab.setImageDrawable(IconPalette.fabAddIcon)
                fab.backgroundTintList = ColorStateList.valueOf((IconPalette.getColorInt(R.color.bright_yellow)))
                fab.setColorFilter(IconPalette.getColorInt(R.color.light_gray))
                fab.visibility = View.VISIBLE
            }
        }
    }

    private fun setViewPage() {
        activity?.supportFragmentManager?.let {
            val sectionsPagerAdapter = AppTabSectionsPagerAdapter(it)
            viewPager.adapter = sectionsPagerAdapter
            with(groupTabs) {
                this.setupWithViewPager(viewPager)
                for (i in 0 until this.tabCount) {
                    this.getTabAt(i)?.let { tab ->
                        tab.customView = sectionsPagerAdapter.getCustomTabView(i, this).apply {
                            if (i == 0) {
                                @Suppress("DEPRECATION")
                                this.groupNameTextView.setTextColor(IconPalette.getColorInt(R.color.text_color))
                            }
                        }
                        if (tab.position == tabSelectedPosition) {
                            tab.customView?.findViewById<TextView>(R.id.groupNameTextView)
                                    ?.setTextColor(IconPalette.getColorInt(R.color.text_color))
                            tab.select()
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
                        tabSelectedPosition = tab.position
                        tab.customView?.findViewById<TextView>(R.id.groupNameTextView)
                                ?.setTextColor(IconPalette.getColorInt(R.color.text_color))
                    }

                    override fun onTabUnselected(tab: TabLayout.Tab) {
                        tab.customView?.findViewById<TextView>(R.id.groupNameTextView)
                                ?.setTextColor(IconPalette.getColorInt(R.color.text_low_priority_color))
                    }

                    override fun onTabReselected(tab: TabLayout.Tab) {
                    }
                })
            }
        }
    }

    companion object {
        private var tabSelectedPosition = 0
    }
}