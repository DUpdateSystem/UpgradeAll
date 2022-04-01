package net.xzos.upgradeall.ui.filemanagement

import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import androidx.databinding.ObservableField
import net.xzos.upgradeall.core.downloader.filedownloader.item.DownloadStatus
import net.xzos.upgradeall.core.downloader.filedownloader.item.DownloadStatusSnap
import net.xzos.upgradeall.core.downloader.filedownloader.item.Downloader
import net.xzos.upgradeall.ui.base.list.ActivityListItemView
import net.xzos.upgradeall.ui.base.list.BaseAppIconItem
import net.xzos.upgradeall.wrapper.download.DownloadTasker

class FileItemView(
    name: String, val fileTasker: DownloadTasker,
) : BaseAppIconItem, ActivityListItemView {
    private val numUtil = DownloadTaskerNumUtil(fileTasker.downloader)
    suspend fun getDownloadingNum() = numUtil.getDownloadingNum().toString()
    suspend fun getPauseNum() = numUtil.getPauseNum().toString()
    suspend fun getCompletedNum() = numUtil.getCompletedNum().toString()
    suspend fun getFailedNum() = numUtil.getFailedNum().toString()
    suspend fun getDownloadProgress() = numUtil.getDownloadProgress()
    override val appName: ObservableField<String> = ObservableField(name)
    override val nameFirst: ObservableField<String> = ObservableField()
    override val appIcon: ObservableField<Drawable> = ObservableField()
    override val iconBackgroundTint: ObservableField<ColorStateList?> = ObservableField()

    override fun getItemIdName(): String {
        return appName.get().toString()
    }
}

class DownloadTaskerNumUtil(private val downloader: Downloader?) {

    suspend fun getDownloadingNum() = getDownloadNumByStatus(DownloadStatus.RUNNING)
    suspend fun getPauseNum() = getDownloadNumByStatus(DownloadStatus.STOP)
    suspend fun getCompletedNum() = getDownloadNumByStatus(DownloadStatus.COMPLETE)
    suspend fun getFailedNum() = getDownloadNumByStatus(DownloadStatus.FAIL)
    suspend fun getDownloadProgress() = downloader?.getDownloadProgress() ?: -1

    private fun getDownloadNumByStatus(status: DownloadStatus) =
        getDownloadNumByStatus { it.status == status }

    private fun getDownloadNumByStatus(predicate: (DownloadStatusSnap) -> Boolean): Int {
        return this.downloader?.getDownloadNumByStatus(predicate) ?: 0
    }

    private fun Downloader.getDownloadNumByStatus(predicate: (DownloadStatusSnap) -> Boolean): Int {
        return downloader?.getStatusList()?.filter(predicate)?.size ?: 0
    }
}