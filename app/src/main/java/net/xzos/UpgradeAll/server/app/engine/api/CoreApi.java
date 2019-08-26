package net.xzos.UpgradeAll.server.app.engine.api;

import org.json.JSONObject;


public interface CoreApi {

    String getDefaultName();

    int getReleaseNum();

    String getVersionNumber(int releaseNum);
    /*返回云端版本号*/

    JSONObject getReleaseDownload(int releaseNum);
    /*
     * 获取特定版本的下载链接
     *
     * 预期的返回值:
     * {
     *       下载文件名: 下载地址(最好为直链，否则提供直接导向网址),
     * }
     * */
}
