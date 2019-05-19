package net.xzos.UpgradeAll.updater.HttpApi;

import org.json.JSONObject;


public class HttpApi {

    public void flashData() {
    }

    public boolean isSuccessFlash() {
        return getReleaseNum() != 0;
    }

    int getReleaseNum() {
        return 0;
    }

    public String getVersionNumber(int releaseNum) {
        /*返回云端版本号*/
        return null;
    }

    public JSONObject getReleaseDownload(int releaseNum) {
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

    public String getDefaultName() {
        return null;
    }
}
