package net.xzos.upgradeall.ui.viewmodels.dialog

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.list_content.*
import net.xzos.upgradeall.R
import net.xzos.upgradeall.utils.MiscellaneousUtils

class DownloadListDialog private constructor(context: Context,
                                             private val fileNameList: List<String>,
                                             private val downloadFun: (position: Int, externalDownloader: Boolean) -> Unit
) : BottomSheetDialog(context) {
    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.list_content)
        super.onCreate(savedInstanceState)
        MiscellaneousUtils.showToast(R.string.long_click_to_use_external_downloader, Toast.LENGTH_LONG)
        placeholderLayout.visibility = View.VISIBLE
        // 下载文件
        list.setOnItemClickListener { _, _, position, _ ->
            downloadFun(position, false)
        }
        list.setOnItemLongClickListener { _, _, position, _ ->
            downloadFun(position, true)
            return@setOnItemLongClickListener true
        }
        if (fileNameList.isNotEmpty())
            list.adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, fileNameList)
        else
            emptyPlaceHolderTextView.visibility = View.VISIBLE
        placeholderLayout.visibility = View.GONE
    }

    companion object {
        fun show(context: Context, fileNameList: List<String>,
                 downloadFun: (position: Int, externalDownloader: Boolean) -> Unit
        ) {
            DownloadListDialog(context, fileNameList, downloadFun).show()
        }
    }
}
