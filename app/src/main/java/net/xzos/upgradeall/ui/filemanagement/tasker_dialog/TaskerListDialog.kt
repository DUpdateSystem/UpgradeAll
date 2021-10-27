package net.xzos.upgradeall.ui.filemanagement.tasker_dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import net.xzos.upgradeall.core.downloader.filetasker.FileTasker
import net.xzos.upgradeall.databinding.DialogFileTaskerBinding
import net.xzos.upgradeall.ui.base.listdialog.ListDialogPart
import net.xzos.upgradeall.ui.filemanagement.tasker_dialog.list.TaskerListAdapter
import net.xzos.upgradeall.ui.filemanagement.tasker_dialog.list.getTaskerItem


class TaskerListDialog private constructor(private val fileTasker: FileTasker) :
    BottomSheetDialogFragment(), ListDialogPart {

    override val sAdapter = TaskerListAdapter(emptyList())

    private lateinit var binding: DialogFileTaskerBinding

    private val viewModel: FileTaskerViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogFileTaskerBinding.inflate(inflater)
        viewModel.setFileTasker(fileTasker)
        initView(binding)
        return binding.root
    }

    private fun renewList() {
        renewListView(binding.listLayout)
    }

    private fun initView(binding: DialogFileTaskerBinding) {
        viewModel.downloadList.observe(this) { list ->
            sAdapter.setDataList(list.map { it.getTaskerItem() })
        }

        viewModel.tagList.observe(this) { list ->
            val chipGroup = binding.cgTag
            chipGroup.removeAllViewsInLayout()
            list.forEach {
                val typeChip = (layoutInflater.inflate(
                    android.R.layout.simple_list_item_1,
                    chipGroup,
                    false
                ) as Chip).apply {
                    text = it
                }
                chipGroup.addView(
                    typeChip,
                    -1,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                )
            }
        }

        binding.viewmodel = viewModel
        renewList()
    }

    companion object {
        private const val TAG = "TaskerListDialog"
        fun newInstance(activity: AppCompatActivity, fileTasker: FileTasker) {
            val taskerListDialog = TaskerListDialog(fileTasker)
            taskerListDialog.show(activity.supportFragmentManager, TAG)
        }
    }
}