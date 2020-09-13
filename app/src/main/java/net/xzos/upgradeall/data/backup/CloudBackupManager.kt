package net.xzos.upgradeall.data.backup

import android.widget.Toast
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import com.thegrizzlylabs.sardineandroid.impl.SardineException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.data.json.nongson.ObjectTag
import net.xzos.upgradeall.core.log.Log
import net.xzos.upgradeall.data.PreferencesMap
import net.xzos.upgradeall.utils.MiscellaneousUtils


class CloudBackupManager {
    private val sardine = OkHttpSardine().apply {
        setCredentials(PreferencesMap.webdav_username, PreferencesMap.webdav_password)
    }

    private val webdavUrl: String? = formatUrl(PreferencesMap.webdav_url)
    private val webFileParentPath = "$webdavUrl${formatUrl(PreferencesMap.webdav_path)}"

    fun getBackupFileList(): List<String>? {
        @Suppress("UNCHECKED_CAST")
        return runWebDAVFun(fun(): List<String> {
            val list = sardine.list(webFileParentPath)
            return list.subList(1, list.size).map { it.name }
        })
    }

    fun backup() {
        GlobalScope.launch {
            privateBackup()
        }
    }

    suspend fun restoreBackup(fileName: String) {
        val webFilePath = "$webFileParentPath/$fileName"
        runWebDAVFun(fun() {
            MiscellaneousUtils.showToast(R.string.restore_running)
            val bytes = sardine.get(webFilePath).readBytes()
            runBlocking { RestoreManager.parseZip(bytes) }
            MiscellaneousUtils.showToast(R.string.restore_stop)
        })
    }

    private fun privateBackup() {
        if (webdavUrl == null) {
            MiscellaneousUtils.showToast(R.string.plz_set_webdav)
            return
        }
        val fileName = BackupManager.newFileName()
        val webFilePath = "$webFileParentPath/$fileName"
        MiscellaneousUtils.showToast(R.string.backup_running)
        val bytes = BackupManager.mkZipFileBytes()
        runWebDAVFun(fun() {
            if (!sardine.exists(webFileParentPath))
                sardine.createDirectory(webFileParentPath)
            sardine.put(webFilePath, bytes)
        })
        MiscellaneousUtils.showToast(R.string.backup_stop)
    }

    private fun <E> runWebDAVFun(function: () -> E): E? {
        return try {
            function()
        } catch (e: SardineException) {
            if (e.statusCode == 409)
                MiscellaneousUtils.showToast(R.string.webdav_path_desc, Toast.LENGTH_LONG)
            Log.e(objectTag, TAG, e.toString())
            null
        } catch (e: Throwable) {
            MiscellaneousUtils.showToast(e.toString(), Toast.LENGTH_LONG)
            Log.e(objectTag, TAG, e.toString())
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