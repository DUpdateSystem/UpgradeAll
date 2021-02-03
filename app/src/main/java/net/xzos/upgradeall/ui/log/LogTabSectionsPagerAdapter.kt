package net.xzos.upgradeall.ui.log

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.LifecycleOwner
import androidx.viewpager.widget.PagerAdapter
import net.xzos.upgradeall.core.log.ObjectTag
import net.xzos.upgradeall.ui.log.LogLiveData
import net.xzos.upgradeall.ui.log.LogPlaceholderFragment


class LogTabSectionsPagerAdapter(owner: LifecycleOwner, fm: FragmentManager, logSort: String) :
        FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    private var mLogObjectList: List<ObjectTag> = listOf()

    init {
        val liveDataLogObjectTagList = LogLiveData.getObjectTagListBySort(logSort)
        liveDataLogObjectTagList.observe(owner, { logObjectList ->
            mLogObjectList = logObjectList
            notifyDataSetChanged()
        })
    }

    override fun getItem(position: Int): Fragment {
        val logObjectTag = mLogObjectList[position]
        return LogPlaceholderFragment.newInstance(logObjectTag)
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return mLogObjectList[position].name
    }

    override fun getItemPosition(`object`: Any): Int {
        return PagerAdapter.POSITION_NONE
    }

    override fun getCount(): Int {
        return mLogObjectList.size
    }
}
