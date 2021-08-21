package net.xzos.upgradeall.core.downloader

import android.annotation.SuppressLint

@SuppressLint("StaticFieldLeak")
lateinit var downloadConfig: DownloadConfig

fun initConfig(_downloadConfig: DownloadConfig) {
    downloadConfig = _downloadConfig
}
