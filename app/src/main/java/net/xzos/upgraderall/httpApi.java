package net.xzos.upgraderall;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


class httpApi {
    githubApi GithubApi(String api_url) {
        return new githubApi(api_url);
    }
}

class getHttpResponse implements Runnable {
    private Thread t;
    private String api_url;
    private String responseString;

    getHttpResponse(String api_url) {
        this.api_url = api_url;
    }

    @Override
    public void run() {
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(api_url)
                    .build();
            Response response = client.newCall(request).execute();
            responseString = response.body() != null ? response.body().string() : null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void start() {
        if (t == null) {
            t = new Thread(this);
            t.start();
        }
    }

    String getResponseString() {
        return this.responseString;
    }
}


class githubApi {
    private static final String TAG = "githubApi";
    private JSONArray returnJsonArray;
    private String api_url = null;

    githubApi(String api_url) {
        setApi_url(api_url);
        try {
            flashData();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void flashData() throws JSONException {
        if (api_url.length() != 0) {
            getHttpResponse getHttp = new getHttpResponse(api_url);
            getHttp.start();
            String jsonText = getHttp.getResponseString();
            Log.d(TAG, "jsonText: " + jsonText);
            this.returnJsonArray = new JSONArray(jsonText);
        }
    }

    JSONObject getLatestRelease() {
        try {
            return getRelease(0);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private JSONObject getRelease(int releaseNum) throws JSONException {
        return new JSONObject(this.returnJsonArray.getString(releaseNum));
    }

    private void setApi_url(String api_url) {
        if (this.api_url == null) {
            this.api_url = api_url;
        }
    }

}
