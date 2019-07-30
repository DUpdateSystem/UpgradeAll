package net.xzos.UpgradeAll.server.JSEngine;

import net.xzos.UpgradeAll.gson.JSCacheData;
import net.xzos.UpgradeAll.server.JSEngine.JSUtils.JSLog;
import net.xzos.UpgradeAll.server.JSEngine.JSUtils.JSUtils;
import net.xzos.UpgradeAll.server.JSEngine.api.Api;

import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class JavaScriptJEngine extends Api {

    private static final String TAG = "JavaScriptJEngine";
    private String[] LogObjectTag;

    private String URL;
    private String jsCode;

    private Context cx;
    private Scriptable scope;

    private JSUtils JSUtils;
    private net.xzos.UpgradeAll.server.JSEngine.JSUtils.JSLog JSLog;

    public JavaScriptJEngine(String[] logObjectTag, String URL, String jsCode) {
        this.LogObjectTag = logObjectTag;
        this.URL = URL;
        this.jsCode = jsCode;
        JSUtils = new JSUtils(this.LogObjectTag);
        JSLog = new JSLog(this.LogObjectTag);
        JSCacheData JSCacheData = new JSCacheData();
        JSUtils.setJsCacheData(JSCacheData);
        Log.i(this.LogObjectTag, TAG, String.format("JavaScriptJEngine: jsCode: \n%s", jsCode));
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
        Object functionObject = scope.get("getDefaultName", scope);
        Function function = (Function) functionObject;
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
        Object functionObject = scope.get("getReleaseNum", scope);
        Function function = (Function) functionObject;
        Object result = function.call(cx, scope, scope, new Object[]{});
        releaseNum = (int) Context.toNumber(result);
        Log.d(LogObjectTag, TAG, "getReleaseNum: releaseNum: " + releaseNum);
        closeRhino(); // 销毁 J2V8 对象
        return releaseNum;
    }


    @Override
    public String getVersionNumber(int releaseNum) {
        if (!initRhino()) return null; // 初始化 J2V8
        String versionNumber;
        Object functionObject = scope.get("getVersionNumber", scope);
        Function function = (Function) functionObject;
        Object[] args = {releaseNum};
        Object result = function.call(cx, scope, scope, args);
        versionNumber = Context.toString(result);
        Log.d(LogObjectTag, TAG, "getVersionNumber: versionNumber: " + versionNumber);
        closeRhino(); // 销毁 J2V8 对象
        return versionNumber;
    }

    @Override
    public JSONObject getReleaseDownload(int releaseNum) {
        if (!initRhino()) return null; // 初始化 J2V8
        String versionNumberString;
        Object functionObject = scope.get("getReleaseDownload", scope);
        Function function = (Function) functionObject;
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
