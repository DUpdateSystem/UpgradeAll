package net.xzos.UpgradeAll.gson;

import org.json.JSONObject;

public class JSCacheData {

    private JSONObject jsoup_dom_dict = new JSONObject();

    public void setJsoupDomDict(JSONObject jsoup_dom_dict) {
        this.jsoup_dom_dict = jsoup_dom_dict;
    }

    public JSONObject getJsoupDomDict() {
        return jsoup_dom_dict;
    }
}
