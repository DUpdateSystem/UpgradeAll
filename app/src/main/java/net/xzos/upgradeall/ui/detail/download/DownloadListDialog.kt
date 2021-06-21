package net.xzos.upgradeall.ui.detail.download

import android.app.Dialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.Toast
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.module.Hub
import net.xzos.upgradeall.core.module.app.version_item.Asset
import net.xzos.upgradeall.ui.base.listdialog.ListDialog
import net.xzos.upgradeall.ui.detail.AppDetailViewModel
import net.xzos.upgradeall.utils.MiscellaneousUtils
import net.xzos.upgradeall.utils.UxUtils

class DownloadListDialog(
    fileAssetList: List<Asset>,
    appDetailViewModel: AppDetailViewModel
) : ListDialog(
        R.string.dialog_title_select_download_item,
        DownloadListAdapter(getItemViewList(fileAssetList), appDetailViewModel)
) {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        MiscellaneousUtils.showToast(R.string.long_click_to_use_external_downloader, Toast.LENGTH_LONG)
        return super.onCreateDialog(savedInstanceState)
    }

    companion object {
        private fun getItemViewList(fileAssetList: List<Asset>): List<DownloadItem> {
            return mutableListOf<DownloadItem>().apply {
                val hubColorMap = mutableMapOf<Hub, ColorStateList>()
                fileAssetList.forEach {
                    for (fileAsset in it.fileAssetList) {
                        val hub = it.hub
                        val hubColor = hubColorMap[hub]
                                ?: ColorStateList.valueOf(UxUtils.getRandomColor()).apply {
                                    hubColorMap[hub] = this
                                }
                        add(DownloadItem(fileAsset.name, it.hub.name, hubColor, fileAsset))
                    }
                }
            }
        }
    }
}