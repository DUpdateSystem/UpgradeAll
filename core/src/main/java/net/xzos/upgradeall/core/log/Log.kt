package net.xzos.upgradeall.core.log

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.core.utils.coroutines.CoroutinesMutableList
import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableListOf
import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableMapOf
import org.apache.commons.text.StringEscapeUtils
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
    const val VERBOSE = 1
    const val DEBUG = 2
    const val INFO = 3
    const val WARN = 4
    const val ERROR = 5
    const val NOTHING = 6

    /**
     * 该静态常量的值用来控制你想打印的日志等级；
     * 比如当前LEVEL的值为常量1（VERBOSE），那么你以上5个日志等级都是可以打印的；
     * 假如当前LEVEL的值为常量2（DEBUG），那么你只能打印从DEBUG（2）到ERROR（5）之间的日志信息；
     * 假如你要是不想让日志信息打印出现，那么将LEVEL的值置为NOTHING即可。
     */
    private var LEVEL = VERBOSE

    internal val logMap =
        coroutinesMutableMapOf<ObjectTag, CoroutinesMutableList<LogItemData>>(true)
    private val mutex = Mutex()

    /**
     * 定义一个JsonObject存储日志
     * {
     *   repo_database_id: [String_Log1, String_Log2, ]
     * }
     */

    /**
     * 向日志 Map 中添加日志内容
     */
    private fun addLogMessage(logItemData: LogItemData) {
        GlobalScope.launch {
            mutex.withLock {
                val logObjectTag = logItemData.logObjectTag
                (logMap[logObjectTag] ?: coroutinesMutableListOf<LogItemData>().also {
                    logMap[logObjectTag] = it
                }).add(logItemData)
            }
            notifyChange()
        }
    }

    /**
     * 通知日志变化（删除操作）
     */
    internal fun notifyChange() {
        runBlocking {
            LogNotify.logChanged(logMap)
        }
    }

    // 调用Log.v()方法打印日志
    fun v(logObjectTag: ObjectTag, tag: String, msg: String) {
        if (LEVEL <= VERBOSE) {
            val logItemData = LogItemData(VERBOSE, logObjectTag, tag, msg)
            addLogMessage(logItemData)
            LogNotify.printLog(logItemData)
        }
    }

    // 调用Log.d()方法打印日志
    fun d(logObjectTag: ObjectTag, tag: String, msg: String) {
        if (LEVEL <= DEBUG) {
            val logItemData = LogItemData(DEBUG, logObjectTag, tag, msg)
            addLogMessage(logItemData)
            LogNotify.printLog(logItemData)
        }
    }

    // 调用Log.i()方法打印日志
    fun i(logObjectTag: ObjectTag, tag: String, msg: String) {
        if (LEVEL <= INFO) {
            val logItemData = LogItemData(INFO, logObjectTag, tag, msg)
            addLogMessage(logItemData)
            LogNotify.printLog(logItemData)
        }
    }

    // 调用Log.w()方法打印日志
    fun w(logObjectTag: ObjectTag, tag: String, msg: String) {
        if (LEVEL <= WARN) {
            val logItemData = LogItemData(WARN, logObjectTag, tag, msg)
            addLogMessage(logItemData)
            LogNotify.printLog(logItemData)
        }
    }

    // 调用Log.e()方法打印日志
    fun e(logObjectTag: ObjectTag, tag: String, msg: String) {
        if (LEVEL <= ERROR) {
            val logItemData = LogItemData(ERROR, logObjectTag, tag, msg)
            addLogMessage(logItemData)
            LogNotify.printLog(logItemData)
        }
    }
}

/**
 * 存储单条日志信息
 */
class LogItemData(
    val logLevel: Int, val logObjectTag: ObjectTag, val tag: String, val msg: String
) {
    override fun toString(): String {
        // 确定日志等级标志
        val logLevelString: String = when (logLevel) {
            Log.VERBOSE -> "V"
            Log.DEBUG -> "D"
            Log.INFO -> "I"
            Log.WARN -> "W"
            Log.ERROR -> "E"
            else -> String.format("NO_Level %s", logLevel)
        }
        // 获取时间
        val ft = SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.ENGLISH)
        // 获取日志对象名称
        val logObjectName = logObjectTag.name
        // 生成日志信息
        val logString = "${ft.format(Date())} $logObjectName $logLevelString/$tag: $msg"
        return StringEscapeUtils.unescapeJava(logString)
    }
}
