package net.xzos.upgradeall.ui.viewmodels.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import net.xzos.upgradeall.data.json.nongson.ObjectTag
import net.xzos.upgradeall.log.LogDataProxy

class LogPageViewModel : ViewModel() {

    private val mLogObjectTag = MutableLiveData<ObjectTag>()
    internal val logList = Transformations.map(mLogObjectTag) { objectTag ->
        LogDataProxy.getLogMessageList(objectTag)
    }

    internal fun setLogObjectTag(logObjectTag: ObjectTag) {
        mLogObjectTag.value = logObjectTag
    }
}