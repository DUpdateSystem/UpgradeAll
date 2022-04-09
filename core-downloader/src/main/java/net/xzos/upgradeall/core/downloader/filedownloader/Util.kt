package net.xzos.upgradeall.core.downloader.filedownloader

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import net.xzos.upgradeall.core.downloader.filedownloader.item.Downloader
import net.xzos.upgradeall.core.utils.log.Log
import java.io.File
import java.util.*

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