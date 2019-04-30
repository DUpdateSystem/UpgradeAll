package net.xzos.UpgradeAll;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

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
                responseString = response.body() != null ? response.body().string() : null;
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
        Log.d(TAG, "api_url" + api_url);
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
        return new JSONObject(this.returnJsonArray.getString(releaseNum));
    }

    private void setApiUrl(String api_url) {
        if (this.api_url == null) {
            this.api_url = api_url;
        }
    }
}
