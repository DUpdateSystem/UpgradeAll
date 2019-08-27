package net.xzos.UpgradeAll.json.cache;

import org.json.JSONObject;

public class JSCacheData {

    private JSONObject jsoup_dom_dict = new JSONObject();
    private JSONObject http_response_dict = new JSONObject();

    public JSONObject getJsoupDomDict() {
        return jsoup_dom_dict;
    }

    public JSONObject getHttpResponseDict() {
        return http_response_dict;
    }
}
