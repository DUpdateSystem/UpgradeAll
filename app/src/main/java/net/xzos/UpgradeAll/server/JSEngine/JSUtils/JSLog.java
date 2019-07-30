package net.xzos.UpgradeAll.server.JSEngine.JSUtils;

import net.xzos.UpgradeAll.application.MyApplication;
import net.xzos.UpgradeAll.utils.log.LogUtil;

public class JSLog {

    protected static final LogUtil Log = MyApplication.getLog();

    private String[] LogObjectTag;
    private String TAG = "JavaScriptRunning";

    public JSLog(String[] logObjectTag) {
        this.LogObjectTag = logObjectTag;
    }

    public void v(Object msgObject) {
        Log.v(LogObjectTag, TAG, msgObject);
    }

    public void d(Object msgObject) {
        Log.d(LogObjectTag, TAG, msgObject);
    }

    public void i(Object msgObject) {
        Log.i(LogObjectTag, TAG, msgObject);
    }

    public void w(Object msgObject) {
        Log.w(LogObjectTag, TAG, msgObject);
    }

    public void e(Object msgObject) {
        Log.e(LogObjectTag, TAG, msgObject);
    }
}
