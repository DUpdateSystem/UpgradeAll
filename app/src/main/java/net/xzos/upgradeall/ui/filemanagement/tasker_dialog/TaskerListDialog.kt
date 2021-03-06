package net.xzos.upgradeall.ui.filemanagement.tasker_dialog

import android.content.Context
import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.tonyodev.fetch2.Download
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeall.core.filetasker.FileTasker
import net.xzos.upgradeall.databinding.DialogFileTaskerBinding
import net.xzos.upgradeall.ui.base.listdialog.ListDialogPart
import net.xzos.upgradeall.ui.filemanagement.tasker_dialog.list.TaskerItem
import net.xzos.upgradeall.ui.filemanagement.tasker_dialog.list.TaskerListAdapter
import net.xzos.upgradeall.utils.runUiFun
import java.io.File


class TaskerListDialog(
        context: Context,
        private val fileTasker: FileTasker
) : BottomSheetDialog(context), ListDialogPart {

    override val sAdapter = TaskerListAdapter(emptyList())

    private lateinit var binding: DialogFileTaskerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = DialogFileTaskerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        super.onCreate(savedInstanceState)
        initView(binding)
    }

    private suspend fun renewList() {
        val downloadList = fileTasker.downloader?.getDownloadList()?.map { getItemView(it) }
                ?: emptyList()
        sAdapter.setDataList(downloadList)
        runUiFun {
            renewListView(binding.listLayout)
        }
    }

    private fun initView(binding: DialogFileTaskerBinding) {
        GlobalScope.launch {
            val item = TaskerDialogItem(fileTasker)
            val handler = TaskerDialogHandler(fileTasker, this@TaskerListDialog)
            binding.item = item
            binding.handler = handler
            renewList()
        }
    }

    companion object {
        private fun getItemView(downloader: Download): TaskerItem {
            val file = File(downloader.file)
            return TaskerItem(file.name, file.path, downloader.progress)
        }
    }
}