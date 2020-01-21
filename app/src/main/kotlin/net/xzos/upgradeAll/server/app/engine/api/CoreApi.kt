package net.xzos.upgradeAll.server.app.engine.api

import net.xzos.upgradeAll.data.json.gson.JSReturnData


interface CoreApi {

    /**
     * 返回更新项默认名称
     */
    suspend fun getDefaultName(): String?

    /**
     * 返回更新项默认图标
     */
    suspend fun getAppIconUrl(): String?

    /**
     * 返回获取到的云端版本号数量
     */
    suspend fun getReleaseNum(): Int

    /**
     * 返回指定版本的云端版本号
     */
    suspend fun getVersionNumber(releaseNum: Int): String?

    /**
     * 返回指定版本的更新日志
     */
    suspend fun getChangelog(releaseNum: Int): String?

    /**
     * 返回指定版本所包含的文件的下载链接
     *
     *
     * 预期的返回值:
     * {
     * 下载文件名: 下载地址(最好为直链，否则提供直接导向网址),
     * }
     *
     */
    suspend fun getReleaseDownload(releaseNum: Int): Map<String, String>

    /**
     * 下载文件操作
     * 操作成功返回 true
     */
    suspend fun downloadReleaseFile(downloadIndex: Pair<Int, Int>): Boolean

    /**
     * 返回由 JavaScript 函数返回的固定 JSON 格式生成的版本信息数据类
     */
    suspend fun getReleaseInfo(): JSReturnData?
}