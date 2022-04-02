package net.xzos.upgradeall.core.downloader.filedownloader.item

import net.xzos.upgradeall.core.downloader.filedownloader.getNewRandomNameFile
import java.io.File

fun getDownloadDir(parent: File) = getNewRandomNameFile(parent, true)