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
    protected static final LogUtil Log = ServerContainer.AppServer.getLog();

    private String URL;
    private String jsCode;

    private Context cx;
    private Scriptable scope;

    private JSUtils JSUtils;
    private net.xzos.UpgradeAll.server.app.engine.js.utils.JSLog JSLog;

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

    @Override
    public String getDefaultName() {
        if (!initRhino()) return null; // 初始化 J2V8
        String defaultName;
        Function function = (Function) scope.get("getDefaultName", scope);
        Object result = function.call(cx, scope, scope, new Object[]{});
        defaultName = Context.toString(result);
        Log.d(LogObjectTag, TAG, "getDefaultName: defaultName: " + defaultName);
        closeRhino(); // 销毁 J2V8 对象
        return defaultName;
    }

    @Override
    public int getReleaseNum() {
        if (!initRhino()) return 0; // 初始化 J2V8
        int releaseNum;
        Function function = (Function) scope.get("getReleaseNum", scope);
        Object result = function.call(cx, scope, scope, new Object[]{});
        releaseNum = (int) Context.toNumber(result);
        Log.d(LogObjectTag, TAG, "getReleaseNum: releaseNum: " + releaseNum);
        closeRhino(); // 销毁 J2V8 对象
        return releaseNum;
    }


    @Override
    public String getVersioning(int releaseNum) {
        if (!initRhino()) return null; // 初始化 J2V8
        String versionNumber;
        Function function;
        try {
            function = (Function) scope.get("getVersioning", scope);
        } catch (ClassCastException e) {
            // TODO: 向下兼容两个主版本后移除，当前版本：0.1.0-alpha.3
            Log.w(LogObjectTag, TAG, "getVersioning: 未找到 getVersioning 函数，尝试向下兼容");
            function = (Function) scope.get("getVersionNumber", scope);
        }
        Object[] args = {releaseNum};
        Object result = function.call(cx, scope, scope, args);
        versionNumber = Context.toString(result);
        Log.d(LogObjectTag, TAG, "getVersioning: versionNumber: " + versionNumber);
        closeRhino(); // 销毁 J2V8 对象
        return versionNumber;
    }

    @Override
    public JSONObject getReleaseDownload(int releaseNum) {
        if (!initRhino()) return null; // 初始化 J2V8
        String versionNumberString;
        Function function = (Function) scope.get("getReleaseDownload", scope);
        Object[] args = {releaseNum};
        Object result = function.call(cx, scope, scope, args);
        versionNumberString = Context.toString(result);
        JSONObject releaseDownloadUrlJsonObject = new JSONObject();
        try {
            releaseDownloadUrlJsonObject = new JSONObject(versionNumberString);
        } catch (JSONException e) {
            Log.e(LogObjectTag, TAG, "getReleaseDownload: 返回值不符合 JsonObject 规范, versionNumberString : " + versionNumberString);
        } catch (NullPointerException e) {
            Log.e(LogObjectTag, TAG, "getReleaseDownload: 返回值为 NULL, versionNumberString : " + versionNumberString);
        }
        closeRhino(); // 销毁 J2V8 对象
        Log.d(LogObjectTag, TAG, "getReleaseDownload:  releaseDownloadUrlJsonObject: " + releaseDownloadUrlJsonObject);
        return releaseDownloadUrlJsonObject;
    }
}
