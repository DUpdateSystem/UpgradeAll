package net.xzos.upgradeall.ui.fragment.app_list

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_app_list.*
import kotlinx.android.synthetic.main.layout_appbar.view.*
import kotlinx.android.synthetic.main.layout_main.*
import net.xzos.upgradeall.R
import net.xzos.upgradeall.ui.activity.MainActivity
import net.xzos.upgradeall.ui.viewmodels.pageradapter.AppTabSectionsPagerAdapter
import net.xzos.upgradeall.utils.IconPalette

class AppListFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_app_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.run {
            navView.setCheckedItem(R.id.app_list)
            layout_appbar.app_logo_image_view.visibility = View.GONE
            window.statusBarColor = Color.TRANSPARENT
            layout_appbar.collapsingToolbarLayout.contentScrim = ContextCompat.getDrawable(requireContext(), R.color.colorPrimary)
            layout_appbar.toolbar_backdrop_image.setBackgroundColor(IconPalette.getColorInt(R.color.colorPrimary))
        }

        MainActivity.actionBarDrawerToggle.isDrawerIndicatorEnabled = true  // 默认允许侧滑
        AppTabSectionsPagerAdapter.newInstance(groupTabs, viewPager, childFragmentManager, viewLifecycleOwner)
    }

    override fun onResume() {
        super.onResume()
        activity?.addFloatingActionButton?.isVisible = true
    }
}
