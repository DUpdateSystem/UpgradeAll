package net.xzos.UpgradeAll.updater.api;

import android.content.res.Resources;
import android.util.Log;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;

import net.xzos.UpgradeAll.R;
import net.xzos.UpgradeAll.data.MyApplication;
import net.xzos.UpgradeAll.gson.HubConfig;
import net.xzos.UpgradeAll.utils.LogUtil;

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

    private String url;
    private HubConfig hubConfig;
    private int hubConfigVersion;
    private int hubConfigVersionJavaScript;
    private V8 v8 = V8.createV8Runtime();

    public JavaScriptJEngine(String url, HubConfig hubConfig) {
        this.url = url;
        this.hubConfig = hubConfig;
        Resources resources = MyApplication.getContext().getResources();
        hubConfigVersionJavaScript = resources.getInteger(R.integer.hub_config_version_javascript);
        hubConfigVersion = this.hubConfig.getBaseVersion();
        // 加载 JavaScript 相关项
        RegisterJavaMethods();
        executeVoidScript();
    }


    // 初始化 URL
    @Override
    public void flashData() {
        if (hubConfigVersion < hubConfigVersionJavaScript) return;
        v8.executeStringFunction("setUrl", new V8Array(v8).push(url));
    }

    @Override
    public String getDefaultName() {
        if (hubConfigVersion < hubConfigVersionJavaScript) return super.getDefaultName();
        return v8.executeStringFunction("getDefaultName", null);
    }

    @Override
    public String getVersionNumber(int releaseNum) {
        if (hubConfigVersion < hubConfigVersionJavaScript)
            return super.getVersionNumber(releaseNum);
        return v8.executeStringFunction("getVersionNumber", new V8Array(v8).push(releaseNum));
    }

    @Override
    public JSONObject getReleaseDownload(int releaseNum) {
        if (hubConfigVersion < hubConfigVersionJavaScript)
            return super.getReleaseDownload(releaseNum);
        V8Array releaseDownloadV8Array = v8.executeArrayFunction("getVersionNumber", new V8Array(v8).push(releaseNum));
        JSONObject releaseDownloadUrlJsonObject = (JSONObject) releaseDownloadV8Array.get(0);
        return releaseDownloadUrlJsonObject;
    }

    // 注册 Java 代码
    private void RegisterJavaMethods() {
        // 爬虫库
        WebCrawler webCrawler = new WebCrawler();
        V8Object v8WebCrawler = new V8Object(v8);
        v8.add("webCrawler", v8WebCrawler);
        v8WebCrawler.registerJavaMethod(webCrawler, "selNByJsoupXpath", "selNByJsoupXpath", new Class<?>[]{String.class});
        v8WebCrawler.release();
        // Log
        LogUtil Log = new LogUtil();
        V8Object v8Log = new V8Object(v8);
        v8.add("Log", v8Log);
        v8Log.registerJavaMethod(Log, "v", "v", new Class<?>[]{String.class});
        v8Log.registerJavaMethod(Log, "d", "d", new Class<?>[]{String.class});
        v8Log.registerJavaMethod(Log, "i", "i", new Class<?>[]{String.class});
        v8Log.registerJavaMethod(Log, "w", "w", new Class<?>[]{String.class});
        v8Log.registerJavaMethod(Log, "e", "e", new Class<?>[]{String.class});
        v8Log.release();
    }

    // 加载 JavaScript 代码
    private void executeVoidScript() {
        v8.executeScript(hubConfig.getWebCrawler().getJavaScript());
    }

    /**
     * 爬虫相关库的打包集合
     * For JavaScript
     */
    private class WebCrawler {
        private List<String> selNByJsoupXpath(String URL, String xpath) {
            String userAgent = hubConfig.getWebCrawler().getUserAgent();
            Connection connection = Jsoup.connect(URL);
            if (userAgent != null) connection.userAgent(userAgent);
            Document doc;
            try {
                doc = connection.get();
            } catch (Throwable e) {
                Log.e(TAG, " getStringByJsoupXpath: Jsoup 对象初始化失败");
                e.printStackTrace();
                return new ArrayList<>();
            }
            JXDocument JXDoc = JXDocument.create(doc);
            List<String> nodeStringArrayList = new ArrayList<>();
            for (JXNode node : JXDoc.selN((xpath))) {
                nodeStringArrayList.add(node.toString());
            }
            return nodeStringArrayList;
        }
    }

    public JSONObject runFunction(ArrayList<String> targetStringList, String javaScriptString) {
        // 初始化
        V8 runtime = V8.createV8Runtime();
        runtime.executeScript(javaScriptString);
        // 转换输入字符串列表
        V8Array targetStringV8Array = new V8Array(runtime);
        for (String targetString : targetStringList)
            targetStringV8Array.push(targetString);
        // 运行 JavaScript 函数
        V8Array returnV8Array = runtime.executeArrayFunction("main", targetStringV8Array);
        // 获取网址
        String url = returnV8Array.getString("url");
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("url", url);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }
}
