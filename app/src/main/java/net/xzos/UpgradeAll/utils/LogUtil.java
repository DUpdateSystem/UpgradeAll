package net.xzos.UpgradeAll.utils;

import android.util.Log;

import com.eclipsesource.v8.V8Object;

/**
 * 自定义的日志打印工具类
 */
public class LogUtil {

    /**
     * 定义6个静态常量，用来表示日志信息的打印等级
     * 由1到5打印等级依次升高
     */
    public static final int VERBOSE = 1;
    public static final int DEBUG = 2;
    public static final int INFO = 3;
    public static final int WARN = 4;
    public static final int ERROR = 5;
    public static final int NOTHING = 6;

    /**
     * 该静态常量的值用来控制你想打印的日志等级；
     * 比如当前LEVEL的值为常量1（VERBOSE），那么你以上5个日志等级都是可以打印的；
     * 假如当前LEVEL的值为常量2（DEBUG），那么你只能打印从DEBUG（2）到ERROR（5）之间的日志信息；
     * 假如你要是不想让日志信息打印出现，那么将LEVEL的值置为NOTHING即可。
     */
    public static final int LEVEL = VERBOSE;

    // 调用Log.v()方法打印日志
    public static void v(String tag, String msg) {
        if (LEVEL <= VERBOSE) {
            if (msg.getClass().equals(V8Object.class))
                msg = msg.toString();
            if (msg.getClass().equals(String.class))
                Log.v(tag, (String) msg);
        }
    }

    // 调用Log.d()方法打印日志
    public static void d(String tag, String msg) {
        if (LEVEL <= DEBUG) {
            if (msg.getClass().equals(V8Object.class))
                msg = msg.toString();
            if (msg.getClass().equals(String.class))
                Log.d(tag, (String) msg);
        }
    }

    // 调用Log.i()方法打印日志
    public static void i(String tag, String msg) {
        if (LEVEL <= INFO) {
            if (msg.getClass().equals(V8Object.class))
                msg = msg.toString();
            if (msg.getClass().equals(String.class))
                Log.i(tag, (String) msg);
        }
    }

    // 调用Log.w()方法打印日志
    public static void w(String tag, String msg) {
        if (LEVEL <= WARN) {
            if (msg.getClass().equals(V8Object.class))
                msg = msg.toString();
            if (msg.getClass().equals(String.class))
                Log.w(tag, (String) msg);
        }
    }

    // 调用Log.e()方法打印日志
    public static void e(String tag, Object msg) {
        if (LEVEL <= ERROR) {
            if (msg.getClass().equals(V8Object.class))
                msg = msg.toString();
            if (msg.getClass().equals(String.class))
                Log.e(tag, (String) msg);
        }
    }
}
