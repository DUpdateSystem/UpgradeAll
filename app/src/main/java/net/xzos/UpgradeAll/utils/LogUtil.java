package net.xzos.UpgradeAll.utils;

import android.annotation.SuppressLint;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 自定义的日志打印工具类
 */
public class LogUtil {

    private static final String TAG = "LogUtil";

    /**
     * 定义6个静态常量，用来表示日志信息的打印等级
     * 由1到5打印等级依次升高
     */
    private static final int VERBOSE = 1;
    private static final int DEBUG = 2;
    private static final int INFO = 3;
    private static final int WARN = 4;
    private static final int ERROR = 5;
    private static final int NOTHING = 6;

    /**
     * 该静态常量的值用来控制你想打印的日志等级；
     * 比如当前LEVEL的值为常量1（VERBOSE），那么你以上5个日志等级都是可以打印的；
     * 假如当前LEVEL的值为常量2（DEBUG），那么你只能打印从DEBUG（2）到ERROR（5）之间的日志信息；
     * 假如你要是不想让日志信息打印出现，那么将LEVEL的值置为NOTHING即可。
     */
    private static final int LEVEL = VERBOSE;  // TODO: 设置中加入对该值的自定义

    /**
     * 定义一个JsonObject存储日志
     * {
     * repo_database_id: [String_Log1, String_Log2, ]
     * }
     */
    private JSONObject logJSONObject = new JSONObject();

    private void addLogMessage(int LogLevel, String logObjectTag, String tag, String msg) {
        // 确定日志等级标志
        String logLevelString;
        switch (LogLevel) {
            case VERBOSE:
                logLevelString = "V";
                break;
            case DEBUG:
                logLevelString = "D";
                break;
            case INFO:
                logLevelString = "I";
                break;
            case WARN:
                logLevelString = "W";
                break;
            case ERROR:
                logLevelString = "E";
                break;
            default:
                logLevelString = String.format("NO_Level %s", LogLevel);
                break;
        }
        // 获取时间
        @SuppressLint("SimpleDateFormat") SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        // 生成日志信息
        String logMessage = String.format("%s %s %s/%s: %s", ft.format(new Date()), logObjectTag, logLevelString, tag, msg);
        // 获取日志列表
        JSONArray logMessageArray = new JSONArray();
        if (logJSONObject.has(logObjectTag)) {
            try {
                logMessageArray = logJSONObject.getJSONArray(logObjectTag);
            } catch (JSONException e) {
                Log.e(TAG, "addLogMessage: 出乎意料的错误， logJSONObject: " + logJSONObject);
                e.printStackTrace();
            }
        }
        // 向日志列表载入新日志
        logMessageArray.put(logMessage);
        try {
            logJSONObject.put(logObjectTag, logMessageArray);
        } catch (JSONException e) {
            Log.e(TAG, "addLogMessage: 出乎意料的错误， logJSONObject: " + logJSONObject);
            e.printStackTrace();
        }
    }

    private String preMsg(Object msgObject) {
        String msg;
        if (msgObject == null)
            msg = "NULL";
        else
            msg = (String) msgObject;
        return msg;
    }

    public JSONObject getLogMessage() {
        return this.logJSONObject;
    }

    // 调用Log.v()方法打印日志
    public void v(String logObjectTag, String tag, Object msgObject) {
        if (LEVEL <= VERBOSE) {
            String msg = preMsg(msgObject);
            Log.v(tag, msg);
            addLogMessage(VERBOSE, logObjectTag, tag, msg);
        }
    }

    // 调用Log.d()方法打印日志
    public void d(String logObjectTag, String tag, Object msgObject) {
        if (LEVEL <= DEBUG) {
            String msg = preMsg(msgObject);
            Log.d(tag, msg);
            addLogMessage(DEBUG, logObjectTag, tag, msg);
        }
    }

    // 调用Log.i()方法打印日志
    public void i(String logObjectTag, String tag, Object msgObject) {
        if (LEVEL <= INFO) {
            String msg = preMsg(msgObject);
            Log.i(tag, msg);
            addLogMessage(INFO, logObjectTag, tag, msg);
        }
    }

    // 调用Log.w()方法打印日志
    public void w(String logObjectTag, String tag, Object msgObject) {
        if (LEVEL <= WARN) {
            String msg = preMsg(msgObject);
            Log.w(tag, msg);
            addLogMessage(WARN, logObjectTag, tag, msg);
        }
    }

    // 调用Log.e()方法打印日志
    public void e(String logObjectTag, String tag, Object msgObject) {
        if (LEVEL <= ERROR) {
            String msg = preMsg(msgObject);
            Log.e(tag, msg);
            addLogMessage(ERROR, logObjectTag, tag, msg);
        }
    }
}
