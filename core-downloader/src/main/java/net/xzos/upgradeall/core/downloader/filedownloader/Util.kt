package net.xzos.upgradeall.core.downloader.filedownloader

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.tonyodev.fetch2.Error
import net.xzos.upgradeall.core.downloader.filedownloader.item.Downloader
import net.xzos.upgradeall.core.downloader.filetasker.FileTaskerManager
import net.xzos.upgradeall.core.downloader.service.DownloadService
import net.xzos.upgradeall.core.utils.log.Log
import java.io.File
import java.util.*

private const val DOWNLOAD_CANCELLED = "DOWNLOAD_CANCELLED"

fun renewDownloadServiceStatus() {
    if (DownloaderManager.getDownloaderList().isEmpty()
        || FileTaskerManager.getFileTaskerList().isEmpty()
    ) {
        DownloadService.close()
    }
}

fun getDownloadDirDocumentFile(name: String, parent: DocumentFile): DocumentFile {
    return parent.createDirectory(name) ?: throw DownloadFileError(parent, name).apply {
        Log.e(
            Downloader.logTagObject, Downloader.TAG,
            "getDownloadDirDocumentFile: download file create file fail, path: ${parent.uri.path} name: $name"
        )
    }
}

fun getDownloadUrl(mimeType: String, fileName: String, parent: DocumentFile): Uri {
    return parent.createFile(mimeType, fileName)?.uri ?: throw DownloadFileError(
        parent,
        fileName
    ).apply {
        Log.e(
            Downloader.logTagObject, "getDownloadUrl",
            "getDownloadUrl: download file create file fail, path: ${parent.uri.path} name: $fileName"
        )
    }
}

fun getNewRandomNameFile(targetDir: File, isDir: Boolean): File {
    if (!targetDir.exists())
        targetDir.mkdirs()
    val randomName = UUID.randomUUID().toString()
    return File(targetDir, randomName).also {
        if (isDir)
            it.mkdir()
        else
            it.createNewFile()
    }
}

class DownloadFileError internal constructor(
    val parent: DocumentFile, val fileName: String
) : RuntimeException()

class DownloadCanceledError internal constructor(
    val msg: String? = null
) : RuntimeException(DOWNLOAD_CANCELLED)

class DownloadFetchError internal constructor(
    val error: Error
) : RuntimeException(DOWNLOAD_CANCELLED)
