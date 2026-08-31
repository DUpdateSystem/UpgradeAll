package net.xzos.upgradeall

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import java.util.concurrent.CopyOnWriteArrayList

internal data class AndroidPackageInstallerRequest(
    val packageName: String,
    val apkPath: String,
)

internal object AndroidPackageInstallerContract {
    private val requestFields = setOf("package_name", "apk_path")

    fun parseRequest(arguments: Map<*, *>): AndroidPackageInstallerRequest {
        val unknownFields = arguments.keys.filterNot { it in requestFields }
        require(unknownFields.isEmpty()) {
            "unknown debug PackageInstaller fields: ${unknownFields.joinToString()}"
        }
        val packageName = arguments["package_name"] as? String
        require(!packageName.isNullOrBlank()) { "package_name is required" }
        val apkPath = arguments["apk_path"] as? String
        require(!apkPath.isNullOrBlank()) { "apk_path is required" }
        return AndroidPackageInstallerRequest(packageName, apkPath)
    }

    fun statusName(status: Int): String = when (status) {
        PackageInstaller.STATUS_PENDING_USER_ACTION -> "pending_user_action"
        PackageInstaller.STATUS_SUCCESS -> "succeeded"
        PackageInstaller.STATUS_FAILURE_ABORTED -> "aborted"
        else -> "failed"
    }
}

internal object AndroidPackageInstallerEvents {
    private val listeners = CopyOnWriteArrayList<(Intent) -> Unit>()

    fun add(listener: (Intent) -> Unit) {
        listeners.add(listener)
    }

    fun remove(listener: (Intent) -> Unit) {
        listeners.remove(listener)
    }

    fun emit(intent: Intent) {
        listeners.forEach { it(intent) }
    }
}

class AndroidPackageInstallerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AndroidPackageInstallerEvents.emit(intent)
    }
}
