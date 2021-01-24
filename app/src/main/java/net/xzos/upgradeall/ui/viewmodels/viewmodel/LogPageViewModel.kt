package net.xzos.upgradeall.ui.viewmodels.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import net.xzos.upgradeall.core.log.LogDataProxy
import net.xzos.upgradeall.core.log.ObjectTag

class LogPageViewModel : ViewModel() {

    private val mLogObjectTag = MutableLiveData<ObjectTag>()
    internal val logList = Transformations.map(mLogObjectTag) { objectTag ->
        LogDataProxy.getLogMessageList(objectTag)
    }

    internal fun setLogObjectTag(logObjectTag: ObjectTag) {
        mLogObjectTag.value = logObjectTag
    }
}