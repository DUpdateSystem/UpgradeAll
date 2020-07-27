package net.xzos.upgradeall.android_api

import android.util.Log
import net.xzos.upgradeall.core.data.json.nongson.ObjectTag
import net.xzos.upgradeall.core.log.Log.DEBUG
import net.xzos.upgradeall.core.log.Log.ERROR
import net.xzos.upgradeall.core.log.Log.INFO
import net.xzos.upgradeall.core.log.Log.VERBOSE
import net.xzos.upgradeall.core.log.Log.WARN
import net.xzos.upgradeall.core.log.LogItemData
import net.xzos.upgradeall.core.system_api.api.LOG_CHANGED_TAG
import net.xzos.upgradeall.core.system_api.api.LogApi
import net.xzos.upgradeall.core.system_api.api.PRINT_LOG_TAG
import net.xzos.upgradeall.data.log.LogLiveData


object Log {

    init {
        LogApi.observeForever<LogItemData>(PRINT_LOG_TAG, fun(logItemData) {
            printLog(logItemData)
        })
        LogApi.observeForever<HashMap<ObjectTag, MutableList<LogItemData>>>(LOG_CHANGED_TAG,
                fun(logMap) {
                    LogLiveData.notifyChange(logMap)
                }
        )
    }

    private fun printLog(logItemData: LogItemData) {
        val tag = logItemData.tag
        val msg = logItemData.msg
        when (logItemData.logLevel) {
            VERBOSE -> Log.v(tag, msg)
            DEBUG -> Log.d(tag, msg)
            INFO -> Log.i(tag, msg)
            WARN -> Log.w(tag, msg)
            ERROR -> Log.e(tag, msg)
        }
    }
}
