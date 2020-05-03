package net.xzos.upgradeall.android_api

import android.util.Log
import net.xzos.upgradeall.core.data.json.nongson.ObjectTag
import net.xzos.upgradeall.core.log.Log.DEBUG
import net.xzos.upgradeall.core.log.Log.ERROR
import net.xzos.upgradeall.core.log.Log.INFO
import net.xzos.upgradeall.core.log.Log.VERBOSE
import net.xzos.upgradeall.core.log.Log.WARN
import net.xzos.upgradeall.core.log.LogItemData
import net.xzos.upgradeall.core.oberver.Observer
import net.xzos.upgradeall.core.system_api.api.LOG_CHANGED_TAG
import net.xzos.upgradeall.core.system_api.api.LogApi
import net.xzos.upgradeall.core.system_api.api.PRINT_LOG_TAG
import net.xzos.upgradeall.server.log.LogLiveData


object Log {

    init {
        LogApi.observeForever(PRINT_LOG_TAG, object : Observer {
            override fun onChanged(vars: Array<out Any>): Any? {
                return printLog(vars[0] as LogItemData)
            }
        })
        LogApi.observeForever(LOG_CHANGED_TAG, object : Observer {
            override fun onChanged(vars: Array<out Any>): Any? {
                @Suppress("UNCHECKED_CAST")
                return LogLiveData.notifyChange(vars[0] as HashMap<ObjectTag, MutableList<LogItemData>>)
            }
        })
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
