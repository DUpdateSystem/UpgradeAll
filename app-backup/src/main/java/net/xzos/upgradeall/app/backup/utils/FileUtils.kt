package net.xzos.upgradeall.app.backup.utils

import net.xzos.upgradeall.core.database.MetaDatabase
import net.xzos.upgradeall.core.database.getDatabase
import java.io.File

private val rootDataDir = androidContext.filesDir.parentFile
val tmpDBFile = File(rootDataDir, "databases/app_metadata_database.db.bak")

val dbFile = File(rootDataDir, "databases/app_metadata_database.db")
val prefsFile = File(rootDataDir, "shared_prefs/${androidContext.packageName}_preferences.xml")

fun getBackupMetaDatabase(dbFileBytes: ByteArray): MetaDatabase {
    tmpDBFile.writeBytes(dbFileBytes)
    return getDatabase(androidContext, MetaDatabase::class.java, tmpDBFile.name)
}

private fun deleteTmpDB() {
    tmpDBFile.delete()
}

fun delBackupTmp() {
    deleteTmpDB()
}