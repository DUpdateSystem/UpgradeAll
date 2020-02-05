package net.xzos.upgradeAll.ui.viewmodels.pageradapter

import android.view.LayoutInflater
import android.view.View
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.group_item.view.*
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.data.database.manager.HubDatabaseManager
import net.xzos.upgradeAll.data.json.gson.UIConfig.Companion.uiConfig
import net.xzos.upgradeAll.ui.activity.MainActivity
import net.xzos.upgradeAll.ui.viewmodels.fragment.AppListPlaceholderFragment
import net.xzos.upgradeAll.utils.IconPalette


class AppTabSectionsPagerAdapter(fm: FragmentManager) :
        FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    private val mTabIndexList: MutableList<Int> = mutableListOf(
            UPDATE_PAGE_INDEX, USER_STAR_PAGE_INDEX, ALL_APP_PAGE_INDEX).apply {
        if (!uiConfig.userStarTab.userStarEnable) this.remove(USER_STAR_PAGE_INDEX)  // 检查用户是否关闭星标页面
    }

    init {
        // 在 ALL_APP_PAGE 前插入 uiConfig 索引
        mTabIndexList.addAll(mTabIndexList.indexOf(ALL_APP_PAGE_INDEX),
                (0 until uiConfig.userTabList.size).toList())
        renewViewPage.value = false
    }

    override fun getItem(position: Int): Fragment {
        return AppListPlaceholderFragment.newInstance(mTabIndexList[position])
    }

    override fun getPageTitle(position: Int): CharSequence? {
        // 传递 hubUuid
        return mTabIndexList[position].toString()
    }

    override fun getItemPosition(`object`: Any): Int {
        return PagerAdapter.POSITION_NONE
    }

    override fun getCount(): Int {
        return mTabIndexList.size
    }

    private fun getCustomTabView(position: Int, rootLayout: TabLayout): View {
        return LayoutInflater.from(rootLayout.context).inflate(R.layout.group_item, rootLayout, false).apply {
            this.delGroupCardView.visibility = View.GONE
            if (position >= 0) {
                val tabTitle: String
                var tabIconDrawableId: Int? = null
                var tabIconUrl: String? = null
                when (mTabIndexList[position]) {
                    UPDATE_PAGE_INDEX -> {
                        tabTitle = context.getString(R.string.update)
                        tabIconDrawableId = R.drawable.ic_update
                    }
                    USER_STAR_PAGE_INDEX -> {
                        tabTitle = context.getString(R.string.user_star)
                        tabIconDrawableId = R.drawable.ic_start
                    }
                    ALL_APP_PAGE_INDEX -> {
                        tabTitle = context.getString(R.string.all_app)
                        tabIconDrawableId = R.drawable.ic_app
                    }
                    else -> {
                        val tabInfo = uiConfig.userTabList[mTabIndexList[position]]
                        tabTitle = tabInfo.name
                        tabIconUrl = tabInfo.icon
                    }
                }
                groupCardView.visibility = View.VISIBLE
                addGroupCardView.visibility = View.GONE
                groupNameTextView.text = tabTitle
                if (tabIconDrawableId != null)
                    IconPalette.loadHubIconView(
                            groupIconImageView,
                            hubIconDrawableId = tabIconDrawableId
                    )
                else
                    IconPalette.loadHubIconView(
                            groupIconImageView,
                            tabIconUrl
                    )
                setOnClickListener(View.OnClickListener {
                    if (delGroupCardView.visibility == View.VISIBLE)
                        waiteDel(delGroupCardView, tabTitle, false)
                    else if (rootLayout.selectedTabPosition != position) {
                        // TODO: 进入分组实现后删除该点击事件
                        rootLayout.getTabAt(position)?.select()
                    }
                    // TODO: 进入分组按钮
                    return@OnClickListener
                })
                setOnLongClickListener(View.OnLongClickListener {
                    waiteDel(delGroupCardView, tabTitle, true)
                    return@OnLongClickListener true
                })
            } else {
                this.groupCardView.visibility = View.GONE
                with(addGroupCardView) {
                    visibility = View.VISIBLE
                    setOnClickListener {
                        MainActivity.navigationItemId.value = R.id.hubCloudFragment
                    }
                }
            }
        }
    }

    private fun waiteDel(delGroupCardView: CardView, hubUuid: String, needDel: Boolean) {
        with(delGroupCardView) {
            if (needDel) {
                visibility = View.VISIBLE
                setOnClickListener {
                    HubDatabaseManager.del(hubUuid)
                    renewViewPage.value = true
                }
            } else {
                visibility = View.GONE
                setOnClickListener(null)
            }
        }
    }

    companion object {
        internal val renewViewPage = MutableLiveData<Boolean>(true)
        internal const val UPDATE_PAGE_INDEX = -3
        internal const val USER_STAR_PAGE_INDEX = -2
        internal const val ALL_APP_PAGE_INDEX = -1
        internal fun setViewPage(tabLayout: TabLayout, viewPager: ViewPager, childFragmentManager: FragmentManager, lifecycleOwner: LifecycleOwner) {
            renewViewPage.let {
                it.value = true
                it.observe(lifecycleOwner, Observer { renew ->
                    if (renew) {
                        renewViewPage(tabLayout, viewPager, childFragmentManager)
                    }
                })
            }
        }

        private fun renewViewPage(tabLayout: TabLayout, viewPager: ViewPager, childFragmentManager: FragmentManager) {
            val sectionsPagerAdapter = AppTabSectionsPagerAdapter(childFragmentManager)
            viewPager.adapter = sectionsPagerAdapter
            with(tabLayout) {
                this.setupWithViewPager(viewPager)
                val count = this.tabCount
                for (i in 0 until count) {
                    this.getTabAt(i)?.customView = sectionsPagerAdapter.getCustomTabView(i, this)
                }
                addTab(
                        this.newTab().apply {
                            customView = sectionsPagerAdapter.getCustomTabView(-1, this@with)
                        }
                )
            }
        }
    }
}
