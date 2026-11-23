package net.xzos.upgradeall.ui.detail.download

import android.app.Dialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.androidutils.ToastUtil
import net.xzos.upgradeall.core.module.Hub
import net.xzos.upgradeall.core.module.app.version.AssetWrapper
import net.xzos.upgradeall.databinding.RecyclerlistContentBinding
import net.xzos.upgradeall.ui.detail.AppDetailViewModel
import net.xzos.upgradeall.utils.UxUtils

class DownloadListDialog : DialogFragment() {
    private var adapter: DownloadListAdapter? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val adapter = adapter ?: return super.onCreateDialog(savedInstanceState)
        val binding = RecyclerlistContentBinding.inflate(layoutInflater)
        binding.srlContainer.isEnabled = false
        binding.rvList.adapter = adapter
        binding.rvList.overScrollMode = RecyclerView.OVER_SCROLL_NEVER
        ToastUtil.showText(
            requireContext(),
            R.string.long_click_to_use_external_downloader,
            Toast.LENGTH_LONG
        )
        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_title_select_download_item)
            .setView(binding.root)
            .create()
    }

    override fun onStart() {
        super.onStart()
        // Do not restore a picker for an asset selection owned by the old activity.
        if (adapter == null) dismissAllowingStateLoss()
    }

    companion object {
        fun show(manager: FragmentManager, assets: List<AssetWrapper>, viewModel: AppDetailViewModel) {
            DownloadListDialog().apply {
                adapter = DownloadListAdapter(getItemViewList(assets), viewModel)
            }.show(manager, "DownloadListDialog")
        }
        private fun getItemViewList(fileAssetList: List<AssetWrapper>): List<DownloadItem> {
            return mutableListOf<DownloadItem>().apply {
                val hubColorMap = mutableMapOf<Hub, ColorStateList>()
                fileAssetList.forEach {
                    val hub = it.hub
                    val hubColor = hubColorMap[hub]
                        ?: ColorStateList.valueOf(UxUtils.getRandomColor()).apply {
                            hubColorMap[hub] = this
                        }
                    add(DownloadItem(it.asset.fileName, it.hub.name, hubColor, it))
                }
            }
        }
    }
}