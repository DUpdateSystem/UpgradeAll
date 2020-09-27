package net.xzos.upgradeall.server.downloader

import com.tonyodev.fetch2core.Downloader
import com.tonyodev.fetch2core.Downloader.FileDownloaderType
import com.tonyodev.fetch2okhttp.OkHttpDownloader
import net.xzos.upgradeall.data.PreferencesMap
import okhttp3.OkHttpClient


class CustomDownloader @JvmOverloads constructor(okHttpClient: OkHttpClient? = null) : OkHttpDownloader(okHttpClient) {
    override fun getRequestFileDownloaderType(request: Downloader.ServerRequest, supportedFileDownloaderTypes: Set<FileDownloaderType>): FileDownloaderType {
        return FileDownloaderType.PARALLEL //For chunk downloading
    }

    override fun getFileSlicingCount(request: Downloader.ServerRequest, contentLength: Long): Int? {
        return PreferencesMap.download_thread_num
    }
}

fun getDownloader(): OkHttpDownloader {
    val client: OkHttpClient = OkHttpClient.Builder().build()
    return CustomDownloader(client)
}
