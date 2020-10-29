package net.xzos.upgradeall.utils.install

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.core.data.json.nongson.ObjectTag
import net.xzos.upgradeall.core.log.Log
import net.xzos.upgradeall.core.oberver.Informer
import net.xzos.upgradeall.server.ApkInstallerService
import net.xzos.upgradeall.utils.ToastUtil
import java.io.File
import java.io.IOException


object ApkSystemInstaller : Informer {

    internal const val TAG = "ApkSystemInstaller"
    internal val logObjectTag = ObjectTag(ObjectTag.core, TAG)

    private val context: Context = MyApplication.context

    suspend fun install(file: File) {
        try {
            val fileUri = file.getApkUri()
            rowInstall(fileUri)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            ToastUtil.makeText(e.toString())
        }
    }

    private fun rowInstall(fileUri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(fileUri, "application/vnd.android.package-archive")
                .putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                .apply {
                    this.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        this.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                }
        context.startActivity(intent)
    }

    suspend fun multipleInstall(apkFileList: List<File>) {
        var totalSize: Long = 0
        try {
            for (listOfFile in apkFileList) {
                if (listOfFile.isFile) {
                    Log.d(logObjectTag, TAG, "multipleInstall: " + listOfFile.name)
                    totalSize += listOfFile.length()
                }
            }
        } catch (e: Exception) {
            Log.e(logObjectTag, TAG, "multipleInstall: " + e.message)
            return
        }
        val packageInstaller = context.packageManager.packageInstaller
        val installParams = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        installParams.setSize(totalSize)
        try {
            @Suppress("BlockingMethodInNonBlockingContext")
            val sessionId = packageInstaller.createSession(installParams)
            Log.d(logObjectTag, TAG, "Success: created install session [$sessionId]")
            for (apkFile in apkFileList) {
                doWriteSession(packageInstaller, sessionId, apkFile)
            }
            doCommitSession(packageInstaller, sessionId)
            Log.d(logObjectTag, TAG, "Success")
        } catch (e: IOException) {
            Log.e(logObjectTag, TAG, "multipleInstall: " + e.message)
        }
    }

    private fun doWriteSession(packageInstaller: PackageInstaller, sessionId: Int, apkFile: File): Int {
        var session: PackageInstaller.Session? = null
        try {

            session = packageInstaller.openSession(sessionId)

            val inputStream = apkFile.inputStream()
            val outputStream = session.openWrite(apkFile.name, 0, apkFile.length())
            val buf = ByteArray(8192)
            var len: Int

            while (inputStream.read(buf).also { len = it } > 0) {
                outputStream.write(buf, 0, len)
                outputStream.flush()
                session.fsync(outputStream)
            }
            session.fsync(outputStream)
            inputStream.close()
            outputStream.close()

            Log.d(logObjectTag, TAG, "Success: streamed bytes")
            return PackageInstaller.STATUS_SUCCESS
        } catch (e: IOException) {
            Log.e(logObjectTag, TAG, "Error: failed to write; " + e.message)
            return PackageInstaller.STATUS_FAILURE
        } finally {
            try {
                session?.close()
            } catch (e: IOException) {
                Log.e(logObjectTag, TAG, "Error: failed to close session; " + e.message)
            }
        }
    }

    private fun doCommitSession(packageInstaller: PackageInstaller, sessionId: Int) {
        var session: PackageInstaller.Session? = null
        try {
            session = packageInstaller.openSession(sessionId)
            val callbackIntent = Intent(context, ApkInstallerService::class.java)
            val pendingIntent = PendingIntent.getService(context, 0, callbackIntent, 0)
            session.commit(pendingIntent.intentSender)
            session.close()
            Log.d(logObjectTag, TAG, "doCommitSession: install request sent, " + packageInstaller.mySessions)
        } catch (e: IOException) {
            Log.e(logObjectTag, TAG, "doCommitSession: " + e.message)
        } finally {
            session!!.close()
        }
    }
}
