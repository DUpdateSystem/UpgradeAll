package net.xzos.UpgradeAll.data;

import org.json.JSONException;
import org.json.JSONObject;
import org.litepal.crud.LitePalSupport;

public class RepoDatabase extends LitePalSupport {
    private int id;
    private String name;
    private String api;
    private String url; //  方便用户获得源网址

    private String versionChecker;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getApi() {
        return api;
    }

    public void setApi(String api) {
        this.api = api;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        if (url.substring(url.length() - 1).equals("/")) {
            url = url.substring(0, url.length() - 1);
            // 判断url是否多余 /
        }
        this.url = url;
    }

    public JSONObject getVersionChecker() {
        try {
            return new JSONObject(versionChecker);
        } catch (JSONException e) {
            return null;
        }
    }

    public void setVersionChecker(JSONObject versionChecker) {
        this.versionChecker = versionChecker.toString();
    }

}