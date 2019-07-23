package net.xzos.UpgradeAll.server.JSEngine;

import net.xzos.UpgradeAll.server.updater.api.Api;

import org.json.JSONObject;

import java.util.ArrayList;

public class JSEngineDataProxy extends Api {
    private JavaScriptJEngine javaScriptJEngine;

    private int releaseNum = 0;
    private ArrayList<String> versionNumberList = new ArrayList<>();
    private ArrayList<JSONObject> releaseDownloadList = new ArrayList<>();

    public JSEngineDataProxy(JavaScriptJEngine javaScriptJEngine) {
        this.javaScriptJEngine = javaScriptJEngine;
    }

    @Override
    public boolean initData() {
        javaScriptJEngine.initData();
        getReleaseNum();
        if (getReleaseNum() != 0) {
            getVersionNumber(0);
            getReleaseDownload(0);
            return true;
        } else
            return false;
    }

    @Override
    public int getReleaseNum() {
        if (this.releaseNum == 0) {
            this.releaseNum = javaScriptJEngine.getReleaseNum();
        }
        return this.releaseNum;
    }

    @Override
    public String getVersionNumber(int releaseNum) {
        if (versionNumberList.size() == 0) {
            for (int i = 0; i < getReleaseNum(); i++) {
                versionNumberList.add(javaScriptJEngine.getVersionNumber(i));
            }
        }
        if (releaseNum >= 0 && releaseNum < getReleaseNum())
            return versionNumberList.get(releaseNum);
        else
            return null;
    }

    @Override
    public JSONObject getReleaseDownload(int releaseNum) {
        if (releaseDownloadList.size() == 0) {
            for (int i = 0; i < getReleaseNum(); i++) {
                releaseDownloadList.add(javaScriptJEngine.getReleaseDownload(i));
            }
        }
        if (releaseNum >= 0 && releaseNum < getReleaseNum())
            return releaseDownloadList.get(releaseNum);
        else
            return null;
    }

    @Override
    public String getDefaultName() {
        return javaScriptJEngine.getDefaultName();
    }
}
