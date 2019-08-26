package net.xzos.UpgradeAll.server.app.engine.js.utils;

import net.xzos.UpgradeAll.application.MyApplication;
import net.xzos.UpgradeAll.gson.JSCacheData;
import net.xzos.UpgradeAll.server.log.LogUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.seimicrawler.xpath.JXDocument;
import org.seimicrawler.xpath.JXNode;

import java.util.ArrayList;

/**
 * 爬虫相关库的打包集合
 * For JavaScript
 */
public class JSUtils {

    private static final LogUtil Log = MyApplication.getServerContainer().getLog();
    private static final String TAG = "JSUtils";
    private String[] logObjectTag;

    private JSCacheData jsCacheData = new JSCacheData();

    public JSUtils(String[] logObjectTag) {
        this.logObjectTag = logObjectTag;
    }

    JSCacheData getJsCacheData() {
        return jsCacheData;
    }

    public void setJsCacheData(JSCacheData jsCacheData) {
        this.jsCacheData = jsCacheData;
    }


    public JSONObject getJSONObject() {
        return new JSONObject();
    }

    public JSONArray getJSONArray() {
        return new JSONArray();
    }

    public JSONObject getJSONObject(String jsonString) throws JSONException {
        return new JSONObject(jsonString);
    }

    public JSONArray getJSONArray(String jsonString) throws JSONException {
        return new JSONArray(jsonString);

    }

    public String getHttpResponse(String URL) {
        JSONObject httpResponseDict = jsCacheData.getHttpResponseDict();
        String responseString = null;
        if (httpResponseDict.has(URL)) {
            try {
                responseString = httpResponseDict.getString(URL);
                Log.d(logObjectTag, TAG, "getHttpResponse: 从缓存加载, URL: " + URL);
            } catch (JSONException e) {
                Log.e(logObjectTag, TAG, "getHttpResponse: HTTP 缓存队列无该对象, httpResponseDict : " + httpResponseDict);
            }
        } else {
            responseString = OkHttpApi.getHttpResponse(logObjectTag, URL);
            if (responseString != null) {
                try {
                    httpResponseDict.put(URL, responseString);
                    Log.d(logObjectTag, TAG, "getHttpResponse: 缓存, URL: " + URL);
                } catch (JSONException e) {
                    Log.d(logObjectTag, TAG, "getHttpResponse: 缓存失败, URL: " + URL);
                }
            }
        }
        return responseString;
    }

    public ArrayList selNByJsoupXpath(String userAgent, String URL, String xpath) {
        Document doc = new Document(URL);
        JSONObject jsoupDomDict = jsCacheData.getJsoupDomDict();
        if (jsoupDomDict.has(URL)) {
            try {
                doc = (Document) jsoupDomDict.get(URL);
                Log.d(logObjectTag, TAG, "selNByJsoupXpathJavaList: 从缓存加载, URL: " + URL);
            } catch (JSONException e) {
                Log.e(logObjectTag, TAG, "selNByJsoupXpathJavaList: Jsoup 缓存队列无该对象, jsoupDomDict: " + jsoupDomDict);
            }
        } else {
            Connection connection = Jsoup.connect(URL);
            if (userAgent != null) connection.userAgent(userAgent);
            doc = JsoupApi.getDoc(connection);
            if (doc == null) {
                Log.e(logObjectTag, TAG, "selNByJsoupXpathJavaList: Jsoup 对象初始化失败");
                return new ArrayList<>();
            }
            try {
                jsoupDomDict.put(URL, doc);
                Log.d(logObjectTag, TAG, "selNByJsoupXpathJavaList: 缓存, URL: " + URL);
            } catch (JSONException e) {
                Log.d(logObjectTag, TAG, "selNByJsoupXpathJavaList: 缓存失败, URL: " + URL);
            }
        }
        JXDocument JXDoc = JXDocument.create(doc);
        ArrayList<String> nodeStringArrayList = new ArrayList<>();
        for (JXNode node : JXDoc.selN((xpath))) {
            nodeStringArrayList.add(node.toString());
        }
        Log.d(logObjectTag, TAG, "selNByJsoupXpath: node_list number: " + nodeStringArrayList.size());
        return nodeStringArrayList;
    }
}

