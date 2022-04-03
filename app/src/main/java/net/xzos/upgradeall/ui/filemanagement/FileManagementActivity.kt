package net.xzos.upgradeall.ui.filemanagement

import androidx.activity.viewModels
import net.xzos.upgradeall.ui.base.list.HubListActivity

class FileManagementActivity : HubListActivity<FileItemView, FileItemView, FileHubListViewHolder>() {
    override val viewModel by viewModels<FileHubViewModel>()
    override val adapter by lazy { FileHubListAdapter(this) }
}