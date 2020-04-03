package net.xzos.upgradeall.core.system_api.api

import net.xzos.upgradeall.core.log.Log
import net.xzos.upgradeall.core.log.LogItemData
import net.xzos.upgradeall.core.system_api.RegisterApi
import net.xzos.upgradeall.core.system_api.annotations.LogApi


private val printLogAnnotation =
    LogApi.printLog::class.java

private val logChangedAnnotation =
    LogApi.logChanged::class.java

/**
 * 自定义的日志打印工具类
 */
object LogApi : RegisterApi(printLogAnnotation) {

    internal fun printLog(logItemData: LogItemData) {
        runNoReturnFun(printLogAnnotation, logItemData)
    }

    internal fun logChanged() {
        runNoReturnFun(logChangedAnnotation, Log.logMap)
    }
}
