package net.xzos.upgradeAll.server.log

import android.annotation.SuppressLint
import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * 自定义的日志打印工具类
 */
class LogUtil {

    /**
     * 定义一个JsonObject存储日志
     * {
     * repo_database_id: [String_Log1, String_Log2, ]
     * }
     */
    internal var logJSONObject = JSONObject()

    internal val logLiveData = LogLiveData()

    private fun addLogMessage(LogLevel: Int, logObjectTag: Array<String>, tag: String, msg: String) {
        // 确定日志等级标志
        val logLevelString: String = when (LogLevel) {
            VERBOSE -> "V"
            DEBUG -> "D"
            INFO -> "I"
            WARN -> "W"
            ERROR -> "E"
            else -> String.format("NO_Level %s", LogLevel)
        }
        // 获取时间
        @SuppressLint("SimpleDateFormat") val ft = SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
        // 生成日志信息
        val logMessage = String.format("%s %s %s/%s: %s", ft.format(Date()), logObjectTag[1], logLevelString, tag, msg)
        // 获取日志列表
        val logSortString = logObjectTag[0]
        val logObjectId = logObjectTag[1]
        var logMessageArray = JSONArray()
        var logSortJson = JSONObject()
        if (logJSONObject.has(logSortString)) {
            try {
                logSortJson = logJSONObject.getJSONObject(logSortString)
            } catch (e: JSONException) {
                Log.e(TAG, "addLogMessage: 出乎意料的错误,  logJSONObject: $logJSONObject")
                e.printStackTrace()
            }

            if (logSortJson.has(logObjectId)) {
                try {
                    logMessageArray = logSortJson.getJSONArray(logObjectId)
                } catch (e: JSONException) {
                    Log.e(TAG, "addLogMessage: 出乎意料的错误, logSortJson: $logJSONObject")
                    e.printStackTrace()
                }

            }
        }
        // 向日志列表载入新日志
        logMessageArray.put(logMessage)
        try {
            logSortJson.put(logObjectId, logMessageArray)
            logJSONObject.put(logSortString, logSortJson)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        logLiveData.setLogJSONObject(logJSONObject)
    }

    private fun preMsg(msgObject: Any?): String {
        return when (msgObject) {
            null -> "NULL"
            Double::class.java -> msgObject.toString()
            else -> msgObject.toString()
        }
    }

    // 调用Log.v()方法打印日志
    fun v(logObjectTag: Array<String>, tag: String, msgObject: Any) {
        if (LEVEL <= VERBOSE) {
            val msg = preMsg(msgObject)
            Log.v(tag, msg)
            addLogMessage(VERBOSE, logObjectTag, tag, msg)
        }
    }

    // 调用Log.d()方法打印日志
    fun d(logObjectTag: Array<String>, tag: String, msgObject: Any) {
        if (LEVEL <= DEBUG) {
            val msg = preMsg(msgObject)
            Log.d(tag, msg)
            addLogMessage(DEBUG, logObjectTag, tag, msg)
        }
    }

    // 调用Log.i()方法打印日志
    fun i(logObjectTag: Array<String>, tag: String, msgObject: Any) {
        if (LEVEL <= INFO) {
            val msg = preMsg(msgObject)
            Log.i(tag, msg)
            addLogMessage(INFO, logObjectTag, tag, msg)
        }
    }

    // 调用Log.w()方法打印日志
    fun w(logObjectTag: Array<String>, tag: String, msgObject: Any) {
        if (LEVEL <= WARN) {
            val msg = preMsg(msgObject)
            Log.w(tag, msg)
            addLogMessage(WARN, logObjectTag, tag, msg)
        }
    }

    // 调用Log.e()方法打印日志
    fun e(logObjectTag: Array<String>, tag: String, msgObject: Any) {
        if (LEVEL <= ERROR) {
            val msg = preMsg(msgObject)
            Log.e(tag, msg)
            addLogMessage(ERROR, logObjectTag, tag, msg)
        }
    }

    companion object {

        private const val TAG = "LogUtil"

        /**
         * 定义6个静态常量，用来表示日志信息的打印等级
         * 由1到5打印等级依次升高
         */
        private const val VERBOSE = 1
        private const val DEBUG = 2
        private const val INFO = 3
        private const val WARN = 4
        private const val ERROR = 5
        private const val NOTHING = 6

        /**
         * 该静态常量的值用来控制你想打印的日志等级；
         * 比如当前LEVEL的值为常量1（VERBOSE），那么你以上5个日志等级都是可以打印的；
         * 假如当前LEVEL的值为常量2（DEBUG），那么你只能打印从DEBUG（2）到ERROR（5）之间的日志信息；
         * 假如你要是不想让日志信息打印出现，那么将LEVEL的值置为NOTHING即可。
         */
        private const val LEVEL = VERBOSE  // TODO: 设置中加入对该值的自定义
    }
}
