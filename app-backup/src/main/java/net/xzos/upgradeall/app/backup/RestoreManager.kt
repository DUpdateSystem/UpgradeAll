package net.xzos.upgradeall.app.backup

import kotlinx.coroutines.runBlocking
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


object RestoreManager {
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
            }
            else -> Log.e(logObjectTag, TAG, "ignore file: $name")
        }
        delBackupTmp()
    }

    private suspend fun restoreDatabase(dbFileBytes: ByteArray) {
        val metaDatabase = getBackupMetaDatabase(dbFileBytes)
        metaDatabase.hubDao().loadAll().forEach {
            HubManager.updateHub(it)
        }
        metaDatabase.appDao().loadAll().forEach {
            AppManager.updateApp(it)
        }
        metaDatabase.extraAppDao().loadAll().forEach {
            it.__mark_version_number?.apply {
                ExtraAppEntityManager.addMarkVersionNumber(it.appId, this)
            }
        }
    }
}
