package net.xzos.upgradeall.ui.detail

import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.FragmentManager
import net.xzos.upgradeall.ui.detail.download.DownloadListDialog
import net.xzos.upgradeall.utils.MiscellaneousUtils

class AppDetailHandler(
        private val viewModel: AppDetailViewModel,
        private val supportFragmentManager: FragmentManager,
) {
    fun clickDownload() {
        DownloadListDialog(viewModel.currentVersion?.assetList
                ?: return, viewModel).show(supportFragmentManager)
    }

    fun showMoreURL(urlList: Set<String>, view: View) {
        PopupMenu(view.context, view).apply {
            urlList.forEach {
                menu.add(it)
            }
            setOnMenuItemClickListener {
                openUrl(it.title.toString(), view)
                true
            }
            show()
        }
    }

    fun openUrl(url: String, view: View) {
        MiscellaneousUtils.accessByBrowser(url, view.context)
    }
}