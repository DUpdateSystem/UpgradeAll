package net.xzos.upgradeAll.ui.viewmodels.log

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.viewpager.widget.PagerAdapter
import net.xzos.upgradeAll.server.ServerContainer
import net.xzos.upgradeAll.server.log.LogDataProxy
import java.util.*

class SectionsPagerAdapter(owner: LifecycleOwner, fm: FragmentManager, private val logSort: String) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    private var mLogObjectIdList: List<String>? = null

    init {
        val logDataProxy = LogDataProxy(ServerContainer.AppServer.log)
        val liveDataLogObjectIdList = logDataProxy.getLiveDataLogObjectIdList(logSort)
        liveDataLogObjectIdList.observe(owner, Observer { logObjectIdList ->
            TAB_TITLES.clear()
            for (databaseIdString in logObjectIdList) {
                val name = LogDataProxy.getNameFromId(databaseIdString)
                if (name != null) TAB_TITLES.add(name)
            }
            mLogObjectIdList = logObjectIdList
            notifyDataSetChanged()
        })
    }

    override fun getItem(position: Int): Fragment {
        val logObjectTag = arrayOf(logSort, mLogObjectIdList!![position])
        return PlaceholderFragment.newInstance(logObjectTag)
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return TAB_TITLES[position]
    }

    override fun getItemPosition(`object`: Any): Int {
        return PagerAdapter.POSITION_NONE
    }

    override fun getCount(): Int {
        return TAB_TITLES.size
    }

    companion object {

        private val TAB_TITLES = ArrayList<String>()
    }
}