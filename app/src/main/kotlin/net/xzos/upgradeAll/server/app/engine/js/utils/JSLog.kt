package net.xzos.upgradeAll.server.app.engine.js.utils

import net.xzos.upgradeAll.server.ServerContainer

class JSLog(private val LogObjectTag: Array<String>) {

    fun v(msgObject: Any) {
        Log.v(LogObjectTag, TAG, msgObject)
    }

    fun d(msgObject: Any) {
        Log.d(LogObjectTag, TAG, msgObject)
    }

    fun i(msgObject: Any) {
        Log.i(LogObjectTag, TAG, msgObject)
    }

    fun w(msgObject: Any) {
        Log.w(LogObjectTag, TAG, msgObject)
    }

    fun e(msgObject: Any) {
        Log.e(LogObjectTag, TAG, msgObject)
    }

    companion object {
        private val Log = ServerContainer.Log
        private const val TAG = "JavaScriptRunning"
    }
}
