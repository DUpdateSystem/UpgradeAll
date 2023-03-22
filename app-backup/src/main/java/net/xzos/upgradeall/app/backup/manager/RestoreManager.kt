package net.xzos.upgradeall.app.backup.manager

import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.app.backup.manager.status.Progress
import net.xzos.upgradeall.app.backup.manager.status.RestoreStage
import net.xzos.upgradeall.app.backup.manager.status.RestoreStatus
import net.xzos.upgradeall.app.backup.utils.dbFile
import net.xzos.upgradeall.app.backup.utils.delBackupTmp
import net.xzos.upgradeall.app.backup.utils.getBackupMetaDatabase
import net.xzos.upgradeall.app.backup.utils.prefsFile
import net.xzos.upgradeall.core.database.table.extra_app.ExtraAppEntityManager
import net.xzos.upgradeall.core.manager.AppManager
import net.xzos.upgradeall.core.manager.HubManager
import net.xzos.upgradeall.core.utils.file.parseZipBytes
import net.xzos.upgradeall.core.utils.log.Log
import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.core.utils.oberver.InformerNoTag


object RestoreManager : InformerNoTag<RestoreStatus>() {
    private const val TAG = "RestoreManager"
    private val logObjectTag = ObjectTag("app-backup", TAG)

    suspend fun parseZip(zipFileByteArray: ByteArray) {
        parseZipBytes(zipFileByteArray) { name, bytes ->
            runBlocking { parseData(name, bytes) }
            false
        }
    }

    private suspend fun parseData(name: String, bytes: ByteArray) {
        when (name) {
            "/${dbFile.name}" -> restoreDatabase(bytes)
            "/${prefsFile.name}" -> {
                val text = bytes.toString(Charsets.UTF_8)
                prefsFile.writeText(text)
                notifyChanged(
                    RestoreStatus(
                        RestoreStage.RESTORE_PREFS,
                        Progress(1, 1),
                        "current(prefs): ${prefsFile.path}"
                    )
                )
            }
            else -> {
                Log.e(logObjectTag, TAG, "ignore file: $name")
                notifyChanged(
                    RestoreStatus(
                        RestoreStage.RESTORE_INVALID,
                        Progress(1, 1),
                        "current(invalid): $name"
                    )
                )
            }
        }
        delBackupTmp()
        notifyChanged(
            RestoreStatus(RestoreStage.FINISH, Progress(0, 0), "finish")
        )
    }

    private suspend fun restoreDatabase(dbFileBytes: ByteArray) {
        val metaDatabase = getBackupMetaDatabase(dbFileBytes)
        with(metaDatabase.hubDao().loadAll()) {
            forEachIndexed { index, hubEntity ->
                HubManager.updateHub(hubEntity)
                notifyChanged(
                    RestoreStatus(
                        RestoreStage.RESTORE_DATABASE,
                        Progress(size, index + 1),
                        "current(hub): ${hubEntity.uuid}"
                    )
                )
            }
        }
        with(metaDatabase.appDao().loadAll()) {
            forEachIndexed { index, appEntity ->
                AppManager.saveApp(appEntity.copy(id = 0L))
                notifyChanged(
                    RestoreStatus(
                        RestoreStage.RESTORE_DATABASE,
                        Progress(size, index + 1),
                        "current(app): ${appEntity.name}"
                    )
                )
            }
        }
        with(metaDatabase.extraAppDao().loadAll()) {
            forEachIndexed { index, extraAppEntity ->
                extraAppEntity.__mark_version_number?.apply {
                    ExtraAppEntityManager.addMarkVersionNumber(extraAppEntity.appId, this)
                }
                notifyChanged(
                    RestoreStatus(
                        RestoreStage.RESTORE_DATABASE,
                        Progress(size, index + 1),
                        "current(extra): ${extraAppEntity.appId}"
                    )
                )
            }
        }
    }
}