package net.xzos.upgradeall.utils

import net.xzos.upgradeall.core.downloader.filedownloader.item.TaskSnap

fun TaskSnap?.progress() = this?.let { downloadSize / totalSize * 100 } ?: -1L
