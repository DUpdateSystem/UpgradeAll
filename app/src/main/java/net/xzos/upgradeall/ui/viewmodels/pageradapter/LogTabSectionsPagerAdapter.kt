package net.xzos.upgradeall.ui.viewmodels.pageradapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.viewpager.widget.PagerAdapter
import net.xzos.dupdatesystem.data.json.nongson.ObjectTag
import net.xzos.upgradeall.server.log.LogLiveData
import net.xzos.upgradeall.ui.viewmodels.fragment.LogPlaceholderFragment
import java.util.*


class LogTabSectionsPagerAdapter(owner: LifecycleOwner, fm: FragmentManager, logSort: String) :
        FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    private lateinit var mLogObjectList: List<ObjectTag>

    init {
        val liveDataLogObjectTagList = LogLiveData.getObjectTagListBySort(logSort)
        liveDataLogObjectTagList.observe(owner, Observer { logObjectIdList ->
            TAB_TITLES.clear()
            for (objectTag in logObjectIdList) {
                val name = objectTag.name
                TAB_TITLES.add(name)
            }
            mLogObjectList = logObjectIdList
            notifyDataSetChanged()
        })
    }

    override fun getItem(position: Int): Fragment {
        val logObjectTag = mLogObjectList[position]
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