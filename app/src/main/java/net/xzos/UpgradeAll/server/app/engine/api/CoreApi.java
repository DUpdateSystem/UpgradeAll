package net.xzos.UpgradeAll.server.app.engine.api;

import org.json.JSONObject;


public interface CoreApi {

    /**
     * 返回更新项默认名称
     */
    String getDefaultName();

    /**
     * 返回获取到的云端版本号数量
     */
    int getReleaseNum();

    /**
     * 返回指定版本的云端版本号
     */
    String getVersioning(int releaseNum);

    /**
     * 返回指定版本的更新日志
     */
    String getChangelog(int releaseNum);

    /**
     * 返回指定版本所包含的文件的下载链接
     * <p>
     * 预期的返回值:
     * {
     * 下载文件名: 下载地址(最好为直链，否则提供直接导向网址),
     * }
     * </p>
     */
    JSONObject getReleaseDownload(int releaseNum);
}
