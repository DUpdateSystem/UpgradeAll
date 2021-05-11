package net.xzos.upgradeall.core.downloader

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import net.xzos.upgradeall.core.utils.file.FileUtil
import net.xzos.upgradeall.core.utils.file.copyAllFile
import java.io.File
import java.util.*

class DownloadFile {
    val documentFile: DocumentFile by lazy {
        getDownloadDirDocumentFile(
            UUID.randomUUID().toString()
        )
    }
    private val tmpFile by lazy {
        val file = File(documentFile.uri.toString())
        if (file.canRead()) file
        else FileUtil.getNewRandomNameFile(FileUtil.DOWNLOAD_EXTRA_CACHE_DIR, true)
    }

    suspend fun getTmpFile(context: Context): File {
        documentFile.copyAllFile(tmpFile, context)
        return tmpFile
    }

    fun delete() {
        documentFile.delete()
        tmpFile.delete()
    }
}