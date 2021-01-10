package net.xzos.upgradeall.core.log

class LogUtilProxy(private val objectTag: ObjectTag, private val tag: String) {

    fun v(msg: String) {
        Log.v(objectTag, tag, msg)
    }

    fun d(msg: String) {
        Log.d(objectTag, tag, msg)
    }

    fun i(msg: String) {
        Log.i(objectTag, tag, msg)
    }

    fun w(msg: String) {
        Log.w(objectTag, tag, msg)
    }

    fun e(msg: String) {
        Log.e(objectTag, tag, msg)
    }

    companion object {
        const val JS_TAG = "JavaScriptRunning"
        val objectTag = ObjectTag("DeBug", JS_TAG)    // 测试环境标签
    }
}
