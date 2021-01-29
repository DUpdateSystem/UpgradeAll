package net.xzos.upgradeall.ui.apphub.filemanagement

import androidx.activity.viewModels
import net.xzos.upgradeall.ui.apphub.HubListActivity
import net.xzos.upgradeall.ui.apphub.adapter.FileHubListAdapter
import net.xzos.upgradeall.ui.viewmodels.view.FileItemView
import net.xzos.upgradeall.ui.viewmodels.view.holder.FileHubListViewHolder
import net.xzos.upgradeall.ui.viewmodels.viewmodel.FileHubViewModel

class FileManagementActivity : HubListActivity<FileItemView, FileHubListViewHolder>() {
    override val viewModel by viewModels<FileHubViewModel>()
    override val adapter = FileHubListAdapter()
}