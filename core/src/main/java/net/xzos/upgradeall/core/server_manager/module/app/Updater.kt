package net.xzos.upgradeall.core.server_manager.module.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.xzos.upgradeall.core.data_manager.utils.VersioningUtils
import net.xzos.upgradeall.core.network_api.GrpcApi
import net.xzos.upgradeall.core.route.AppStatus
import net.xzos.upgradeall.core.route.ReleaseInfoItem
import net.xzos.upgradeall.core.system_api.api.IoApi


class Updater(private val app: App) : UpdaterApi {
    private val dataMutex = Mutex()

    override suspend fun getUpdateStatus(): Int {
        val appStatus = getAppStatus() ?: return NETWORK_ERROR
        return if (!appStatus.validApp)
            INVALID_APP
        else {
            //检查是否取得云端版本号
            val versionNumber = app.markProcessedVersionNumber ?: app.installedVersionNumber
            if (versionNumber != null) {
                // 检查是否获取本地版本号
                if (isLatest(versionNumber))
                    APP_LATEST
                else APP_OUTDATED
            } else APP_NO_LOCAL
        }
    }

    private suspend fun isLatest(versionNumber: String): Boolean {
        val latestVersion = getLatestVersioning()
        return VersioningUtils.compareVersionNumber(
                versionNumber, latestVersion
        )
    }

    override suspend fun getAppStatus(): AppStatus? {
        val appId = app.appId
        if (appId != null) {
            dataMutex.withLock {
                val hubUuid = app.hubDatabase?.hubConfig?.uuid ?: return null
                return GrpcApi.getAppStatus(hubUuid, appId)
            }
        }
        return null
    }

    override suspend fun getReleaseInfo(): List<ReleaseInfoItem>? {
        return getAppStatus()?.releaseInfoList
    }

    // 获取最新版本号
    override suspend fun getLatestVersioning(): String? {
        val appInfo = app.appId
        val releaseInfoList = getReleaseInfo() ?: return null
        return if (appInfo != null) {
            if (releaseInfoList.isNotEmpty())
                releaseInfoList[0].versionNumber
            else null
        } else null
    }

    // 使用内置下载器下载文件
    suspend fun downloadReleaseFile(fileIndex: Pair<Int, Int>, externalDownloader: Boolean = false) {
        withContext(Dispatchers.Default) {
            getReleaseInfo()?.let { releaseInfoList ->
                val asset = releaseInfoList[fileIndex.first].getAssets(fileIndex.second)
                val hubUuid = app.hubDatabase?.uuid
                val appId = app.appId
                val downloadInfo = if (hubUuid != null && appId != null)
                    GrpcApi.getDownloadInfo(hubUuid, appId, fileIndex.toList())
                else null
                val url = (if (!downloadInfo?.url.isNullOrBlank())
                    downloadInfo?.url
                else asset.downloadUrl) ?: return@withContext
                val headers = mutableMapOf<String, String>().also {
                    for (dict in downloadInfo?.requestHeaderList ?: listOf())
                        it[dict.key] = dict.value
                }
                IoApi.downloadFile(
                        asset.fileName, url,
                        headers = headers,
                        externalDownloader = externalDownloader
                )
            }
        }
    }

    companion object {
        const val INVALID_APP = -1
        const val NETWORK_ERROR = 0
        const val APP_LATEST = 1
        const val APP_OUTDATED = 2
        const val APP_NO_LOCAL = 3

    }
}

private interface UpdaterApi {
    suspend fun getAppStatus(): AppStatus?
    suspend fun getReleaseInfo(): List<ReleaseInfoItem>?
    suspend fun getUpdateStatus(): Int
    suspend fun getLatestVersioning(): String?
}
