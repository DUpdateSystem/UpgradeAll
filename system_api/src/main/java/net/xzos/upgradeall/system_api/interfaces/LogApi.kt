package net.xzos.upgradeall.system_api.interfaces

import net.xzos.upgradeall.data.json.nongson.ObjectTag


/**
 * 自定义的日志打印工具类
 */
interface LogApi {

    // 调用Log.v()方法打印日志
    fun v(objectTag: ObjectTag, tag: String, msg: String)

    // 调用Log.d()方法打印日志
    fun d(objectTag: ObjectTag, tag: String, msg: String)

    // 调用Log.i()方法打印日志
    fun i(objectTag: ObjectTag, tag: String, msg: String)

    // 调用Log.w()方法打印日志
    fun w(objectTag: ObjectTag, tag: String, msg: String)

    // 调用Log.e()方法打印日志
    fun e(objectTag: ObjectTag, tag: String, msg: String)


    // 提醒日志更新
    fun change()
}
