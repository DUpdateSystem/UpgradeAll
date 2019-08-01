package net.xzos.UpgradeAll.server.JSEngine.JSUtils;

import net.xzos.UpgradeAll.application.MyApplication;
import net.xzos.UpgradeAll.server.log.LogUtil;

import java.io.IOException;
import java.util.Objects;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class OkHttpApi {
    private static final LogUtil Log = MyApplication.getServerContainer().getLog();
    private static final String TAG = "OkHttpApi";

    public static String getHttpResponse(String[] LogObjectTag, String api_url) {
        String responseString = null;
        Response response = null;
        OkHttpClient client = new OkHttpClient();
        Request.Builder builder = new Request.Builder();
        builder.url(api_url);
        Request request = builder.build();
        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            Log.e(LogObjectTag, TAG, "getHttpResponse:  网络错误");
        }
        if (response != null) {
            try {
                responseString = Objects.requireNonNull(response.body()).string();
            } catch (IOException e) {
                Log.e(LogObjectTag, TAG, "getHttpResponse: ERROR_MESSAGE: " + e.toString());
            }
        }
        return responseString;
    }
}
