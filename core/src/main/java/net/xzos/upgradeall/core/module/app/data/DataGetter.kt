package net.xzos.upgradeall.core.module.app.data

import kotlinx.coroutines.sync.Mutex
import net.xzos.upgradeall.core.manager.AppManager
import net.xzos.upgradeall.core.module.Hub
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.module.app.version.AssetWrapper
import net.xzos.upgradeall.core.module.app.version.Version
import net.xzos.upgradeall.core.module.app.version.VersionWrapper
import net.xzos.upgradeall.core.utils.coroutines.ValueMutex
import net.xzos.upgradeall.core.utils.coroutines.runWithLock

internal object DataGetter {

    private val lockedHubMutex = Mutex()
    private val lockedAppMutex = Mutex()

    private val lockedHub = mutableMapOf<Hub, ValueMutex>()
    private val lockedApp = mutableMapOf<String, ValueMutex>()

    fun getLatestVersion(app: App): Version? {
        app.hubEnableList.forEach {
            val mutex = lockedHubMutex.runWithLock {
                lockedHub.getOrPut(it) { ValueMutex() }
            }
            val appList = mutex.runWithLock {
                hubGetUpdate(it)
            }
            lockedHubMutex.runWithLock { lockedHub.remove(it) }
            if (!appList.contains(app)) getVersionList(app, it)
        }
        return app.versionMap.getVersionList().firstOrNull()
    }

    private fun hubGetUpdate(hub: Hub): Set<App> {
        val appList = AppManager.getAppList(hub)
        val appLatestReleaseMap =
            hub.getAppLatestRelease(*appList.toTypedArray()) ?: return emptySet()
        appLatestReleaseMap.forEach {
            val (app, releaseGson) = it
            app.versionMap.addSingleRelease(
                VersionWrapper(
                    hub, releaseGson,
                    releaseGson.assetGsonList.mapIndexed { assetIndex, assetGson ->
                        AssetWrapper(hub, listOf(0, assetIndex), assetGson)
                    })
            )
        }
        return appLatestReleaseMap.keys
    }

    fun getVersionList(app: App): List<Version> {
        app.hubEnableList.forEach {
            lockedHubMutex.runWithLock { lockedHub.remove(it) }
            getVersionList(app, it)
        }
        return app.versionMap.getVersionList()
    }

    private fun getVersionList(app: App, hub: Hub): Boolean {
        val mutex = lockedAppMutex.runWithLock {
            lockedApp.getOrPut("$app$hub") { ValueMutex() }
        }
        return mutex.runWithLock {
            getVersionList0(app, hub).also {
                lockedAppMutex.runWithLock {
                    lockedApp.remove("$app$hub")
                }
            }
        }
    }

    private fun getVersionList0(app: App, hub: Hub): Boolean {
        return hub.getAppReleaseList(app)?.mapIndexed { index, releaseGson ->
            VersionWrapper(
                hub, releaseGson,
                releaseGson.assetGsonList.mapIndexed { assetIndex, assetGson ->
                    AssetWrapper(hub, listOf(index, assetIndex), assetGson)
                })
        }.also {
            if (it != null)
                app.versionMap.addReleaseList(it)
            else app.versionMap.setError()
        } != null
    }
}