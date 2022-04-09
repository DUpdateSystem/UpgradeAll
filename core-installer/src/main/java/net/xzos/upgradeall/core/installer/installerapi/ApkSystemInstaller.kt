package net.xzos.upgradeall.core.installer.installerapi

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import net.xzos.upgradeall.core.installer.getApkUri
import net.xzos.upgradeall.core.installer.service.ApkInstallerService
import net.xzos.upgradeall.core.utils.log.Log
import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.core.utils.log.ObjectTag.Companion.core
import net.xzos.upgradeall.core.utils.log.msg
import java.io.File
import java.io.IOException


object ApkSystemInstaller {

    private const val TAG = "ApkSystemInstaller"
    private val logObjectTag = ObjectTag(core, TAG)


    suspend fun install(file: File, context: Context) {
        try {
            val fileUri = file.getApkUri(context)
            rowInstall(fileUri, context)
        } catch (e: IllegalArgumentException) {
            Log.e(logObjectTag, TAG, e.stackTraceToString())
            throw e
        }
    }

    private fun rowInstall(fileUri: Uri, context: Context) {
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

    suspend fun multipleInstall(apkFileList: List<File>, context: Context) {
        doMultipleInstall(apkFileList, context)
    }

    private fun doMultipleInstall(apkFileList: List<File>, context: Context) {
        var totalSize: Long = 0
        try {
            for (listOfFile in apkFileList) {
                if (listOfFile.isFile) {
                    Log.d(logObjectTag, TAG, "multipleInstall: " + listOfFile.name)
                    totalSize += listOfFile.length()
                }
            }
        } catch (e: Exception) {
            Log.e(logObjectTag, TAG, "multipleInstall: " + e.msg())
            return
        }
        val packageInstaller = context.packageManager.packageInstaller
        val installParams =
            PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        installParams.setSize(totalSize)
        try {
            val sessionId = packageInstaller.createSession(installParams)
            Log.d(logObjectTag, TAG, "Success: created install session [$sessionId]")
            val session = packageInstaller.openSession(sessionId)
            for (apkFile in apkFileList) {
                doWriteSession(session, apkFile)
            }
            doCommitSession(session, context)
            Log.d(logObjectTag, TAG, "Success")
        } catch (e: IOException) {
            Log.e(logObjectTag, TAG, "multipleInstall: " + e.msg())
        }
    }

    private fun doWriteSession(session: PackageInstaller.Session, apkFile: File) {
        try {

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
        } catch (e: IOException) {
            Log.e(logObjectTag, TAG, "Error: failed to write; " + e.msg())
        } finally {
            try {
                session.close()
            } catch (e: IOException) {
                Log.e(logObjectTag, TAG, "Error: failed to close session; " + e.msg())
            }
        }
    }

    private fun doCommitSession(session: PackageInstaller.Session, context: Context) {
        try {
            val callbackIntent = Intent(context, ApkInstallerService::class.java)
            val pendingIntent = PendingIntent.getService(
                context,
                0,
                callbackIntent,
                net.xzos.upgradeall.core.androidutils.FlagDelegate.PENDING_INTENT_FLAG_IMMUTABLE
            )
            session.commit(pendingIntent.intentSender)
            session.close()
            Log.d(logObjectTag, TAG, "doCommitSession: install request sent")
        } catch (e: IOException) {
            Log.e(logObjectTag, TAG, "doCommitSession: " + e.msg())
        } finally {
            session.close()
        }
    }

    suspend fun obbInstall(obbFileList: List<File>) {
        // 参考: https://stackoverflow.com/questions/55212788/is-it-possible-to-merge-install-split-apk-files-aka-app-bundle-on-android-d
        for (obbFile in obbFileList) {
            val delimiterIndexList = mutableListOf<Int>()
            val fileName = obbFile.name
            Log.d(logObjectTag, TAG, "multipleInstall: obb name: $fileName")
            var index: Int = fileName.indexOf('.')
            delimiterIndexList.add(index)
            while (true) {
                index = fileName.indexOf('.', index + 1)
                if (index >= 0)
                    delimiterIndexList.add(index)
                else
                    break
            }
            val obbPackageName = fileName.subSequence(
                delimiterIndexList[1] + 1,
                delimiterIndexList.last()
            )
            val command = "mv $obbFile /storage/emulated/0/Android/obb/$obbPackageName/."
            Log.d(logObjectTag, TAG, "multipleInstall: obb command: $command")

            obbFile.renameTo(File("/storage/emulated/0/Android/obb/$obbPackageName/", obbFile.name))
        }
    }
}