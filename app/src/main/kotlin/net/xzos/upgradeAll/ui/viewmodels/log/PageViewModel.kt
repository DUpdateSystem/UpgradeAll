package net.xzos.upgradeAll.ui.viewmodels.log

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel

import net.xzos.upgradeAll.server.ServerContainer
import net.xzos.upgradeAll.server.log.LogDataProxy

class PageViewModel : ViewModel() {

    private val mLogObjectTag = MutableLiveData<Array<String>>()
    internal val logList = Transformations.map(mLogObjectTag) { logObjectTag -> LogDataProxy(ServerContainer.Log).getLogMessageListLiveData(logObjectTag) }

    internal fun setLogObjectTag(logObjectTag: Array<String>) {
        mLogObjectTag.value = logObjectTag
    }
}