package net.xzos.upgradeall.core.server_manager.module.app

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.core.data_manager.utils.VersioningUtils
import net.xzos.upgradeall.core.network_api.GrpcApi
import net.xzos.upgradeall.core.network_api.toMap
import net.xzos.upgradeall.core.route.AssetItem
import net.xzos.upgradeall.core.route.ReleaseListItem
import net.xzos.upgradeall.core.system_api.api.IoApi


class Updater(private val app: App) {
    private val dataMutex = Mutex()

    suspend fun getUpdateStatus(): Int {
        val release = getReleaseList() ?: return NETWORK_ERROR
        return when {
            release.isEmpty() -> INVALID_APP
            release[0] == null -> NETWORK_ERROR
            else -> {
                val versionNumber = app.ignoreVersionNumber ?: app.installedVersionNumber
                when {
                    versionNumber == null -> APP_NO_LOCAL
                    !versionNumber.isLatest() -> APP_OUTDATED
                    else -> APP_LATEST
                }
            }
        }.also {
            app.statusRenewedFun(it)
        }
    }

    private suspend fun String.isLatest(): Boolean {
        val latestVersion = getLatestVersioning()
        return VersioningUtils.compareVersionNumber(
                this, latestVersion
        )
    }

    suspend fun getReleaseList(): List<ReleaseListItem?>? {
        val appId = app.appId
        if (appId != null) {
            dataMutex.withLock {
                val hubUuid = app.hubDatabase?.hubConfig?.uuid ?: return null
                return GrpcApi.getAppRelease(hubUuid, app.appDatabase.auth, appId)
            }
        }
        return null
    }

    // 获取最新版本号
    suspend fun getLatestVersioning(): String? {
        val appId = app.appId
        val releaseList = getReleaseList() ?: return null
        return if (appId != null) {
            if (releaseList.isNotEmpty())
                releaseList[0]?.versionNumber
            else null
        } else null
    }

    // 使用内置下载器下载文件
    suspend fun downloadReleaseFile(fileIndex: Pair<Int, Int>, externalDownloader: Boolean = false) {
        getReleaseList()?.let { releaseList ->
            val asset = releaseList.getAssetsByFileIndex(fileIndex) ?: return
            val hubUuid = app.hubDatabase?.uuid
            val appId = app.appId
            val downloadResponse = if (hubUuid != null && appId != null)
                GrpcApi.getDownloadInfo(hubUuid, appId, app.appDatabase.auth, fileIndex.toList())
            else null
            val list = downloadResponse?.listList
            if (!list.isNullOrEmpty())
                for (download in list) {
                    val headers = download?.requestHeaderList?.toMap() ?: mapOf()
                    IoApi.downloadFile(
                            asset.fileName, download?.url ?: return,
                            headers = headers,
                            externalDownloader = externalDownloader
                    )
                }
            else
                IoApi.downloadFile(
                        asset.fileName, asset.downloadUrl,
                        externalDownloader = externalDownloader
                )
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

private fun List<ReleaseListItem?>.getAssetsByFileIndex(fileIndex: Pair<Int, Int>): AssetItem? {
    return try {
        this[fileIndex.first]?.getAssets(fileIndex.second)
    } catch (ignore: IndexOutOfBoundsException) {
        null
    }
}
