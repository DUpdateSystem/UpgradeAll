package net.xzos.UpgradeAll.gson;

import org.json.JSONObject;

public class JSCacheData {

    private JSONObject jsoup_dom_dict = new JSONObject();
    private JSONObject http_response_dict = new JSONObject();

    public JSONObject getJsoupDomDict() {
        return jsoup_dom_dict;
    }

    public void setJsoupDomDict(JSONObject jsoup_dom_dict) {
        this.jsoup_dom_dict = jsoup_dom_dict;
    }

    public JSONObject getHttpResponseDict() {
        return http_response_dict;
    }

    public void setHttpResponseDict(JSONObject http_response_dict) {
        this.http_response_dict = http_response_dict;
    }
}
