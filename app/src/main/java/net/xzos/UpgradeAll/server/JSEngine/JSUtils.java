package net.xzos.UpgradeAll.server.JSEngine;

import android.util.Log;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.utils.V8ObjectUtils;

import net.xzos.UpgradeAll.application.MyApplication;
import net.xzos.UpgradeAll.gson.JSCacheData;
import net.xzos.UpgradeAll.server.updater.api.GithubApi;
import net.xzos.UpgradeAll.utils.LogUtil;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.seimicrawler.xpath.JXDocument;
import org.seimicrawler.xpath.JXNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 爬虫相关库的打包集合
 * For JavaScript
 */
class JSUtils {

    private V8 v8;

    private static final LogUtil Log = MyApplication.getLog();
    private static final String TAG = "JSUtils";
    private String APITAG;

    private JSCacheData jsCacheData = new JSCacheData();

    JSUtils(V8 v8, String APITAG) {
        this.v8 = v8;
        this.APITAG = APITAG;
    }

    JSCacheData getJsCacheData() {
        return jsCacheData;
    }

    void setJsCacheData(JSCacheData jsCacheData) {
        this.jsCacheData = jsCacheData;
    }

    public String getHttpResponse(String URL) {
        JSONObject httpResponseDict = jsCacheData.getHttpResponseDict();
        String responseString = null;
        if (httpResponseDict.has(URL)) {
            try {
                responseString = httpResponseDict.getString(URL);
                Log.d(APITAG, TAG, "getHttpResponse: 从缓存加载, URL: " + URL);
            } catch (JSONException e) {
                Log.e(APITAG, TAG, "getHttpResponse: HTTP 缓存队列无该对象, httpResponseDict : " + httpResponseDict);
            }
        } else {
            responseString = OkHttpApi.getHttpResponse(APITAG, URL);
            if (responseString != null) {
                try {
                    httpResponseDict.put(URL, responseString);
                    Log.d(APITAG, TAG, "getHttpResponse: 缓存, URL: " + URL);
                } catch (JSONException e) {
                    Log.d(APITAG, TAG, "getHttpResponse: 缓存失败, URL: " + URL);
                }
            }
        }
        return responseString;
    }

    public V8Array selNByJsoupXpath(String userAgent, String URL, String xpath) {
        Document doc = new Document(URL);
        JSONObject jsoupDomDict = jsCacheData.getJsoupDomDict();
        if (jsoupDomDict.has(URL)) {
            try {
                doc = (Document) jsoupDomDict.get(URL);
                Log.d(APITAG, TAG, "selNByJsoupXpathJavaList: 从缓存加载, URL: " + URL);
            } catch (JSONException e) {
                Log.e(APITAG, TAG, "selNByJsoupXpathJavaList: Jsoup 缓存队列无该对象, jsoupDomDict: " + jsoupDomDict);
            }
        } else {
            Connection connection = Jsoup.connect(URL);
            if (userAgent != null) connection.userAgent(userAgent);
            doc = JsoupApi.getDoc(connection);
            if (doc == null) {
                Log.e(APITAG, TAG, "selNByJsoupXpathJavaList: Jsoup 对象初始化失败");
                return new V8Array(v8);
            }
            try {
                jsoupDomDict.put(URL, doc);
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
        Log.d(APITAG, TAG, "selNByJsoupXpath: node_list number: " + nodeStringArrayList.size());
        return V8ObjectUtils.toV8Array(v8, nodeStringArrayList);
    }
}

class JsoupApi {
    private static final String TAG = "JsoupApi";

    static Document getDoc(Connection connection) {
        Document doc;
        try {
            doc = connection.get();
        } catch (Throwable e) {
            Log.e(TAG, "getStringByJsoupXpath: Jsoup 对象初始化失败");
            e.printStackTrace();
            doc = null;
        }
        return doc;
    }
}

class OkHttpApi {
    private static final LogUtil Log = MyApplication.getLog();
    private static final String TAG = "OkHttpApi";

    static String getHttpResponse(String APITAG, String api_url) {
        String responseString = null;
        Response response = null;
        OkHttpClient client = new OkHttpClient();
        Request.Builder builder = new Request.Builder();
        builder.url(api_url);
        Request request = builder.build();
        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            Log.e(APITAG, TAG, "getHttpResponse:  网络错误");
        }
        if (response != null) {
            try {
                responseString = Objects.requireNonNull(response.body()).string();
            } catch (IOException e) {
                Log.e(APITAG, TAG, "getHttpResponse: ERROR_MESSAGE: " + e.toString());
            }
        }
        return responseString;
    }
}