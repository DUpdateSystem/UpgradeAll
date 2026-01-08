package net.xzos.upgradeall.ui.filemanagement

import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import androidx.databinding.ObservableField
import net.xzos.upgradeall.core.downloader.filedownloader.item.Status
import net.xzos.upgradeall.getter.rpc.RustDownloadStatus
import net.xzos.upgradeall.getter.rpc.RustDownloaderAdapter
import net.xzos.upgradeall.ui.base.list.ActivityListItemView
import net.xzos.upgradeall.ui.base.list.BaseAppIconItem
import net.xzos.upgradeall.wrapper.download.DownloadTasker

class FileItemView(
    name: String, val fileTasker: DownloadTasker,
) : BaseAppIconItem, ActivityListItemView {
    private val numUtil = DownloadTaskerNumUtil(fileTasker.rustDownloader)
    fun getDownloadingNum() = numUtil.getDownloadingNum().toString()
    fun getPauseNum() = numUtil.getPauseNum().toString()
    fun getCompletedNum() = numUtil.getCompletedNum().toString()
    fun getFailedNum() = numUtil.getFailedNum().toString()
    fun getDownloadProgress() = numUtil.getDownloadProgress()
    override val appName: ObservableField<String> = ObservableField(name)
    override val nameFirst: ObservableField<String> = ObservableField()
    override val appIcon: ObservableField<Drawable> = ObservableField()
    override val iconBackgroundTint: ObservableField<ColorStateList?> = ObservableField()

    override fun getItemIdName(): String {
        return appName.get().toString()
    }
}

class DownloadTaskerNumUtil(private val downloader: RustDownloaderAdapter?) {

    fun getDownloadingNum() = getDownloadNumByStatus(RustDownloadStatus.RUNNING)
    fun getPauseNum() = getDownloadNumByStatus(RustDownloadStatus.STOP)
    fun getCompletedNum() = getDownloadNumByStatus(RustDownloadStatus.COMPLETE)
    fun getFailedNum() = getDownloadNumByStatus(RustDownloadStatus.FAIL)
    fun getDownloadProgress() = downloader?.getDownloadProgress() ?: -1

    private fun getDownloadNumByStatus(status: RustDownloadStatus) =
        getDownloadNumByStatus { it == status }

    private fun getDownloadNumByStatus(predicate: (RustDownloadStatus) -> Boolean): Int {
        return downloader?.getTaskList()?.mapNotNull { it.snap.status }?.filter(predicate)?.size ?: 0
    }
}