package net.xzos.UpgradeAll;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


class HttpApi {
    githubApi GithubApi(String api_url) {
        return new githubApi(api_url);
    }

    static String getHttpResponse(String api_url) {
        String responseString = null;
        try {
            OkHttpClient client = new OkHttpClient();
            Request.Builder builder = new Request.Builder();
            builder.url(api_url);
            Request request = builder.build();
            Response response = client.newCall(request).execute();
            responseString = response.body() != null ? response.body().string() : null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return responseString;
    }
}


class githubApi {
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
            String jsonText = HttpApi.getHttpResponse(api_url);
            if (jsonText.length() != 0) {
                this.returnJsonArray = new JSONArray(jsonText);
            }
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
