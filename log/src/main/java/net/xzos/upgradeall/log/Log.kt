package net.xzos.upgradeall.log

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.data.json.nongson.ObjectTag
import net.xzos.upgradeall.system_api.api.LogApi
import java.text.SimpleDateFormat
import java.util.*


/**
 * 自定义的日志打印工具类
 */
object Log {

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
    private var LEVEL = VERBOSE  // TODO: 设置中加入对该值的自定义

    val logMap = hashMapOf<ObjectTag, MutableList<String>>()
    private val mutex = Mutex()

    /**
     * 定义一个JsonObject存储日志
     * {
     *   repo_database_id: [String_Log1, String_Log2, ]
     * }
     */

    private fun addLogMessage(LogLevel: Int, logObjectTag: ObjectTag, tag: String, msg: String) {
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
        val ft = SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.ENGLISH)
        // 获取日志列表
        val logObjectId = logObjectTag.name
        val logMessage = "${ft.format(Date())} $logObjectId $logLevelString/$tag: $msg"  // 生成日志信息
        GlobalScope.launch {
            mutex.withLock {
                (logMap[logObjectTag] ?: mutableListOf<String>().also {
                    logMap[logObjectTag] = it
                }).add(logMessage)
                notifyChange()
            }
        }
    }

    // 通知日志变化
    internal fun notifyChange() {
        // TODO
    }

    // 调用Log.v()方法打印日志
    fun v(logObjectTag: ObjectTag, tag: String, msg: String) {
        if (LEVEL <= VERBOSE) {
            LogApi.v(logObjectTag, tag, msg)
            addLogMessage(VERBOSE, logObjectTag, tag, msg)
        }
    }

    // 调用Log.d()方法打印日志
    fun d(logObjectTag: ObjectTag, tag: String, msg: String) {
        if (LEVEL <= DEBUG) {
            LogApi.d(logObjectTag, tag, msg)
            addLogMessage(DEBUG, logObjectTag, tag, msg)
        }
    }

    // 调用Log.i()方法打印日志
    fun i(logObjectTag: ObjectTag, tag: String, msg: String) {
        if (LEVEL <= INFO) {
            LogApi.i(logObjectTag, tag, msg)
            addLogMessage(INFO, logObjectTag, tag, msg)
        }
    }

    // 调用Log.w()方法打印日志
    fun w(logObjectTag: ObjectTag, tag: String, msg: String) {
        if (LEVEL <= WARN) {
            LogApi.w(logObjectTag, tag, msg)
            addLogMessage(WARN, logObjectTag, tag, msg)
        }
    }

    // 调用Log.e()方法打印日志
    fun e(logObjectTag: ObjectTag, tag: String, msg: String) {
        if (LEVEL <= ERROR) {
            LogApi.e(logObjectTag, tag, msg)
            addLogMessage(ERROR, logObjectTag, tag, msg)
        }
    }
}
