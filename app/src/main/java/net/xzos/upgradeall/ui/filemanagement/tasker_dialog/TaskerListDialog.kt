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


class TaskerListDialog private constructor(
        context: Context,
        private val fileTasker: FileTasker,
        private val item: TaskerDialogItem = TaskerDialogItem(fileTasker),
        private val downloadList: List<Download>
) : BottomSheetDialog(context), ListDialogPart {

    override val sAdapter = TaskerListAdapter(emptyList())

    private lateinit var binding: DialogFileTaskerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = DialogFileTaskerBinding.inflate(layoutInflater)
        initView(binding)
        setContentView(binding.root)
        super.onCreate(savedInstanceState)
    }

    private fun renewList() {
        sAdapter.setDataList(downloadList.map { getItemView(it) })
        renewListView(binding.listLayout)
    }

    private fun initView(binding: DialogFileTaskerBinding) {
        val handler = TaskerDialogHandler(fileTasker, this@TaskerListDialog)
        binding.item = item
        binding.handler = handler
        renewList()
    }

    companion object {
        private fun getItemView(downloader: Download): TaskerItem {
            val file = File(downloader.file)
            return TaskerItem(file.name, file.path, downloader.progress)
        }

        fun newInstance(context: Context, fileTasker: FileTasker) {
            GlobalScope.launch {
                val item = TaskerDialogItem(fileTasker)
                val downloadList = fileTasker.downloader?.getDownloadList() ?: emptyList()
                runUiFun {
                    TaskerListDialog(context, fileTasker, item, downloadList).show()
                }
            }
        }
    }
}