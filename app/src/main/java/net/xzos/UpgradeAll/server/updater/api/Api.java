package net.xzos.UpgradeAll.server.updater.api;

import net.xzos.UpgradeAll.application.MyApplication;
import net.xzos.UpgradeAll.utils.LogUtil;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class Api {
    protected static final LogUtil Log = MyApplication.getLog();

    public boolean initData() {
        return false;
    }

    public String getDefaultName() {
        return null;
    }

    public int getReleaseNum() {
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

    public boolean isSuccessFlash() {
        return getReleaseNum() != 0;
    }
}
