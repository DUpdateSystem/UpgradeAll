package net.xzos.upgradeall.core.downloader

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.tonyodev.fetch2.Error
import net.xzos.upgradeall.core.R
import net.xzos.upgradeall.core.coreConfig
import net.xzos.upgradeall.core.filetasker.FileTaskerManager
import net.xzos.upgradeall.core.log.Log

private const val DOWNLOAD_CANCELLED = "DOWNLOAD_CANCELLED"

fun renewDownloadServiceStatus() {
    if (DownloaderManager.getDownloaderList().isEmpty()
            || FileTaskerManager.getFileTaskerList().isEmpty()) {
        DownloadService.close()
    }
}

fun getDownloadDirDocumentFile(name: String): DocumentFile {
    val documentFile = coreConfig.download_document_file
    return documentFile.createDirectory(name) ?: throw DownloadFileError(
            documentFile, name
    ).apply {
        Log.e(Downloader.logTagObject, "getDownloadUrl",
                "${coreConfig.androidContext.getString(R.string.download_file_create_error)} path: ${documentFile.uri.path} name: $name"
        )
    }
}

fun getDownloadUrl(mimeType: String, fileName: String, parent: DocumentFile): Uri {
    return parent.createFile(mimeType, fileName)?.uri ?: throw DownloadFileError(
            parent, fileName
    ).apply {
        Log.e(Downloader.logTagObject, "getDownloadUrl",
                "${coreConfig.androidContext.getString(R.string.download_file_create_error)} path: ${parent.uri.path} name: $fileName"
        )
    }
}

class DownloadFileError internal constructor(val parent: DocumentFile, val fileName: String) : RuntimeException()
class DownloadCanceledError internal constructor() : RuntimeException(DOWNLOAD_CANCELLED)
class DownloadFetchError internal constructor(error: Error) : RuntimeException(DOWNLOAD_CANCELLED)
