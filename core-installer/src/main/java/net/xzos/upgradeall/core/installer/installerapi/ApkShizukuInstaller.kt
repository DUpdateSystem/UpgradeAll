package net.xzos.upgradeall.core.installer.installerapi

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.IPackageInstaller
import android.content.pm.IPackageInstallerSession
import android.content.pm.IPackageManager
import android.content.pm.PackageInstaller
import android.os.Process
import net.xzos.upgradeall.core.androidutils.requestPermission
import net.xzos.upgradeall.core.installer.installerapi.shizuku.IIntentSenderAdaptor
import net.xzos.upgradeall.core.installer.installerapi.shizuku.IntentSenderUtils
import net.xzos.upgradeall.core.installer.installerapi.shizuku.PackageInstallerUtils
import net.xzos.upgradeall.core.installer.installerapi.shizuku.ShizukuUtils
import net.xzos.upgradeall.core.utils.log.Log
import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.core.utils.log.ObjectTag.Companion.core
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.ShizukuProvider
import rikka.shizuku.SystemServiceHelper
import java.io.File
import java.util.concurrent.CountDownLatch


object ApkShizukuInstaller {

    private const val TAG = "ApkShizukuInstaller"
    private val logObjectTag = ObjectTag(core, TAG)


    @Suppress("ObjectPropertyName")
    private val _packageManager: IPackageManager by lazy {
        IPackageManager.Stub.asInterface(ShizukuBinderWrapper(SystemServiceHelper.getSystemService("package")))
    }

    @Suppress("ObjectPropertyName")
    private val _packageInstaller: IPackageInstaller by lazy {
        IPackageInstaller.Stub.asInterface(ShizukuBinderWrapper(_packageManager.packageInstaller.asBinder()))
    }

    @Suppress("RedundantSuspendModifier")
    suspend fun install(file: File, context: Context) {
        val shizukuUtils = ShizukuUtils(context)
        shizukuUtils.bindUserServiceStandaloneProcess()
        doApkInstall(file, context)
        shizukuUtils.unbindUserServiceStandaloneProcess()
    }

    @Suppress("RedundantSuspendModifier")
    suspend fun multipleInstall(apkFileList: List<File>, context: Context) {
        val shizukuUtils = ShizukuUtils(context)
        shizukuUtils.bindUserServiceStandaloneProcess()
        doMultipleInstall(apkFileList, context)
        shizukuUtils.unbindUserServiceStandaloneProcess()
    }

    private fun doApkInstall(file: File, context: Context) {
        var session: PackageInstaller.Session? = null
        val res: StringBuilder = StringBuilder()

        var tr: Throwable? = null

        try {
            session = getPackageInstallerSession(context)

            res.append('\n').append("write: ")
            doWriteSession(session, file)

            res.append('\n').append("commit: ")
            val result = doCommitSession(session)
            val status =
                result!!.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
            val message = result.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
            res.append('\n').append("status: ").append(status).append(" (").append(message)
                .append(")")
        } catch (e: Throwable) {
            res.append(e.stackTraceToString())
            tr = e
        } finally {
            try {
                session?.close()
            } catch (e: Throwable) {
                res.append(e.stackTraceToString())
            }
            Log.e(logObjectTag, TAG, res.toString())
            if (tr != null)
                throw tr
        }
        Log.i(logObjectTag, TAG, res.toString())
    }

    private fun doMultipleInstall(apkFileList: List<File>, context: Context) {
        val res: StringBuilder = StringBuilder()
        var totalSize: Long = 0

        try {
            for (listOfFile in apkFileList) {
                if (listOfFile.isFile) {
                    res.append('\n').append("check file: " + listOfFile.name)
                    totalSize += listOfFile.length()
                }
            }
        } catch (e: Exception) {
            res.append('\n').append("error: " + e.stackTraceToString())
            Log.e(logObjectTag, TAG, res.toString())
            throw e
        }

        val session = getPackageInstallerSession(context, totalSize)
        var tr: Throwable? = null
        try {
            for (apkFile in apkFileList) {
                res.append('\n').append("write: ")
                doWriteSession(session, apkFile)
            }
            res.append('\n').append("commit: ")
            val result = doCommitSession(session)
            val status =
                result!!.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
            val message = result.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
            res.append('\n').append("status: ").append(status).append(" (").append(message)
                .append(")")
        } catch (e: Throwable) {
            res.append(e.stackTraceToString())
            tr = e
        } finally {
            @Suppress("SameParameterValue")
            try {
                session.close()
            } catch (tr: Throwable) {
                res.append(tr)
            }
            Log.e(logObjectTag, TAG, res.toString())
            if (tr != null)
                throw tr
        }
        Log.i(logObjectTag, TAG, res.toString())
    }

    private fun getPackageInstaller(context: Context): PackageInstaller {
        val isRoot = Shizuku.getUid() == 0
        // the reason for use "com.android.shell" as installer package under adb is that getMySessions will check installer package's owner
        val installerPackageName = if (isRoot) context.packageName else "com.android.shell"
        val userId = if (isRoot) Process.myUserHandle().hashCode() else 0
        return PackageInstallerUtils.createPackageInstaller(
            context, _packageInstaller, installerPackageName, userId
        )
    }

    private fun getPackageInstallerSession(
        context: Context,
        totalSize: Long? = null
    ): PackageInstaller.Session {
        val res: StringBuilder = StringBuilder()
        res.append("createSession: ")

        val params: PackageInstaller.SessionParams =
            PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        totalSize?.let {
            params.setSize(totalSize)
        }
        var installFlags = PackageInstallerUtils.getInstallFlags(params)
        installFlags =
            installFlags or (0x00000004 /*PackageManager.INSTALL_ALLOW_TEST*/ or 0x00000002) /*PackageManager.INSTALL_REPLACE_EXISTING*/
        PackageInstallerUtils.setInstallFlags(params, installFlags)

        val packageInstaller = getPackageInstaller(context)
        val sessionId = packageInstaller.createSession(params)
        res.append(sessionId)
        @Suppress("LocalVariableName")
        val _session = IPackageInstallerSession.Stub.asInterface(
            ShizukuBinderWrapper(
                _packageInstaller.openSession(sessionId).asBinder()
            )
        )
        Log.i(logObjectTag, TAG, res.toString())
        return PackageInstallerUtils.createSession(_session)
    }

    private fun doWriteSession(session: PackageInstaller.Session, apkFile: File) {
        val inputStream = apkFile.inputStream()
        val outputStream = session.openWrite(apkFile.name, 0, -1)
        val buf = ByteArray(8192)
        var len: Int

        while (inputStream.read(buf).also { len = it } > 0) {
            outputStream.write(buf, 0, len)
            outputStream.flush()
            session.fsync(outputStream)
        }
        inputStream.close()
        outputStream.close()
    }

    private fun doCommitSession(session: PackageInstaller.Session): Intent? {
        val results = arrayOf<Intent?>(null)
        val countDownLatch = CountDownLatch(1)
        val intentSender: IntentSender =
            IntentSenderUtils.newInstance(object : IIntentSenderAdaptor() {
                override fun send(intent: Intent?) {
                    results[0] = intent
                    countDownLatch.countDown()
                }
            })
        session.commit(intentSender)

        countDownLatch.await()
        return results[0]
    }

    fun initByActivity(
        activity: Activity,
        PERMISSIONS_REQUEST_CONTACTS: Int,
    ) {
        Shizuku.addBinderReceivedListenerSticky { }
        Shizuku.addBinderDeadListener { }
        Shizuku.addRequestPermissionResultListener { _: Int, _: Int -> }
        ShizukuUtils.checkPermission(activity, PERMISSIONS_REQUEST_CONTACTS)
    }

    fun requestShizukuPermission(
        activity: Activity,
        PERMISSIONS_REQUEST_CONTACTS: Int,
    ): Boolean {
        return requestPermission(
            activity, ShizukuProvider.PERMISSION,
            PERMISSIONS_REQUEST_CONTACTS, "shizuku permission request"
        )
    }
}