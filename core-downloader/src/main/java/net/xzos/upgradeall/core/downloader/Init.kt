package net.xzos.upgradeall.core.downloader

import android.annotation.SuppressLint
import io.reactivex.plugins.RxJavaPlugins
import net.xzos.upgradeall.core.utils.log.Log
import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.core.utils.log.ObjectTag.Companion.core

@SuppressLint("StaticFieldLeak")
lateinit var downloadConfig: DownloadConfig

fun initDownload(
    _downloadConfig: DownloadConfig,
) {
    patchRxJava()
    downloadConfig = _downloadConfig
}

// fix rxjava
private fun patchRxJava() {
    if (RxJavaPlugins.getErrorHandler() == null) {
        RxJavaPlugins.setErrorHandler {
            val tag = "RxJava"
            val logObjectTag = ObjectTag(core, tag)
            Log.e(logObjectTag, tag, it.stackTraceToString())
        }
    }
}
