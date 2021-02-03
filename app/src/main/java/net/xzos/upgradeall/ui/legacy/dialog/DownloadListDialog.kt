package net.xzos.upgradeall.ui.legacy.dialog

import android.app.Dialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import net.xzos.upgradeall.R
import net.xzos.upgradeall.databinding.ListContentBinding
import net.xzos.upgradeall.utils.MiscellaneousUtils

class DownloadListDialog private constructor(private val fileNameList: List<String>,
                                             private val downloadFun: (position: Int, externalDownloader: Boolean) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val contentBinding = ListContentBinding.inflate(layoutInflater)
        MiscellaneousUtils.showToast(R.string.long_click_to_use_external_downloader, Toast.LENGTH_LONG)

        contentBinding.apply {
            list.setOnItemClickListener { _, _, position, _ ->
                downloadFun(position, false)
            }
            list.setOnItemLongClickListener { _, _, position, _ ->
                downloadFun(position, true)
                return@setOnItemLongClickListener true
            }
            if (fileNameList.isNotEmpty()) {
                list.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, fileNameList)
            } else {
                vfContainer.displayedChild = 1
            }
        }

        return AlertDialog.Builder(requireContext())
                .setView(contentBinding.root)
                .setTitle(R.string.dialog_title_select_download_item)
                .create()
    }

    companion object {
        fun show(activity: AppCompatActivity, fileNameList: List<String>,
                 downloadFun: (position: Int, externalDownloader: Boolean) -> Unit
        ) {
            DownloadListDialog(fileNameList, downloadFun).run { show(activity.supportFragmentManager, tag) }
        }
    }
}
