package net.xzos.upgradeAll.server.log

import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.application.MyApplication
import net.xzos.upgradeAll.data.json.nongson.ObjectTag
import java.text.SimpleDateFormat
import java.util.*

/**
 * 自定义的日志打印工具类
 */
internal object LogUtil {

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
     * 比如当前LEVEL的值为常量1（VERBOSE），那么你以上5个日志https://discuss.kotlinlang.org/t/are-kotlins-immutable-collections-thread-safe/35等级都是可以打印的；
     * 假如当前LEVEL的值为常量2（DEBUG），那么你只能打印从DEBUG（2）到ERROR（5）之间的日志信息；
     * 假如你要是不想让日志信息打印出现，那么将LEVEL的值置为NOTHING即可。
     */
    private var LEVEL = MyApplication.context.resources.getInteger(R.integer.log_level)  // TODO: 设置中加入对该值的自定义

    private val logMap = mutableMapOf<String, MutableMap<String, MutableList<String>>>()
    internal val mutex = Mutex()

    internal val logLiveData = LogLiveData(logMap)
    internal val logDataProxy = LogDataProxy(logMap)

    private fun <T> MutableLiveData<T>.notifyObserver() {
        runBlocking(Dispatchers.Main) {
            this@notifyObserver.value = this@notifyObserver.value
        }
    }

    /**
     * 定义一个JsonObject存储日志
     * {
     *   repo_database_id: [String_Log1, String_Log2, ]
     * }
     */

    private fun addLogMessage(LogLevel: Int, objectTag: ObjectTag, tag: String, msg: String) {
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
        // 获取日志列表
        val logSort = objectTag.sort
        val logObjectId = objectTag.id
        val logMessage = "${ft.format(Date())} $logObjectId $logLevelString/$tag: $msg"  // 生成日志信息
        GlobalScope.launch {
            mutex.withLock {
                (logMap[logSort]?.get(logObjectId)
                        ?: mutableListOf<String>().apply {
                            ((logMap[logSort]
                                    ?: mutableMapOf()).also { logMap[logSort] = it }
                                    )[logObjectId] = this
                        }).add(logMessage)
                notifyObserver()
            }
        }
    }

    internal fun notifyObserver() {
        logLiveData.mLogMapLiveData.notifyObserver()
    }

    // 调用Log.v()方法打印日志
    fun v(objectTag: ObjectTag, tag: String, msg: String) {
        if (LEVEL <= VERBOSE) {
            Log.v(tag, msg)
            addLogMessage(VERBOSE, objectTag, tag, msg)
        }
    }

    // 调用Log.d()方法打印日志
    fun d(logObjectTag: ObjectTag, tag: String, msg: String) {
        if (LEVEL <= DEBUG) {
            Log.d(tag, msg)
            addLogMessage(DEBUG, logObjectTag, tag, msg)
        }
    }

    // 调用Log.i()方法打印日志
    fun i(objectTag: ObjectTag, tag: String, msg: String) {
        if (LEVEL <= INFO) {
            Log.i(tag, msg)
            addLogMessage(INFO, objectTag, tag, msg)
        }
    }

    // 调用Log.w()方法打印日志
    fun w(objectTag: ObjectTag, tag: String, msg: String) {
        if (LEVEL <= WARN) {
            Log.w(tag, msg)
            addLogMessage(WARN, objectTag, tag, msg)
        }
    }

    // 调用Log.e()方法打印日志
    fun e(objectTag: ObjectTag, tag: String, msg: String) {
        if (LEVEL <= ERROR) {
            Log.e(tag, msg)
            addLogMessage(ERROR, objectTag, tag, msg)
        }
    }

}
