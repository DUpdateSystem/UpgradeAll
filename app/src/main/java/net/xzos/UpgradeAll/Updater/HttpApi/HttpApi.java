package net.xzos.UpgradeAll.Updater.HttpApi;

import android.util.Log;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class HttpApi {

    private static final String TAG = "HttpApi";

    public void flashData() {
    }

    public boolean isSuccessFlash() {
        return false;
    }

    JSONObject getLatestRelease() {
        return null;
    }

    public String getVersion(int releaseNum) {
        /*返回云端版本号*/
        return null;
    }

    public JSONObject getReleaseDownloadUrl(int releaseNum) {
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


