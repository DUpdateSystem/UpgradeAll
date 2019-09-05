package net.xzos.upgradeAll.server.app.engine.js.utils

import net.xzos.upgradeAll.server.ServerContainer

class JSLog(private val LogObjectTag: Array<String>) {

    fun v(msg: String) {
        Log.v(LogObjectTag, TAG, msg)
    }

    fun d(msg: String) {
        Log.d(LogObjectTag, TAG, msg)
    }

    fun i(msg: String) {
        Log.i(LogObjectTag, TAG, msg)
    }

    fun w(msg: String) {
        Log.w(LogObjectTag, TAG, msg)
    }

    fun e(msg: String) {
        Log.e(LogObjectTag, TAG, msg)
    }

    companion object {
        private val Log = ServerContainer.Log
        private const val TAG = "JavaScriptRunning"
    }
}
