package net.xzos.upgradeall.ui.filemanagement.tasker_dialog

import android.content.Context
import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.tonyodev.fetch2.Download
import net.xzos.upgradeall.databinding.DialogFileTaskerBinding
import net.xzos.upgradeall.ui.base.listdialog.DialogListAdapter
import net.xzos.upgradeall.ui.base.listdialog.ListDialogPart
import java.io.File


class TaskerListDialog(
        context: Context,
        dataList: List<Download>
) : BottomSheetDialog(context), ListDialogPart {

    override val adapter: DialogListAdapter<*, *, *> = TaskerListAdapter(dataList.map { getItemView(it) })
    override fun onCreate(savedInstanceState: Bundle?) {
        val contentBinding = DialogFileTaskerBinding.inflate(layoutInflater)
        initBinding(contentBinding.listLayout)
        setContentView(contentBinding.root)
        super.onCreate(savedInstanceState)
    }

    companion object {
        private fun getItemView(downloader: Download): TaskerItem {
            val file = File(downloader.file)
            return TaskerItem(file.name, file.path, downloader.progress)
        }
    }
}