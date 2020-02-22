package net.xzos.upgradeall.system_api.api

import net.xzos.upgradeall.data.json.nongson.ObjectTag
import net.xzos.upgradeall.system_api.interfaces.LogApi


/**
 * 自定义的日志打印工具类
 */
object LogApi {

    var logApiInterface: LogApi? = null

    // 调用Log.v()方法打印日志
    fun v(objectTag: ObjectTag, tag: String, msg: String) {
        logApiInterface?.v(objectTag, tag, msg)
    }

    // 调用Log.d()方法打印日志
    fun d(objectTag: ObjectTag, tag: String, msg: String) {
        logApiInterface?.d(objectTag, tag, msg)
    }

    // 调用Log.i()方法打印日志
    fun i(objectTag: ObjectTag, tag: String, msg: String) {
        logApiInterface?.i(objectTag, tag, msg)
    }

    // 调用Log.w()方法打印日志
    fun w(objectTag: ObjectTag, tag: String, msg: String) {
        logApiInterface?.w(objectTag, tag, msg)
    }

    // 调用Log.e()方法打印日志
    fun e(objectTag: ObjectTag, tag: String, msg: String) {
        logApiInterface?.e(objectTag, tag, msg)
    }

    // 提醒日志更新
    fun change(){
        logApiInterface?.change()
    }
}
