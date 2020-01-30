package net.xzos.upgradeAll.ui.viewmodels.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import net.xzos.upgradeAll.data.json.nongson.ObjectTag
import net.xzos.upgradeAll.server.log.LogUtil

class LogPageViewModel : ViewModel() {

    private val mLogObjectTag = MutableLiveData<ObjectTag>()
    internal val logList = Transformations.map(mLogObjectTag) { objectTag ->
        LogUtil.logDataProxy.getLogMessageListLiveData(objectTag)
    }

    internal fun setLogObjectTag(logObjectTag: ObjectTag) {
        mLogObjectTag.value = logObjectTag
    }
}