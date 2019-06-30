package net.xzos.UpgradeAll.server.updater.api;

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

public class GithubApi extends Api {
    private static final String TAG = "GithubApi";
    private String APITAG;

    private String apiUrl;
    private String URL;
    private JSONArray returnJsonArray = new JSONArray();

    public GithubApi(String URL) {
        this.APITAG = URL;
        this.URL = URL;
        this.apiUrl = getApiUrl(URL);
        Log.v(APITAG, TAG, "api_url: " + apiUrl);
    }

    @Override
    public void flashData() {
        // 仅刷新数据，并进行数据校验
        if (apiUrl != null) {
            String jsonText = getHttpResponse(APITAG, apiUrl);
            // 如果刷新失败，则不记录数据
            if (jsonText.length() != 0) {
                try {
                    this.returnJsonArray = new JSONArray(jsonText);
                } catch (JSONException e) {
                    Log.e(APITAG, TAG, "flashData: ERROR_MESSAGE: " + e.toString());
                }
            }
        }
        Log.d(APITAG, TAG, "getRelease:  returnJsonArray: " + returnJsonArray);
    }

    @Override
    public int getReleaseNum() {
        return this.returnJsonArray.length();
    }

    private JSONObject getRelease(int releaseNum) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject = new JSONObject(this.returnJsonArray.getString(releaseNum));
        } catch (JSONException e) {
            Log.e(APITAG, TAG, String.format("getRelease:  返回数据解析错误: %s, returnJsonArray: %s", releaseNum, this.returnJsonArray));
        }
        return jsonObject;
    }

    @Override
    public String getVersionNumber(int releaseNum) {
        if (!isSuccessFlash()) return null;
        String versionNumber = null;
        try {
            versionNumber = getRelease(releaseNum).getString("name");
            if (versionNumber.equals("null"))
                versionNumber = getRelease(releaseNum).getString("tag_name");
            if (versionNumber.equals("null")) versionNumber = null;
        } catch (JSONException e) {
            Log.e(APITAG, TAG, String.format("getVersionNumber:  返回数据解析错误: %s, returnJsonArray: %s", "name", getRelease(releaseNum)));
        }
        Log.d(APITAG, TAG, "getVersionNumber: version: " + versionNumber);
        return versionNumber;
    }

    @Override
    public JSONObject getReleaseDownload(int releaseNum) {
        JSONObject releaseDownloadUrlJsonObject = new JSONObject();
        if (!isSuccessFlash()) return releaseDownloadUrlJsonObject;
        JSONArray releaseAssets = new JSONArray();
        try {
            releaseAssets = getRelease(releaseNum).getJSONArray("assets");
        } catch (JSONException e) {
            Log.e(APITAG, TAG, String.format(" getReleaseDownload:  返回数据解析错误: %s, returnJsonArray: %s", "assets", getRelease(releaseNum)));
        }
        for (int i = 0; i < releaseAssets.length(); i++) {
            JSONObject tmpJsonObject = new JSONObject();
            try {
                tmpJsonObject = releaseAssets.getJSONObject(i);
            } catch (JSONException e) {
                Log.e(APITAG, TAG, "getReleaseDownload: ERROR_MESSAGE: " + e.toString());
            }
            // 获取一项的 JsonObject
            try {
                releaseDownloadUrlJsonObject.put(tmpJsonObject.getString("name"), tmpJsonObject.getString("browser_download_url"));
            } catch (JSONException e) {
                Log.e(APITAG, TAG, "getReleaseDownload: ERROR_MESSAGE: " + e.toString());
            }
        }
        return releaseDownloadUrlJsonObject;
    }

    @Override
    public String getDefaultName() {
        // 获取默认名称的独立方法
        String[] apiUrlStringList = GithubApi.splitUrl(URL);
        if (apiUrlStringList == null) return null;  // 网址不符合规则返回 false
        return apiUrlStringList[1];
    }

    private static String getApiUrl(String url) {
        //获取api地址的独立方法
        String[] apiUrlStringList = GithubApi.splitUrl(url);
        if (apiUrlStringList == null) return null;  // 网址不符合规则返回 false
        else return apiUrlStringList[0];
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

    public static String getHttpResponse(String APITAG, String api_url) {
        String responseString = "";
        String TAG = " getHttpResponse";
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
