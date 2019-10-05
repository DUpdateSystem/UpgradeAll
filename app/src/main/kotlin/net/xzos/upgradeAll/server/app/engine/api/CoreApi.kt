package net.xzos.upgradeAll.server.app.engine.api

import org.json.JSONObject


interface CoreApi {

    /**
     * 返回更新项默认名称
     */
    suspend fun getDefaultName(): String?

    /**
     * 返回获取到的云端版本号数量
     */
    suspend fun getReleaseNum(): Int

    /**
     * 返回指定版本的云端版本号
     */
    suspend fun getVersioning(releaseNum: Int): String?

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
    suspend fun getReleaseDownload(releaseNum: Int): JSONObject

    /**
     * 下载文件操作
     * 并 返回文件路径
     */
    fun downloadReleaseFile(fileIndex: Pair<Int, Int>): String?
}
