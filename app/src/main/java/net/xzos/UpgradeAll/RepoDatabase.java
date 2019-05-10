package net.xzos.UpgradeAll;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.litepal.crud.LitePalSupport;

public class RepoDatabase extends LitePalSupport {
    private static final String TAG = "RepoDatabase";
    private int id;
    private String name;
    private String api;
    private String url; //  方便用户获得源网址
    private String api_url;

    private String versionChecker;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    String getApi() {
        return api;
    }

    void setApi(String api) {
        this.api = api;
    }

    String getUrl() {
        return url;
    }

    void setUrl(String url) {
        if (url.substring(url.length() - 1).equals("/")) {
            url = url.substring(0, url.length() - 1);
            // 判断url是否多余 /
        }
        this.url = url;
    }

    String getApiUrl() {
        return api_url;
    }

    void setApiUrl(String api_url) {
        this.api_url = api_url;
    }

    JSONObject getVersionChecker() {
        try {
            Log.d(TAG, "getVersionChecker:  " + versionChecker);
            return new JSONObject(versionChecker);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new JSONObject();
    }

    void setVersionChecker(JSONObject versionChecker) {
        this.versionChecker = versionChecker.toString();
    }
}
