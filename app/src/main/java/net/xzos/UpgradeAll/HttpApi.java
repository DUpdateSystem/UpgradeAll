package net.xzos.UpgradeAll;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


class HttpApi {

    JSONObject getLatestRelease() {
        return null;
    }

    String getVersion(int releaseNum) {
        /*返回云端版本号*/
        return null;
    }

    JSONObject getReleaseDownloadUrl(int releaseNum) {
        /*
         * 获取特定版本的下载链接
         *
         * 预期的返回值:
         * {
         *       下载文件名: 下载地址(最好为直链，否则提供直接导向网址),
         * }
         * */
        return null;
    }

    int getReleaseNum() {
        return 0;
    }

    static String getHttpResponse(String api_url) {
        String responseString = "";
        Response response = null;
        OkHttpClient client = new OkHttpClient();
        Request.Builder builder = new Request.Builder();
        builder.url(api_url);
        Request request = builder.build();
        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (response != null) {
            try {
                responseString = response.body() != null ? response.body().string() : "";
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return responseString;
    }
}


class GithubApi extends HttpApi {
    private JSONArray returnJsonArray;
    private String api_url;
    private static final String TAG = "GithubApi";

    GithubApi(String api_url) {
        Log.d(TAG, "api_url: " + api_url);
        this.api_url = api_url;
        try {
            flashData();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void flashData() throws JSONException {
        if (api_url.length() != 0) {
            String jsonText = getHttpResponse(api_url);
            if (jsonText.length() != 0) {
                this.returnJsonArray = new JSONArray(jsonText);
                Log.d(TAG, "getRelease:  returnJsonArray: " + returnJsonArray);
            }
        }
    }

    private JSONObject getRelease(int releaseNum) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject = new JSONObject(this.returnJsonArray.getString(releaseNum));
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    @Override
    int getReleaseNum() {
        return this.returnJsonArray.length();
    }


    @Override
    String getVersion(int releaseNum) {
        String latestVersion = null;
        try {
            latestVersion = getRelease(releaseNum).getString("name");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return latestVersion;
    }

    @Override
    JSONObject getReleaseDownloadUrl(int releaseNum) {
        JSONObject releaseDownloadUrl = new JSONObject();
        JSONArray releaseAssets = new JSONArray();
        try {
            releaseAssets = getRelease(releaseNum).getJSONArray("assets");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < releaseAssets.length(); i++) {
            JSONObject tmpJsonObject = new JSONObject();
            try {
                tmpJsonObject = releaseAssets.getJSONObject(i);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            // 获取一项的 JsonObject
            try {
                releaseDownloadUrl.put(tmpJsonObject.getString("name"), tmpJsonObject.getString("browser_download_url"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return releaseDownloadUrl;
    }

    static String[] getApiUrl(String url) {
        // 做切割字符串的独立方法
        String[] temp = url.split("github\\.com");
        temp = temp[temp.length - 1].split("/");
        List<String> list = new ArrayList<>(Arrays.asList(temp));
        list.removeAll(Arrays.asList("", null));
        String owner = list.get(0);
        String repo = list.get(1);
        // 分割网址
        String apiUrl = "https://api.github.com/repos/"
                + owner + "/" + repo + "/releases";
        return new String[]{apiUrl, repo};
    }
}
