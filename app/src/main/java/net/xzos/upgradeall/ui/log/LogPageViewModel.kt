package net.xzos.upgradeall.ui.log

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import net.xzos.upgradeall.core.utils.log.LogDataProxy
import net.xzos.upgradeall.core.utils.log.ObjectTag

class LogPageViewModel : ViewModel() {

    private val mLogObjectTag = MutableLiveData<ObjectTag>()
    internal val logList = mLogObjectTag.map { objectTag ->
        LogDataProxy.getLogMessageList(objectTag)
    }

    internal fun setLogObjectTag(logObjectTag: ObjectTag) {
        mLogObjectTag.value = logObjectTag
    }
}