package net.xzos.UpgradeAll.server.JSEngine;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.V8ResultUndefined;
import com.eclipsesource.v8.V8ScriptCompilationException;
import com.eclipsesource.v8.V8ScriptExecutionException;
import com.eclipsesource.v8.utils.MemoryManager;

import net.xzos.UpgradeAll.gson.JSCacheData;
import net.xzos.UpgradeAll.server.updater.api.Api;

import org.json.JSONException;
import org.json.JSONObject;

public class JavaScriptJEngine extends Api {

    private static final String TAG = "JavaScriptJEngine";
    private String APITAG;

    private String URL;
    private String jsCode;

    private V8 v8 = null;
    private MemoryManager memoryManager = null;
    private V8Object WebCrawler = null;

    private JSCacheData JSCacheData = new JSCacheData();

    public JavaScriptJEngine(String URL, String jsCode) {
        this.APITAG = URL;
        this.URL = URL;
        this.jsCode = jsCode;
    }

    // 加载 JavaScript 代码
    private boolean executeVoidScript() {
        boolean isSuccess = false;
        try {
            v8.executeScript(this.jsCode);
            isSuccess = true;
        } catch (V8ScriptCompilationException e) {
            Log.e(APITAG, TAG, "executeVoidScript: 脚本载入错误, ERROR_MESSAGE: " + e.toString());
        }
        return isSuccess;
    }

    private boolean initJ2V8() {
        // 实例化 V2J8
        if (this.v8 != null) return false;
        this.v8 = V8.createV8Runtime();
        this.memoryManager = new MemoryManager(v8); // 实例化 MemoryManager
        // 载入 JavaScript 实例
        RegisterJavaMethods();
        boolean methodsSuccess = executeVoidScript();
        if (!methodsSuccess) return false;
        // 获取 JSUtils 实例
        this.WebCrawler = v8.getObject("WebCrawler");
        this.WebCrawler.add("URL", URL);  // 初始化 URL
        return true;
    }

    private void closeJ2V8() {
        if (this.memoryManager == null) return;
        this.memoryManager.release();
        this.memoryManager = null;
        this.WebCrawler = null;
        this.v8 = null;
    }

    // 注册 Java 代码
    private void RegisterJavaMethods() {
        // 爬虫库
        JSUtils JSUtils = new JSUtils(v8, APITAG);
        JSUtils.setJsoupDomDict(JSCacheData.getJsoupDomDict());
        V8Object v8JSUtils = new V8Object(v8);
        v8.add("JSUtils", v8JSUtils);
        v8JSUtils.registerJavaMethod(JSUtils, "selNByJsoupXpath", "selNByJsoupXpath", new Class<?>[]{String.class, String.class, String.class});
        v8JSUtils.registerJavaMethod(JSUtils, "getHttpResponse", "getHttpResponse", new Class<?>[]{String.class});
        // Log
        V8Object v8Log = new V8Object(v8);
        v8.add("Log", v8Log);
        v8Log.registerJavaMethod(Log, "v", "v", new Class<?>[]{String.class, String.class, Object.class});
        v8Log.registerJavaMethod(Log, "d", "d", new Class<?>[]{String.class, String.class, Object.class});
        v8Log.registerJavaMethod(Log, "i", "i", new Class<?>[]{String.class, String.class, Object.class});
        v8Log.registerJavaMethod(Log, "w", "w", new Class<?>[]{String.class, String.class, Object.class});
        v8Log.registerJavaMethod(Log, "e", "e", new Class<?>[]{String.class, String.class, Object.class});
    }

    /**
     * 初始化一些可能造成软件卡顿的网络数据
     * 建议但不强制要求
     *
     * @Version:0.0.9
     */
    @Override
    public boolean initData() {
        boolean isSuccess = false;
        if (!initJ2V8()) return false; // 初始化 J2V8
        try {
            this.WebCrawler.executeVoidFunction("initData", null);
            isSuccess = true;
        } catch (V8ScriptExecutionException e) {
            Log.e(APITAG, TAG, "initData: 脚本执行错误, ERROR_MESSAGE: " + e.toString());
        } catch (V8ResultUndefined e) {
            Log.e(APITAG, TAG, "initData: 函数返回值错误, ERROR_MESSAGE: " + e.toString());
        }
        closeJ2V8(); // 销毁 J2V8 对象
        return isSuccess;
    }

    @Override
    public String getDefaultName() {
        if (!initJ2V8()) return null; // 初始化 J2V8
        String defaultName = null;
        try {
            defaultName = this.WebCrawler.executeStringFunction("getDefaultName", null);
        } catch (V8ScriptExecutionException e) {
            Log.e(APITAG, TAG, "getDefaultName: 脚本执行错误, ERROR_MESSAGE: " + e.toString());
        } catch (V8ResultUndefined e) {
            Log.e(APITAG, TAG, "getDefaultName: 函数返回值错误, ERROR_MESSAGE: " + e.toString());
        }
        Log.d(APITAG, TAG, "getDefaultName: defaultName: " + defaultName);
        closeJ2V8(); // 销毁 J2V8 对象
        return defaultName;
    }

    @Override
    public int getReleaseNum() {
        if (!initJ2V8()) return 0; // 初始化 J2V8
        int releaseNum = 0;
        try {
            releaseNum = this.WebCrawler.executeIntegerFunction("getReleaseNum", null);
        } catch (V8ScriptExecutionException e) {
            Log.e(APITAG, TAG, "getReleaseNum: 脚本执行错误, ERROR_MESSAGE: " + e.toString());
        } catch (V8ResultUndefined e) {
            Log.e(APITAG, TAG, "getReleaseNum: 函数返回值错误, ERROR_MESSAGE: " + e.toString());
        }
        Log.d(APITAG, TAG, "getReleaseNum: releaseNum: " + releaseNum);
        closeJ2V8(); // 销毁 J2V8 对象
        return releaseNum;
    }


    @Override
    public String getVersionNumber(int releaseNum) {
        if (!initJ2V8()) return null; // 初始化 J2V8
        String versionNumber = null;
        try {
            versionNumber = this.WebCrawler.executeStringFunction("getVersionNumber", new V8Array(v8).push(releaseNum));
        } catch (V8ScriptExecutionException e) {
            Log.e(APITAG, TAG, "getVersionNumber: 脚本执行错误, ERROR_MESSAGE: " + e.toString());
        } catch (V8ResultUndefined e) {
            Log.e(APITAG, TAG, "getVersionNumber: 函数返回值错误, ERROR_MESSAGE: " + e.toString());
        }
        Log.d(APITAG, TAG, "getVersionNumber: versionNumber: " + versionNumber);
        memoryManager.release();
        closeJ2V8(); // 销毁 J2V8 对象
        return versionNumber;
    }

    @Override
    public JSONObject getReleaseDownload(int releaseNum) {
        if (!initJ2V8()) return null; // 初始化 J2V8
        String versionNumberString = null;
        try {
            versionNumberString = this.WebCrawler.executeStringFunction("getReleaseDownload", new V8Array(v8).push(releaseNum));
        } catch (V8ScriptExecutionException e) {
            Log.e(APITAG, TAG, "getReleaseDownload: 脚本执行错误, ERROR_MESSAGE: " + e.toString());
        } catch (V8ResultUndefined e) {
            Log.e(APITAG, TAG, "getReleaseDownload: 函数返回值错误, ERROR_MESSAGE: " + e.toString());
        }
        JSONObject releaseDownloadUrlJsonObject = new JSONObject();
        try {
            releaseDownloadUrlJsonObject = new JSONObject(versionNumberString);
        } catch (JSONException e) {
            Log.e(APITAG, TAG, "getReleaseDownload: 返回值不符合 JsonObject 规范, versionNumberString : " + versionNumberString);
        } catch (NullPointerException e) {
            Log.e(APITAG, TAG, "getReleaseDownload: 返回值为 NULL, versionNumberString : " + versionNumberString);
        }
        closeJ2V8(); // 销毁 J2V8 对象
        Log.d(APITAG, TAG, "getReleaseDownload:  releaseDownloadUrlJsonObject: " + releaseDownloadUrlJsonObject);
        return releaseDownloadUrlJsonObject;
    }
}
