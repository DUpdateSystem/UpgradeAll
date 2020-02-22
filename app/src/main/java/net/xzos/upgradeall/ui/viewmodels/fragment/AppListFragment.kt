package net.xzos.upgradeall.ui.viewmodels.fragment

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.fragment_app_list.*
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
        MainActivity.actionBarDrawerToggle.isDrawerIndicatorEnabled = true  // 默认允许侧滑
        activity?.run {
            this as AppCompatActivity
            this.findViewById<ImageView>(R.id.app_logo_image_view)?.visibility = View.GONE
            this.findViewById<CollapsingToolbarLayout>(R.id.collapsingToolbarLayout)?.contentScrim = getDrawable(R.color.colorPrimary)
            this.findViewById<ImageView>(R.id.toolbar_backdrop_image)?.setBackgroundColor(IconPalette.getColorInt(R.color.colorPrimary))
            this.findViewById<FloatingActionButton>(R.id.floatingActionButton)?.visibility = View.GONE
            this.findViewById<FloatingActionButton>(R.id.addFloatingActionButton)?.let { fab ->
                fab.setOnClickListener {
                    MainActivity.navigationItemId.value = R.id.appSettingFragment
                }
                fab.setImageDrawable(IconPalette.fabAddIcon)
                fab.backgroundTintList = ColorStateList.valueOf((IconPalette.getColorInt(R.color.bright_yellow)))
                fab.setColorFilter(IconPalette.getColorInt(R.color.light_gray))
                fab.visibility = View.VISIBLE
            }
        }
        AppTabSectionsPagerAdapter.newInstance(groupTabs, viewPager, childFragmentManager, viewLifecycleOwner)
    }

    override fun onResume() {
        super.onResume()
        activity?.findViewById<NavigationView>(R.id.navView)?.setCheckedItem(R.id.app_list)
    }

}