package net.xzos.upgradeall.core.downloader.filedownloader.fetch

import com.tonyodev.fetch2core.Downloader.FileDownloaderType
import com.tonyodev.fetch2core.Downloader.ServerRequest
import com.tonyodev.fetch2okhttp.OkHttpDownloader
import net.xzos.upgradeall.core.downloader.downloadConfig
import okhttp3.OkHttpClient


class CustomDownloader @JvmOverloads constructor(
    okHttpClient: OkHttpClient? = null, private val downloadThreadNum: Int
) : OkHttpDownloader(okHttpClient) {
    override fun getRequestFileDownloaderType(
        request: ServerRequest, supportedFileDownloaderTypes: Set<FileDownloaderType>
    ): FileDownloaderType {
        return FileDownloaderType.PARALLEL  // For chunk downloading
    }

    override fun getFileSlicingCount(request: ServerRequest, contentLength: Long): Int {
        return downloadThreadNum
    }
}

fun getDownloader(): OkHttpDownloader {
    val client: OkHttpClient = OkHttpClient.Builder().build()
    return CustomDownloader(client, downloadConfig.DOWNLOAD_THREAD_NUM)
}