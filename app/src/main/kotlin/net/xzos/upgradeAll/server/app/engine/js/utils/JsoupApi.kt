package net.xzos.upgradeAll.server.app.engine.js.utils

import android.util.Log

import org.jsoup.Connection
import org.jsoup.nodes.Document

internal object JsoupApi {
    private const val TAG = "JsoupApi"

    fun getDoc(connection: Connection): Document? {
        return try {
            connection.get()
        } catch (e: Throwable) {
            Log.e(TAG, "getStringByJsoupXpath: Jsoup 对象初始化失败")
            e.printStackTrace()
            null
        }
    }
}
