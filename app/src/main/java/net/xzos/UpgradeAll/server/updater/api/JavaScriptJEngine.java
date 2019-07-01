package net.xzos.UpgradeAll.server.updater.api;

import android.content.res.Resources;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.V8ResultUndefined;
import com.eclipsesource.v8.V8ScriptExecutionException;
import com.eclipsesource.v8.utils.MemoryManager;

import net.xzos.UpgradeAll.R;
import net.xzos.UpgradeAll.data.MyApplication;
import net.xzos.UpgradeAll.gson.HubConfig;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.seimicrawler.xpath.JXDocument;
import org.seimicrawler.xpath.JXNode;

import java.util.ArrayList;
import java.util.List;

public class JavaScriptJEngine extends Api {

    private static final String TAG = "JavaScriptJEngine";
    private String APITAG;

    private String URL;
    private HubConfig hubConfig;
    private int hubConfigVersion;
    private int hubConfigVersionJavaScript;
    private JSONObject JsoupDomDict = new JSONObject();
    private V8 v8 = null;
    private MemoryManager memoryManager = null;
    private V8Object WebCrawler = null;

    public JavaScriptJEngine(String URL, HubConfig hubConfig) {
        this.APITAG = URL;
        this.URL = URL;
        this.hubConfig = hubConfig;
        Resources resources = MyApplication.getContext().getResources();
        hubConfigVersionJavaScript = resources.getInteger(R.integer.hub_config_version_javascript);
        hubConfigVersion = this.hubConfig.getBaseVersion();
    }

    private void initJ2V8() {
        // 实例化 V2J8
        if (this.memoryManager != null) return;
        this.v8 = V8.createV8Runtime();
        this.memoryManager = new MemoryManager(v8); // 实例化 MemoryManager
        // 载入 JavaScript 实例
        RegisterJavaMethods();
        executeVoidScript();
        // 获取 WebCrawler 实例
        this.WebCrawler = v8.getObject("WebCrawler");
        this.WebCrawler.executeVoidFunction("setUrl", new V8Array(v8).push(URL)); // 初始化 URL
    }

    private void closeJ2V8() {
        if (this.memoryManager == null) return;
        this.memoryManager.release();
        this.WebCrawler = null;
        this.v8 = null;
        this.memoryManager = null;
    }

    @Override
    public void flashData() {
        initData();
    }

    /**
     * 初始化一些可能造成软件卡顿的网络数据
     * 建议但不强制要求
     *
     * @Version:0.0.9
     */
    private void initData() {
        if (hubConfigVersion < hubConfigVersionJavaScript) return;
        initJ2V8(); // 初始化 J2V8
        try {
            this.WebCrawler.executeVoidFunction("flashData", null);
        } catch (V8ScriptExecutionException e) {
            Log.e(APITAG, TAG, "initData: 脚本执行错误, ERROR_MESSAGE: " + e.toString());
        } catch (V8ResultUndefined e) {
            Log.e(APITAG, TAG, "initData: 函数返回值错误, ERROR_MESSAGE: " + e.toString());
        }
        closeJ2V8(); // 销毁 J2V8 对象
    }

    @Override
    public String getDefaultName() {
        if (hubConfigVersion < hubConfigVersionJavaScript) return super.getDefaultName();
        initJ2V8(); // 初始化 J2V8
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
        if (hubConfigVersion < hubConfigVersionJavaScript)
            return super.getReleaseNum();
        initJ2V8(); // 初始化 J2V8
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
        if (hubConfigVersion < hubConfigVersionJavaScript)
            return super.getVersionNumber(releaseNum);
        initJ2V8(); // 初始化 J2V8
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
        if (hubConfigVersion < hubConfigVersionJavaScript)
            return super.getReleaseDownload(releaseNum);
        initJ2V8(); // 初始化 J2V8
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

    // 注册 Java 代码
    private void RegisterJavaMethods() {
        // 爬虫库
        WebCrawler webCrawler = new WebCrawler();
        V8Object v8WebCrawler = new V8Object(v8);
        v8.add("webCrawler", v8WebCrawler);
        v8WebCrawler.registerJavaMethod(webCrawler, "selNByJsoupXpath", "selNByJsoupXpath", new Class<?>[]{String.class, String.class});
        v8WebCrawler.release();
        // Log
        V8Object v8Log = new V8Object(v8);
        v8.add("Log", v8Log);
        v8Log.registerJavaMethod(Log, "v", "v", new Class<?>[]{String.class, String.class, Object.class});
        v8Log.registerJavaMethod(Log, "d", "d", new Class<?>[]{String.class, String.class, Object.class});
        v8Log.registerJavaMethod(Log, "i", "i", new Class<?>[]{String.class, String.class, Object.class});
        v8Log.registerJavaMethod(Log, "w", "w", new Class<?>[]{String.class, String.class, Object.class});
        v8Log.registerJavaMethod(Log, "e", "e", new Class<?>[]{String.class, String.class, Object.class});
        v8Log.release();
    }

    // 加载 JavaScript 代码
    private void executeVoidScript() {
        v8.executeScript(hubConfig.getWebCrawler().getJavaScript());
    }

    private V8Array toV8Array(List<?> list) {
        V8Array v8Array = new V8Array(v8);
        for (Object item : list) {
            v8Array.push(item);
        }
        return v8Array;
    }

    /**
     * 爬虫相关库的打包集合
     * For JavaScript
     */
    private class WebCrawler {
        public V8Array selNByJsoupXpath(String URL, String xpath) {
            return toV8Array(selNByJsoupXpathJavaList(URL, xpath));
        }

        private List<String> selNByJsoupXpathJavaList(String URL, String xpath) {
            String userAgent = hubConfig.getWebCrawler().getUserAgent();
            Document doc = new Document(URL);
            if (JsoupDomDict.has(URL)) {
                try {
                    doc = (Document) JsoupDomDict.get(URL);
                    Log.d(APITAG, TAG, "selNByJsoupXpathJavaList: 从缓存加载, URL: " + URL);
                } catch (JSONException e) {
                    Log.e(APITAG, TAG, "selNByJsoupXpathJavaList: Jsoup 缓存队列无该对象, JsoupDomDict: " + JsoupDomDict);
                }
            } else {
                Connection connection = Jsoup.connect(URL);
                if (userAgent != null) connection.userAgent(userAgent);
                doc = JsoupApi.getDoc(connection);
                if (doc == null) {
                    Log.e(APITAG, TAG, "selNByJsoupXpathJavaList: Jsoup 对象初始化失败");
                    return new ArrayList<>();
                }
                try {
                    JsoupDomDict.put(URL, doc);
                    Log.d(APITAG, TAG, "selNByJsoupXpathJavaList: 缓存, URL: " + URL);
                } catch (JSONException e) {
                    Log.d(APITAG, TAG, "selNByJsoupXpathJavaList: 缓存失败, URL: " + URL);
                }
            }
            JXDocument JXDoc = JXDocument.create(doc);
            List<String> nodeStringArrayList = new ArrayList<>();
            for (JXNode node : JXDoc.selN((xpath))) {
                nodeStringArrayList.add(node.toString());
            }
            Log.d(APITAG, TAG, "selNByJsoupXpath: node_list: " + nodeStringArrayList);
            return nodeStringArrayList;
        }
    }
}
