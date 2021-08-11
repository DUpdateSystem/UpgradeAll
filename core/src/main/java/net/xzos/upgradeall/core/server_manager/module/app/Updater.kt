package net.xzos.upgradeall.core.server_manager.module.app

import android.os.Looper
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.core.data.json.gson.Assets
import net.xzos.upgradeall.core.data.json.gson.ReleaseGson
import net.xzos.upgradeall.core.data.json.gson.DownloadInfoItem
import net.xzos.upgradeall.core.data_manager.utils.*
import net.xzos.upgradeall.core.network.DataCache
import net.xzos.upgradeall.core.network.ServerApi
import net.xzos.upgradeall.core.network.getNoNullMap
import net.xzos.upgradeall.core.system_api.api.IoApi
import net.xzos.upgradeall.core.utils.wait

class Updater internal constructor(private val app: App) {

    private val dataRefreshMutex = Mutex()

    suspend fun getUpdateStatus(): Int {
        val release = getReleaseList()
        return when {
            release == null -> NETWORK_ERROR
            release.isEmpty() -> INVALID_APP
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
        ) ?: run {
            val versionNumberList = getReleaseList()?.map { it.versionNumber } ?: return false
            val searchUtils = SearchUtils(versionNumberList.map {
                SearchInfo(MatchInfo(it, it, listOf(MatchString(it))), null)
            })
            val searchList = searchUtils.search(this)
            if (searchList.isNotEmpty()) {
                val version = searchList[0].matchInfo.matchList[0].matchString
                versionNumberList.indexOf(version) == 0
            } else false
        }
    }

    suspend fun getReleaseList(): List<ReleaseGson>? {
        val appId = app.appId ?: return null
        val hubUuid = app.hubDatabase?.hubConfig?.uuid ?: return null
        val auth = app.appDatabase.auth
        return if (Looper.myLooper() == Looper.getMainLooper()) {
            DataCache.getAppRelease(hubUuid, auth, appId)
        } else {
            if (dataRefreshMutex.isLocked) {
                dataRefreshMutex.wait()
                DataCache.getAppRelease(hubUuid, auth, appId)
            } else {
                val mutex = Mutex(true)
                val list = mutableListOf<ReleaseGson>()
                dataRefreshMutex.withLock {
                    ServerApi.getAppReleaseList(hubUuid, getNoNullMap(auth), getNoNullMap(appId)) {
                        it?.run { list.addAll(this) }
                        mutex.unlock()
                    }
                }
                mutex.wait()
                return list
            }
        }
    }

    // 获取最新版本号
    suspend fun getLatestVersioning(): String? {
        val appId = app.appId
        val releaseList = getReleaseList() ?: return null
        return if (appId != null) {
            if (releaseList.isNotEmpty())
                releaseList[0].versionNumber
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
                ServerApi.getDownloadInfo(hubUuid, getNoNullMap(appId), getNoNullMap(app.appDatabase.auth), fileIndex)
            else null
            var list = downloadResponse?.map { downloadPackage ->
                val fileName = if (downloadPackage.name.isNotBlank())
                    downloadPackage.name
                else {
                    asset.fileName
                }
                DownloadInfoItem(fileName, downloadPackage.url,
                    downloadPackage.getHeaders(), downloadPackage.getCookies()
                )
            }
            if (list.isNullOrEmpty())
                list = listOf(DownloadInfoItem(asset.fileName, asset.downloadUrl, mapOf(), mapOf()))
            val taskName = app.appDatabase.name
            IoApi.downloadFile(taskName, list, externalDownloader)
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

private fun List<ReleaseGson?>.getAssetsByFileIndex(fileIndex: Pair<Int, Int>): Assets? {
    return try {
        this[fileIndex.first]?.assetList?.get(fileIndex.second)
    } catch (ignore: IndexOutOfBoundsException) {
        null
    }
}
