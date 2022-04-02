package net.xzos.upgradeall.ui.filemanagement

import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import androidx.databinding.ObservableField
import net.xzos.upgradeall.core.downloader.filedownloader.item.Downloader
import net.xzos.upgradeall.core.downloader.filedownloader.item.Status
import net.xzos.upgradeall.ui.base.list.ActivityListItemView
import net.xzos.upgradeall.ui.base.list.BaseAppIconItem
import net.xzos.upgradeall.wrapper.download.DownloadTasker

class FileItemView(
    name: String, val fileTasker: DownloadTasker,
) : BaseAppIconItem, ActivityListItemView {
    private val numUtil = DownloadTaskerNumUtil(fileTasker.downloader)
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

class DownloadTaskerNumUtil(private val downloader: Downloader?) {

    fun getDownloadingNum() = getDownloadNumByStatus(Status.RUNNING)
    fun getPauseNum() = getDownloadNumByStatus(Status.STOP)
    fun getCompletedNum() = getDownloadNumByStatus(Status.COMPLETE)
    fun getFailedNum() = getDownloadNumByStatus(Status.FAIL)
    fun getDownloadProgress() = downloader?.getDownloadProgress() ?: -1

    private fun getDownloadNumByStatus(status: Status) =
        getDownloadNumByStatus { it == status }

    private fun getDownloadNumByStatus(predicate: (Status) -> Boolean): Int {
        return this.downloader?.getDownloadNumByStatus(predicate) ?: 0
    }

    private fun Downloader.getDownloadNumByStatus(predicate: (Status) -> Boolean): Int {
        return downloader?.getTaskList()?.mapNotNull { it.snap?.status }?.filter(predicate)?.size
            ?: 0
    }
}