package net.xzos.UpgradeAll.server.app.engine.js.utils;

import net.xzos.UpgradeAll.server.ServerContainer;
import net.xzos.UpgradeAll.server.log.LogUtil;

public class JSLog {

    protected static final LogUtil Log = ServerContainer.AppServer.getLog();

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
