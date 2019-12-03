package net.xzos.upgradeAll.ui.viewmodels.pageradapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.viewpager.widget.PagerAdapter
import net.xzos.upgradeAll.server.log.LogDataProxy
import net.xzos.upgradeAll.server.log.LogUtil
import net.xzos.upgradeAll.ui.viewmodels.fragment.LogPlaceholderFragment
import java.util.*

class LogTabSectionsPagerAdapter(owner: LifecycleOwner, fm: FragmentManager, private val logSort: String) :
        FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    private lateinit var mLogObjectIdList: List<String>

    init {
        val logDataProxy = LogUtil.logDataProxy
        val liveDataLogObjectIdList = logDataProxy.getLiveDataLogObjectIdList(logSort)
        liveDataLogObjectIdList.observe(owner, Observer { logObjectIdList ->
            TAB_TITLES.clear()
            for (databaseIdString in logObjectIdList) {
                val name = LogDataProxy.getNameFromId(databaseIdString)
                if (name != null) TAB_TITLES.add(name)
            }
            mLogObjectIdList = logObjectIdList.toList()
            notifyDataSetChanged()
        })
    }

    override fun getItem(position: Int): Fragment {
        val logObjectTag = arrayOf(logSort, mLogObjectIdList[position])
        return LogPlaceholderFragment.newInstance(logObjectTag)
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