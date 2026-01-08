package net.xzos.upgradeall.getter.rpc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import java.io.File

/**
 * Helper functions to create and use Rust-based downloaders
 */
object DownloaderHelper {
    /**
     * Create a RustDownloaderAdapter from GetterService
     */
    fun createRustDownloader(
        getterService: GetterService,
        downloadDir: File,
        scope: CoroutineScope = GlobalScope
    ): RustDownloaderAdapter {
        downloadDir.mkdirs()
        return RustDownloaderAdapter(downloadDir, getterService, scope)
    }
}

/**
 * Convert DownloadItem to RustInputData
 */
fun net.xzos.upgradeall.websdk.data.json.DownloadItem.toRustInputData(filename: String): RustInputData {
    return RustInputData(
        name = filename,
        url = this.url,
        headers = this.headers ?: emptyMap(),
        cookies = this.cookies ?: emptyMap()
    )
}
