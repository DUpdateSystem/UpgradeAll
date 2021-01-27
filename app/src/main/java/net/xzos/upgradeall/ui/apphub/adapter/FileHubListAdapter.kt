package net.xzos.upgradeall.ui.apphub.adapter

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.tonyodev.fetch2.Status
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.downloader.Downloader
import net.xzos.upgradeall.ui.viewmodels.view.FileItemView
import net.xzos.upgradeall.utils.runUiFun

class FileHubListAdapter : BaseQuickAdapter<FileItemView, BaseViewHolder>(R.layout.item_hub_file) {

    override fun convert(holder: BaseViewHolder, item: FileItemView) {
        holder.setText(R.id.iv_icon, item.name.first().toString())
        holder.setText(R.id.tv_app_name, item.name)
        setDownloaderNum(holder, item.downloader)
        item.downloader.setStatusChangedFun {
            try {
                setDownloaderNum(holder, item.downloader)
            } catch (ignore: Throwable) {
            }
        }
    }

    private fun setDownloaderNum(holder: BaseViewHolder, downloader: Downloader) {
        GlobalScope.launch {
            val downloadList = downloader.getDownloadList()
            val downloadingNum = downloadList.filter { it.status == Status.DOWNLOADING }.size
            val completedNum = downloadList.filter { it.status == Status.COMPLETED }.size
            val failedNum = downloadList.filter { it.status == Status.FAILED }.size
            runUiFun {
                holder.setText(R.id.tv_completed, completedNum)
                holder.setText(R.id.tv_downloading, downloadingNum)
                holder.setText(R.id.tv_failed, failedNum)
            }
        }
    }
}