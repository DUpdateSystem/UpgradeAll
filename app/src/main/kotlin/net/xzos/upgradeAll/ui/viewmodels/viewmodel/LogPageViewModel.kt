package net.xzos.upgradeAll.ui.viewmodels.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import net.xzos.upgradeAll.server.log.LogUtil

class LogPageViewModel : ViewModel() {

    private val mLogObjectTag = MutableLiveData<Pair<String, String>>()
    internal val logList = Transformations.map(mLogObjectTag) { logObjectTag ->
        LogUtil.logDataProxy.getLogMessageListLiveData(logObjectTag)
    }

    internal fun setLogObjectTag(logObjectTag: Pair<String, String>) {
        mLogObjectTag.value = logObjectTag
    }
}