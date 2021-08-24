package net.xzos.upgradeall.ui.filemanagement

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeall.core.androidutils.runUiFun
import net.xzos.upgradeall.databinding.ItemHubFileTaskerBinding
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewHolder


class FileHubListViewHolder(private val binding: ItemHubFileTaskerBinding)
    : RecyclerViewHolder<FileItemView, FileHubListItemHandler, ItemHubFileTaskerBinding>(binding, binding) {

    override fun doBind(itemView: FileItemView) {
        binding.fileItem = itemView
    }

    override fun setHandler(handler: FileHubListItemHandler) {
        binding.hander = handler
    }

    override suspend fun loadExtraUi(itemView: FileItemView) {
        GlobalScope.launch {
            val completedNum = itemView.getCompletedNum()
            val downloadingNum = itemView.getDownloadingNum()
            val pausedNum = itemView.getPauseNum()
            val failedNum = itemView.getFailedNum()
            val downloadProgress = itemView.getDownloadProgress()
            runUiFun {
                binding.tvCompleted.text = completedNum
                binding.tvDownloading.text = downloadingNum
                binding.tvPaused.text = pausedNum
                binding.tvFailed.text = failedNum
                binding.pbDownload.progress = downloadProgress
            }
        }
    }
}
