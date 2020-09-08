package net.xzos.upgradeall.utils.install

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.StrictMode
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.xzos.upgradeall.BuildConfig
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.application.MyApplication.Companion.context
import net.xzos.upgradeall.core.oberver.Informer
import net.xzos.upgradeall.utils.ToastUtil
import java.io.File


object ApkSystemInstaller : Informer {

    private val context: Context = MyApplication.context

    suspend fun install(file: File) {
        withContext(Dispatchers.Default) {
            try {
                val fileUri = file.getApkUri()
                rowInstall(fileUri)
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
                ToastUtil.makeText(e.toString())
            }
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

@Throws(IllegalArgumentException::class)
fun File.getUri(): Uri {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
        try {
            FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileprovider", this)
        } catch (e: IllegalArgumentException) { throw e }
    else Uri.fromFile(this)
}

/**
 * 修复后缀名
 */
fun File.autoAddApkExtension(): File {
    if (this.isApkFile()) {
        if (this.extension != "apk") {
            this.renameTo(File(parent, "$name.apk"))
        }
    }
    return this
}

@Throws(IllegalArgumentException::class)
fun File.getApkUri(): Uri {
    // 修复后缀名
    val apkFile = this.autoAddApkExtension()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        try {
            val m = StrictMode::class.java.getMethod("disableDeathOnFileUriExposure")
            m.invoke(null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return try {
        apkFile.getUri()
    } catch (e: IllegalArgumentException) {
        throw e
    }
}

