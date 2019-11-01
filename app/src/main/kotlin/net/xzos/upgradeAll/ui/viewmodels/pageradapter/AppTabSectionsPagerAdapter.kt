package net.xzos.upgradeAll.ui.viewmodels.pageradapter

import android.view.LayoutInflater
import android.view.View
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.PagerAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.group_item.view.*
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.server.hub.HubManager
import net.xzos.upgradeAll.ui.activity.MainActivity.Companion.navigationItemId
import net.xzos.upgradeAll.ui.viewmodels.fragment.AppListPlaceholderFragment


class AppTabSectionsPagerAdapter(fm: FragmentManager) :
        FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    private val mHubUuidList: MutableList<String> = mutableListOf()

    init {
        val hubDatabases = HubManager.databases
        for (hubDatabase in hubDatabases) {
            mHubUuidList.add(hubDatabase.uuid)
        }
    }

    override fun getItem(position: Int): Fragment {
        return AppListPlaceholderFragment.newInstance(mHubUuidList[position])
    }

    override fun getPageTitle(position: Int): CharSequence? {
        // 传递 hubUuid
        return mHubUuidList[position]
    }

    override fun getItemPosition(`object`: Any): Int {
        return PagerAdapter.POSITION_NONE
    }

    override fun getCount(): Int {
        return mHubUuidList.size
    }

    internal fun getCustomTabView(position: Int, rootLayout: TabLayout): View {
        return LayoutInflater.from(rootLayout.context).inflate(R.layout.group_item, rootLayout, false).apply {
            this.delGroupCardView.visibility = View.GONE
            if (position >= 0) {
                val hubUuid = mHubUuidList[position]
                this.groupCardView.visibility = View.VISIBLE
                this.addGroupCardView.visibility = View.GONE
                this.groupNameTextView.text = HubManager.getDatabase(hubUuid)?.name
                val hubIconUrl = HubManager.getHubIconUrl(hubUuid)
                if (hubIconUrl != null)
                    Glide.with(this)
                            .load(hubIconUrl)
                            .apply(RequestOptions.circleCropTransform())
                            .into(this.groupIconImageView)
                setOnClickListener(View.OnClickListener {
                    if (delGroupCardView.visibility == View.VISIBLE)
                        waiteDel(rootLayout, position, delGroupCardView, hubUuid, false)
                    else if (rootLayout.selectedTabPosition != position) {
                        // TODO: 进入分组实现后删除该点击事件
                        rootLayout.getTabAt(position)?.select()
                    }
                    // TODO: 进入分组按钮
                    return@OnClickListener
                })
                setOnLongClickListener(View.OnLongClickListener {
                    waiteDel(rootLayout, position, delGroupCardView, hubUuid, true)
                    return@OnLongClickListener true
                })
            } else {
                this.groupCardView.visibility = View.GONE
                with(this.addGroupCardView) {
                    visibility = View.VISIBLE
                    setOnClickListener {
                        navigationItemId.value = Pair(R.id.hubCloudFragment, null)
                    }
                }
            }
        }
    }

    private fun waiteDel(rootLayout: TabLayout, position: Int, delGroupCardView: CardView, hubUuid: String, needDel: Boolean) {
        with(delGroupCardView) {
            if (needDel) {
                visibility = View.VISIBLE
                setOnClickListener {
                    HubManager.del(hubUuid)
                    mHubUuidList.remove(hubUuid)
                    rootLayout.removeTabAt(position)
                }
            } else {
                visibility = View.GONE
                setOnClickListener(null)
            }
        }
    }
}