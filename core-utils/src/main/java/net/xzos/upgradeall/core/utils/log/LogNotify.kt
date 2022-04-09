package net.xzos.upgradeall.core.utils.log

import android.util.Log
import net.xzos.upgradeall.core.utils.oberver.InformerNullable
import net.xzos.upgradeall.core.utils.oberver.Tag


enum class LogStatus : Tag {
    PRINT_LOG_TAG, LOG_CHANGED_TAG
}

/**
 * 自定义的日志打印工具类
 */
object LogNotify : InformerNullable<LogStatus, LogItemData?>() {

    internal fun printLog(logItemData: LogItemData) {
        notifyChanged(LogStatus.PRINT_LOG_TAG, logItemData)
        when (logItemData.logLevel) {
            Log.VERBOSE -> Log.v(logItemData.tag, logItemData.msg)
            Log.DEBUG -> Log.d(logItemData.tag, logItemData.msg)
            Log.INFO -> Log.i(logItemData.tag, logItemData.msg)
            Log.WARN -> Log.w(logItemData.tag, logItemData.msg)
            Log.ERROR -> Log.e(logItemData.tag, logItemData.msg)
            else -> Log.wtf(logItemData.tag, logItemData.msg)
        }
    }

    internal fun logChanged() {
        notifyChanged(LogStatus.LOG_CHANGED_TAG, null)
    }
}