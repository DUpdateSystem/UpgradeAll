package net.xzos.upgradeall.ui.detail.download

import net.xzos.upgradeall.databinding.ItemDownloadDialogBinding
import net.xzos.upgradeall.ui.base.listdialog.DialogListAdapter
import net.xzos.upgradeall.ui.detail.AppDetailViewModel

class DownloadListAdapter(dataList: List<DownloadItem>, appDetailViewModel: AppDetailViewModel) :
    DialogListAdapter<DownloadItem, DownloadItemHandler, DownloadHolder>(
        dataList.toMutableList(), DownloadItemHandler(appDetailViewModel),
        fun(layoutInflater, viewGroup) =
            DownloadHolder(ItemDownloadDialogBinding.inflate(layoutInflater, viewGroup, false))
    )