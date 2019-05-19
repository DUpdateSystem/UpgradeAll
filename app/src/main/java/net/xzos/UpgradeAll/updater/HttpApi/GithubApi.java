package net.xzos.UpgradeAll.updater.HttpApi;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class GithubApi extends HttpApi {
    private static final String TAG = "GithubApi";

    private String apiUrl;
    private String url;
    private JSONArray returnJsonArray = new JSONArray();

    public GithubApi(String url) {
        this.url = url;
        this.apiUrl = getApiUrl(url);
        Log.d(TAG, "api_url: " + apiUrl);
    }

    @Override
    public void flashData() {
        // 仅刷新数据，并进行数据校验
        if (apiUrl.length() != 0) {
            String jsonText = getHttpResponse(apiUrl);
            // 如果刷新失败，则不记录数据
            if (jsonText.length() != 0) {
                try {
                    this.returnJsonArray = new JSONArray(jsonText);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        Log.d(TAG, "getRelease:  returnJsonArray: " + returnJsonArray);
    }

    private JSONObject getRelease(int releaseNum) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject = new JSONObject(this.returnJsonArray.getString(releaseNum));
        } catch (JSONException e) {
            Log.e(TAG, String.format("getRelease:  返回数据解析错误: %s, returnJsonArray: %s", releaseNum, this.returnJsonArray));
        }
        return jsonObject;
    }

    @Override
    int getReleaseNum() {
        return this.returnJsonArray.length();
    }


    @Override
    public String getVersionNumber(int releaseNum) {
        String latestVersion = null;
        try {
            latestVersion = getRelease(releaseNum).getString("name");
        } catch (JSONException e) {
            Log.e(TAG, String.format("getVersionNumber:  返回数据解析错误: %s, returnJsonArray: %s", "name", getRelease(releaseNum)));
        }
        return latestVersion;
    }

    @Override
    public JSONObject getReleaseDownload(int releaseNum) {
        JSONObject releaseDownloadUrl = new JSONObject();
        JSONArray releaseAssets = new JSONArray();
        try {
            releaseAssets = getRelease(releaseNum).getJSONArray("assets");
        } catch (JSONException e) {
            Log.e(TAG, String.format(" getReleaseDownload:  返回数据解析错误: %s, returnJsonArray: %s", "assets", getRelease(releaseNum)));
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

    public String getDefaultName() {
        // 获取默认名称的独立方法
        String[] apiUrlStringList = GithubApi.splitUrl(url);
        if (apiUrlStringList == null) return null;  // 网址不符合规则返回 false
        return apiUrlStringList[1];
    }

    private static String getApiUrl(String url) {
        //获取api地址的独立方法
        return Objects.requireNonNull(GithubApi.splitUrl(url))[0];
    }

    private static String[] splitUrl(String url) {
        String[] temp = url.split("github\\.com");
        temp = temp[temp.length - 1].split("/");
        List<String> list = new ArrayList<>(Arrays.asList(temp));
        list.removeAll(Arrays.asList("", null));
        if (list.size() == 2) {
            String owner = list.get(0);
            String repo = list.get(1);
            // 分割网址
            String apiUrl = "https://api.github.com/repos/"
                    + owner + "/" + repo + "/releases";
            return new String[]{apiUrl, repo};
        } else return null;
    }

    private static String getHttpResponse(String api_url) {
        String responseString = "";
        Response response = null;
        OkHttpClient client = new OkHttpClient();
        Request.Builder builder = new Request.Builder();
        builder.url(api_url);
        Request request = builder.build();
        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            Log.e(TAG, "getHttpResponse:  网络错误");
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
