package net.xzos.upgradeall.android_api

import android.util.Log
import net.xzos.upgradeall.core.data.json.nongson.ObjectTag
import net.xzos.upgradeall.core.log.Log.DEBUG
import net.xzos.upgradeall.core.log.Log.ERROR
import net.xzos.upgradeall.core.log.Log.INFO
import net.xzos.upgradeall.core.log.Log.VERBOSE
import net.xzos.upgradeall.core.log.Log.WARN
import net.xzos.upgradeall.core.log.LogItemData
import net.xzos.upgradeall.core.system_api.api.LogApi
import net.xzos.upgradeall.server.log.LogLiveData


object Log {

    init {
        LogApi.register(this)
    }

    @net.xzos.upgradeall.core.system_api.annotations.LogApi.printLog
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

    @net.xzos.upgradeall.core.system_api.annotations.LogApi.logChanged
    fun logChanged(logMap: HashMap<ObjectTag, MutableList<LogItemData>>) {
        LogLiveData.notifyChange(logMap)
    }
}
