package net.xzos.upgradeall.ui.filemanagement.tasker_dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.downloader.filedownloader.item.TaskWrapper
import net.xzos.upgradeall.databinding.DialogFileTaskerBinding
import net.xzos.upgradeall.ui.base.list.HubListPart
import net.xzos.upgradeall.ui.filemanagement.tasker_dialog.list.TaskerItem
import net.xzos.upgradeall.ui.filemanagement.tasker_dialog.list.TaskerItemHolder
import net.xzos.upgradeall.ui.filemanagement.tasker_dialog.list.TaskerListAdapter
import net.xzos.upgradeall.wrapper.download.DownloadTasker


class TaskerListDialog private constructor(private val fileTasker: DownloadTasker) :
    BottomSheetDialogFragment(), HubListPart<TaskWrapper, TaskerItem, TaskerItemHolder> {

    override val adapter = TaskerListAdapter()

    private lateinit var rootBinding: DialogFileTaskerBinding

    override lateinit var rvList: RecyclerView
    override var srlContainer: SwipeRefreshLayout? = null

    private val rootViewModel: FileTaskerViewModel by viewModels()
    override val viewModel: FileTaskerListViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        rootBinding = DialogFileTaskerBinding.inflate(inflater)
        rootViewModel.setFileTasker(fileTasker)
        rootViewModel.renew()
        initView(rootBinding)
        initListView(rootBinding.rvList)
        return rootBinding.root
    }

    private fun initListView(rvList: RecyclerView, srlContainer: SwipeRefreshLayout? = null) {
        this.rvList = rvList
        this.srlContainer = srlContainer
        initViewData(this)
    }

    private fun initView(binding: DialogFileTaskerBinding) {
        viewModel.getDownload = { rootViewModel.downloadList.value ?: emptyList() }
        viewModel.loadData(rootViewModel.downloadList.value ?: emptyList())
        rootViewModel.downloadList.observe(this) { list ->
            viewModel.loadData(list)
        }

        rootViewModel.tagList.observe(this) { list ->
            val chipGroup = binding.cgTag
            chipGroup.removeAllViewsInLayout()
            list.forEach {
                val typeChip = (layoutInflater.inflate(
                    R.layout.single_chip_layout,
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

        binding.viewmodel = rootViewModel
    }

    companion object {
        private const val TAG = "TaskerListDialog"
        fun newInstance(activity: AppCompatActivity, fileTasker: DownloadTasker) {
            val taskerListDialog = TaskerListDialog(fileTasker)
            taskerListDialog.show(activity.supportFragmentManager, TAG)
        }
    }
}