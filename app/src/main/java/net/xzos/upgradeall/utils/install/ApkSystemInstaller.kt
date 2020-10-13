package net.xzos.upgradeall.utils.install

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.core.oberver.Informer
import net.xzos.upgradeall.utils.ToastUtil
import java.io.File


object ApkSystemInstaller : Informer {

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

}
