package net.xzos.upgradeall.utils.install

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.IPackageInstaller
import android.content.pm.IPackageInstallerSession
import android.content.pm.IPackageManager
import android.content.pm.PackageInstaller
import android.os.Process
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.shizuku.api.ShizukuApiConstants
import moe.shizuku.api.ShizukuBinderWrapper
import moe.shizuku.api.ShizukuService
import moe.shizuku.api.SystemServiceHelper
import net.xzos.upgradeall.R
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.core.data.json.nongson.ObjectTag
import net.xzos.upgradeall.core.log.Log
import net.xzos.upgradeall.utils.MiscellaneousUtils
import net.xzos.upgradeall.utils.install.shizuku.IIntentSenderAdaptor
import net.xzos.upgradeall.utils.install.shizuku.IntentSenderUtils
import net.xzos.upgradeall.utils.install.shizuku.PackageInstallerUtils
import java.io.File
import java.util.concurrent.CountDownLatch


object ApkShizukuInstaller {

    private const val TAG = "ApkShizukuInstaller"
    private val logObjectTag = ObjectTag(ObjectTag.core, TAG)

    @Suppress("ObjectPropertyName")
    private val _packageManager: IPackageManager by lazy {
        IPackageManager.Stub.asInterface(ShizukuBinderWrapper(SystemServiceHelper.getSystemService("package")))
    }

    @Suppress("ObjectPropertyName")
    private val _packageInstaller: IPackageInstaller by lazy {
        IPackageInstaller.Stub.asInterface(ShizukuBinderWrapper(_packageManager.packageInstaller.asBinder()))
    }

    suspend fun install(file: File) {
        withContext(Dispatchers.Default) {
            doApkInstall(file)
        }
        ApkInstaller.completeInstall(file)
    }

    private fun doApkInstall(file: File) {
        var session: PackageInstaller.Session? = null
        val installerPackageName: String
        val userId: Int
        val isRoot: Boolean
        val packageInstaller: PackageInstaller
        val res: StringBuilder = StringBuilder()

        try {
            isRoot = ShizukuService.getUid() == 0
            // the reason for use "com.android.shell" as installer package under adb is that getMySessions will check installer package's owner
            installerPackageName = if (isRoot) MyApplication.context.packageName else "com.android.shell"
            userId = if (isRoot) Process.myUserHandle().hashCode() else 0
            packageInstaller = PackageInstallerUtils.createPackageInstaller(_packageInstaller, installerPackageName, userId)

            val sessionId: Int
            res.append("createSession: ")

            val params: PackageInstaller.SessionParams = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            var installFlags = PackageInstallerUtils.getInstallFlags(params)
            installFlags = installFlags or (0x00000004 /*PackageManager.INSTALL_ALLOW_TEST*/ or 0x00000002) /*PackageManager.INSTALL_REPLACE_EXISTING*/
            PackageInstallerUtils.setInstallFlags(params, installFlags)

            sessionId = packageInstaller.createSession(params)
            res.append(sessionId).append('\n')

            res.append('\n').append("write: ")
            @Suppress("LocalVariableName")
            val _session = IPackageInstallerSession.Stub.asInterface(ShizukuBinderWrapper(_packageInstaller.openSession(sessionId).asBinder()))
            session = PackageInstallerUtils.createSession(_session)

            val inputStream = file.inputStream()
            val outputStream = session.openWrite("1.apk", 0, -1)
            val buf = ByteArray(8192)
            var len: Int

            while (inputStream.read(buf).also { len = it } > 0) {
                outputStream.write(buf, 0, len)
                outputStream.flush()
                session.fsync(outputStream)
            }
            inputStream.close()
            outputStream.close()

            res.append('\n').append("commit: ")

            val results = arrayOf<Intent?>(null)
            val countDownLatch = CountDownLatch(1)
            val intentSender: IntentSender = IntentSenderUtils.newInstance(object : IIntentSenderAdaptor() {
                override fun send(intent: Intent?) {
                    results[0] = intent
                    countDownLatch.countDown()
                }
            })
            session.commit(intentSender)

            countDownLatch.await()
            val result = results[0]
            val status = result!!.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
            val message = result.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
            res.append('\n').append("status: ").append(status).append(" (").append(message).append(")")
        } catch (tr: Throwable) {
            tr.printStackTrace()
            res.append(tr)
        } finally {
            if (session != null) {
                try {
                    session.close()
                } catch (tr: Throwable) {
                    res.append(tr)
                }
            }
        }
        Log.i(logObjectTag, TAG, res.toString())
        MiscellaneousUtils.showToast(res.toString())
    }

    fun requestShizukuPermission(activity: Activity, PERMISSIONS_REQUEST_CONTACTS: Int): Boolean {
        return MiscellaneousUtils.requestPermission(
                activity, ShizukuApiConstants.PERMISSION,
                PERMISSIONS_REQUEST_CONTACTS, R.string.shizuku_permission_request)
    }
}
