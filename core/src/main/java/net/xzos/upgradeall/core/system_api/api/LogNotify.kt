package net.xzos.upgradeall.core.system_api.api

import net.xzos.upgradeall.core.data.coroutines.CoroutinesMutableList
import net.xzos.upgradeall.core.data.coroutines.CoroutinesMutableMap
import net.xzos.upgradeall.core.data.json.nongson.ObjectTag
import net.xzos.upgradeall.core.log.LogItemData
import net.xzos.upgradeall.core.oberver.Informer


const val PRINT_LOG_TAG = "PRINT_LOG"
const val LOG_CHANGED_TAG = "LOG_CHANGED"

/**
 * 自定义的日志打印工具类
 */
object LogApi : Informer {

    internal fun printLog(logItemData: LogItemData) {
        notifyChanged(PRINT_LOG_TAG, logItemData)
    }

    internal fun logChanged(logMap: CoroutinesMutableMap<ObjectTag, CoroutinesMutableList<LogItemData>>) {
        notifyChanged(LOG_CHANGED_TAG, logMap)
    }
}
