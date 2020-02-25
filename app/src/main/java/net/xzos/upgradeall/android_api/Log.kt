package net.xzos.upgradeall.android_api

import android.util.Log
import net.xzos.dupdatesystem.data.json.nongson.ObjectTag
import net.xzos.dupdatesystem.system_api.interfaces.LogApi
import net.xzos.upgradeall.server.log.LogLiveData


object Log : LogApi {

    init {
        net.xzos.dupdatesystem.system_api.api.LogApi.logApiInterface = this
    }

    // 调用Log.v()方法打印日志
    override fun v(objectTag: ObjectTag, tag: String, msg: String) {
        Log.v(tag, msg)
    }

    // 调用Log.d()方法打印日志
    override fun d(objectTag: ObjectTag, tag: String, msg: String) {
        Log.d(tag, msg)
    }

    // 调用Log.i()方法打印日志
    override fun i(objectTag: ObjectTag, tag: String, msg: String) {
        Log.i(tag, msg)
    }

    // 调用Log.w()方法打印日志
    override fun w(objectTag: ObjectTag, tag: String, msg: String) {
        Log.w(tag, msg)
    }

    // 调用Log.e()方法打印日志
    override fun e(objectTag: ObjectTag, tag: String, msg: String) {
        Log.e(tag, msg)
    }

    override fun change() {
        LogLiveData.notifyChange()
    }
}