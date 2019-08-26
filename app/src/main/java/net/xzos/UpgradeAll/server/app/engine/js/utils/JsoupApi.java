package net.xzos.UpgradeAll.server.app.engine.js.utils;

import android.util.Log;

import org.jsoup.Connection;
import org.jsoup.nodes.Document;

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
