package net.xzos.upgradeall.core.data.backup

import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.log.Log
import net.xzos.upgradeall.core.log.ObjectTag
import net.xzos.upgradeall.core.log.msg
import net.xzos.upgradeall.core.webDavConfig


class CloudBackupManager {
    private val sardine = OkHttpSardine().apply {
        setCredentials(webDavConfig.username, webDavConfig.password)
    }

    private val webdavUrl: String? = formatUrl(webDavConfig.url)
    private val webFileParentPath = "$webdavUrl${formatUrl(webDavConfig.path)}"

    fun getBackupFileList(): List<String>? {
        @Suppress("UNCHECKED_CAST")
        return runWebDAVFun(fun(): List<String> {
            val list = sardine.list(webFileParentPath)
            return list.subList(1, list.size).map { it.name }
        }, fun(_) {})
    }

    fun backup(startFunc: () -> Unit, stopFunc: () -> Unit, errorFunc: (Throwable) -> Unit) {
        GlobalScope.launch {
            doBackup(startFunc, stopFunc, errorFunc)
        }
    }

    suspend fun restoreBackup(fileName: String, startFunc: () -> Unit, stopFunc: () -> Unit, errorFunc: (Throwable) -> Unit) {
        val webFilePath = "$webFileParentPath/$fileName"
        runWebDAVFun(fun() {
            startFunc()
            val bytes = sardine.get(webFilePath).readBytes()
            runBlocking { RestoreManager.parseZip(bytes) }
            stopFunc()
        }, fun(e) { errorFunc(e) })
    }

    private suspend fun doBackup(startFunc: () -> Unit, stopFunc: () -> Unit, errorFunc: (Throwable) -> Unit) {
        // TODO: 试试空网址导致的出错
        val fileName = BackupManager.newFileName()
        val webFilePath = "$webFileParentPath/$fileName"
        startFunc()
        val bytes = BackupManager.mkZipFileBytes()
        runWebDAVFun(fun() {
            if (!sardine.exists(webFileParentPath))
                sardine.createDirectory(webFileParentPath)
            sardine.put(webFilePath, bytes)
        }, fun(e) { errorFunc(e) })
        stopFunc()
    }

    private fun <E> runWebDAVFun(function: () -> E, errorFun: (e: Throwable) -> Unit): E? {
        return try {
            function()
        } catch (e: Throwable) {
            errorFun(e)
            Log.e(objectTag, TAG, e.msg())
            null
        }
    }

    private fun formatUrl(url: String?): String? {
        return if (url?.last() == '/') {
            url.substring(0, url.lastIndex)
        } else url
    }

    companion object {
        private const val TAG = "CloudBackupManager"
        private val objectTag = ObjectTag("Backup", TAG)
    }
}