package net.xzos.upgradeall.ui.log

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.viewpager2.adapter.FragmentStateAdapter
import net.xzos.upgradeall.core.utils.log.ObjectTag


class LogTabSectionsPagerAdapter(owner: LifecycleOwner, fragmentActivity: FragmentActivity, logSort: String) :
        FragmentStateAdapter(fragmentActivity) {
    private var mLogObjectList: List<ObjectTag> = listOf()

    init {
        val liveDataLogObjectTagList = LogLiveData.getObjectTagListBySort(logSort)
        liveDataLogObjectTagList.observe(owner) { logObjectList ->
            mLogObjectList = logObjectList
            notifyDataSetChanged()
        }
    }

    fun getPageTitle(position: Int): CharSequence {
        return mLogObjectList[position].name
    }

    override fun getItemCount(): Int {
        return mLogObjectList.size
    }

    override fun createFragment(position: Int): Fragment {
        val logObjectTag = mLogObjectList[position]
        return LogPlaceholderFragment.newInstance(logObjectTag)
    }
}
