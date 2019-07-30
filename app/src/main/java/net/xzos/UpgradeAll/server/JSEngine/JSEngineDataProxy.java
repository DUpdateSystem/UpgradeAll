package net.xzos.UpgradeAll.server.JSEngine;

import net.xzos.UpgradeAll.server.JSEngine.api.Api;

import org.json.JSONObject;

import java.util.ArrayList;

public class JSEngineDataProxy extends Api {

    private static final String TAG = "JSEngineDataProxy";
    private static String[] LogObjectTag;
    private JavaScriptJEngine javaScriptJEngine;

    private int releaseNum = 0;
    private ArrayList<String> versionNumberList = new ArrayList<>();
    private ArrayList<JSONObject> releaseDownloadList = new ArrayList<>();

    public JSEngineDataProxy(JavaScriptJEngine javaScriptJEngine) {
        this.javaScriptJEngine = javaScriptJEngine;
        LogObjectTag = javaScriptJEngine.getLogObjectTag();
    }

    @Override
    public boolean refreshData() {
        int releaseNum = getReleaseNum();
        if (releaseNum != 0) {
            for (int i = 0; i < releaseNum; i++) {
                versionNumberList.add(getVersionNumber(i));
                releaseDownloadList.add(getReleaseDownload(i));
            }
            return true;
        } else
            return false;
    }

    @Override
    public int getReleaseNum() {
        if (this.releaseNum == 0) {
            try {
                this.releaseNum = javaScriptJEngine.getReleaseNum();
            } catch (Throwable e) {
                Log.e(LogObjectTag, TAG, "getReleaseNum: 脚本执行错误, ERROR_MESSAGE: " + e.toString());
            }
        }
        return this.releaseNum;
    }

    @Override
    public String getVersionNumber(int releaseNum) {
        String versionNumber = null;
        if (versionNumberList.size() == 0) {
            try {
                versionNumber = javaScriptJEngine.getVersionNumber(releaseNum);
            } catch (Throwable e) {
                Log.e(LogObjectTag, TAG, "getVersionNumber: 脚本执行错误, ERROR_MESSAGE: " + e.toString());
                return null;
            }
        } else if (releaseNum >= 0 && releaseNum < versionNumberList.size())
            versionNumber = versionNumberList.get(releaseNum);
        return versionNumber;
    }

    @Override
    public JSONObject getReleaseDownload(int releaseNum) {
        JSONObject releaseDownload = null;
        if (releaseDownloadList.size() == 0) {
            try {
                releaseDownload = javaScriptJEngine.getReleaseDownload(releaseNum);
            } catch (Throwable e) {
                Log.e(LogObjectTag, TAG, "getReleaseDownload: 脚本执行错误, ERROR_MESSAGE: " + e.toString());
                return null;
            }
        } else if (releaseNum >= 0 && releaseNum < releaseDownloadList.size())
            releaseDownload = releaseDownloadList.get(releaseNum);
        return releaseDownload;
    }

    @Override
    public String getDefaultName() {
        try {
            return javaScriptJEngine.getDefaultName();
        } catch (Throwable e) {
            Log.e(LogObjectTag, TAG, "getDefaultName: 脚本执行错误, ERROR_MESSAGE: " + e.toString());
        }
        return null;
    }
}
