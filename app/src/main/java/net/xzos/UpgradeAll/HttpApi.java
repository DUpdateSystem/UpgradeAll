package net.xzos.UpgradeAll;

import android.annotation.SuppressLint;
import android.app.Application;
import android.util.Log;
import android.widget.Toast;

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

    String getLatestRelease() {
        return null;
    }

    static String getHttpResponse(String api_url) {
        String responseString = null;
        OkHttpClient client = new OkHttpClient();
        Request.Builder builder = new Request.Builder();
        builder.url(api_url);
        Request request = builder.build();
        Response response = null;
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
    private String api_url = null;
    private static final String TAG = "GithubApi";

    GithubApi(String api_url) {
        Log.d(TAG, "api_url: " + api_url);
        setApiUrl(api_url);
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
            }
        }
    }

    @Override
    String getLatestRelease() {
        try {
            return getRelease(0).getString("name");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private JSONObject getRelease(int releaseNum) throws JSONException {
        Log.d(TAG, "getRelease:  returnJsonArray: " + returnJsonArray);
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject = new JSONObject(this.returnJsonArray.getString(releaseNum));
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    private void setApiUrl(String api_url) {
        if (this.api_url == null) {
            this.api_url = api_url;
        }
    }

    static String[] getApiUrl(String url) {
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
