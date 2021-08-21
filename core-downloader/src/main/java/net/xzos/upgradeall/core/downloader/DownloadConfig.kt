package net.xzos.upgradeall.core.downloader

import android.content.Context
import androidx.documentfile.provider.DocumentFile

class DownloadConfig(
    @JvmField internal val ANDROID_CONTEXT: Context,
    internal val DOWNLOAD_DOCUMENT_FILE: DocumentFile,
    internal val DOWNLOAD_MAX_TASK_NUM: Int,
    internal val DOWNLOAD_THREAD_NUM: Int,
    internal val DOWNLOAD_AUTO_RETRY_MAX_ATTEMPTS: Int,
)