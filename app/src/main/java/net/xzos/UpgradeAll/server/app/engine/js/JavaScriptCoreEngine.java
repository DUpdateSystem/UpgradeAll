package net.xzos.UpgradeAll.server.app.engine.js;

import net.xzos.UpgradeAll.json.cache.JSCacheData;
import net.xzos.UpgradeAll.server.ServerContainer;
import net.xzos.UpgradeAll.server.app.engine.api.CoreApi;
import net.xzos.UpgradeAll.server.app.engine.js.utils.JSLog;
import net.xzos.UpgradeAll.server.app.engine.js.utils.JSUtils;
import net.xzos.UpgradeAll.server.log.LogUtil;

import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

class JavaScriptCoreEngine implements CoreApi {

    private static final String TAG = "JavaScriptCoreEngine";
    private String[] LogObjectTag;
    private static final LogUtil Log = ServerContainer.AppServer.getLog();

    private String URL;
    private String jsCode;

    private Context cx;
    private Scriptable scope;

    private JSUtils JSUtils;
    private JSLog JSLog;

    JavaScriptCoreEngine(String[] logObjectTag, String URL, String jsCode) {
        this.LogObjectTag = logObjectTag;
        this.URL = URL;
        this.jsCode = jsCode;
        JSUtils = new JSUtils(this.LogObjectTag);
        JSLog = new JSLog(this.LogObjectTag);
        JSCacheData JSCacheData = new JSCacheData();
        JSUtils.setJsCacheData(JSCacheData);
    }

    String[] getLogObjectTag() {
        return LogObjectTag;
    }

    // 加载 JavaScript 代码
    private boolean executeVoidScript() {
        if (jsCode == null) return false;
        boolean isSuccess = false;
        try {
            cx.evaluateString(scope, jsCode, null, 1, null);
            isSuccess = true;
        } catch (Throwable e) {
            Log.e(LogObjectTag, TAG, String.format("executeVoidScript: 脚本载入错误, ERROR_MESSAGE: %s", e.toString()));
        }
        return isSuccess;
    }

    private boolean initRhino() {
        // 初始化 rhino 对象
        cx = Context.enter();
        cx.setOptimizationLevel(-1);
        scope = cx.initStandardObjects();
        // 载入 JavaScript 实例
        RegisterJavaMethods();
        boolean methodsSuccess = executeVoidScript();
        if (!methodsSuccess) return false;
        ScriptableObject.putProperty(scope, "URL", URL);  // 初始化 URL
        return true;
    }

    private void closeRhino() {
        Context.exit();
    }

    // 注册 Java 代码
    private void RegisterJavaMethods() {
        // 爬虫库
        Object rhinoJSUtils = Context.javaToJS(JSUtils, scope);
        ScriptableObject.putProperty(scope, "JSUtils", rhinoJSUtils);
        // Log
        Object rhinoLogUtils = Context.javaToJS(JSLog, scope);
        ScriptableObject.putProperty(scope, "Log", rhinoLogUtils);
    }

    // 运行 JS 代码
    private Object runJS(String functionName, Object[] args) {
        if (!initRhino()) return null; // 初始化 J2V8
        Function function = (Function) scope.get(functionName, scope);
        Object result = function.call(cx, scope, scope, args);
        closeRhino(); // 销毁 J2V8 对象
        return result;
    }

    @Override
    public String getDefaultName() {
        String defaultName = Context.toString(runJS("getDefaultName", new Object[]{}));
        Log.d(LogObjectTag, TAG, "getDefaultName: defaultName: " + defaultName);
        return defaultName;
    }

    @Override
    public int getReleaseNum() {
        int releaseNum = (int) Context.toNumber(runJS("getReleaseNum", new Object[]{}));
        Log.d(LogObjectTag, TAG, "getReleaseNum: releaseNum: " + releaseNum);
        return releaseNum;
    }


    @Override
    public String getVersioning(int releaseNum) {
        Object result;
        Object[] args = {releaseNum};
        try {
            result = runJS("getVersioning", args);
        } catch (ClassCastException e) {
            // TODO: 向下兼容两个主版本后移除，当前版本：0.1.0-alpha.3
            Log.w(LogObjectTag, TAG, "getVersioning: 未找到 getVersioning 函数，尝试向下兼容");
            result = runJS("getVersionNumber", args);
        }
        String versionNumber = Context.toString(result);
        Log.d(LogObjectTag, TAG, "getVersioning: versionNumber: " + versionNumber);
        return versionNumber;
    }

    @Override
    public String getChangelog(int releaseNum) {
        Object[] args = {releaseNum};
        String changeLog = Context.toString(runJS("getChangelog", args));
        Log.d(LogObjectTag, TAG, "getChangelog: Changelog: " + changeLog);
        return changeLog;
    }

    @Override
    public JSONObject getReleaseDownload(int releaseNum) {
        Object[] args = {releaseNum};
        String fileJsonString = Context.toString(runJS("getReleaseDownload", args));
        JSONObject fileJson = new JSONObject();
        try {
            fileJson = new JSONObject(fileJsonString);
        } catch (JSONException e) {
            Log.e(LogObjectTag, TAG, "getReleaseDownload: 返回值不符合 JsonObject 规范, fileJsonString : " + fileJsonString);
        } catch (NullPointerException e) {
            Log.e(LogObjectTag, TAG, "getReleaseDownload: 返回值为 NULL, fileJsonString : " + fileJsonString);
        }
        Log.d(LogObjectTag, TAG, "getReleaseDownload:  fileJson: " + fileJson);
        return fileJson;
    }
}
