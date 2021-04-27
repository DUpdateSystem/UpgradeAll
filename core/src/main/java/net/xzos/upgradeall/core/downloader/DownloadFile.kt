package net.xzos.upgradeall.core.downloader

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import net.xzos.upgradeall.core.utils.file.FileUtil
import net.xzos.upgradeall.core.utils.file.copyAllFile
import java.io.File
import java.util.*

class DownloadFile(
        val documentFile: DocumentFile = getDownloadDirDocumentFile(UUID.randomUUID().toString())
) {
    private val tmpFile by lazy {
        FileUtil.getNewRandomNameFile(FileUtil.DOWNLOAD_CACHE_DIR, true)
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