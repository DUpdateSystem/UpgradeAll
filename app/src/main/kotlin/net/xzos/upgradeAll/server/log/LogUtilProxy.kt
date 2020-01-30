package net.xzos.upgradeAll.server.log

import net.xzos.upgradeAll.data.json.nongson.ObjectTag

internal class LogUtilProxy(private val ObjectTag: ObjectTag, private val tag: String) {

    fun v(msg: String) {
        LogUtil.v(ObjectTag, tag, msg)
    }

    fun d(msg: String) {
        LogUtil.d(ObjectTag, tag, msg)
    }

    fun i(msg: String) {
        LogUtil.i(ObjectTag, tag, msg)
    }

    fun w(msg: String) {
        LogUtil.w(ObjectTag, tag, msg)
    }

    fun e(msg: String) {
        LogUtil.e(ObjectTag, tag, msg)
    }

    companion object {
        internal const val JS_TAG = "JavaScriptRunning"
    }
}
