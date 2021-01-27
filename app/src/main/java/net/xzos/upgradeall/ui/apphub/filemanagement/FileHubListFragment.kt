package net.xzos.upgradeall.ui.apphub.filemanagement

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import net.xzos.upgradeall.databinding.FragmentHubListBinding
import net.xzos.upgradeall.ui.apphub.HubListFragment
import net.xzos.upgradeall.ui.apphub.adapter.FileHubListAdapter
import net.xzos.upgradeall.ui.viewmodels.view.FileItemView
import net.xzos.upgradeall.ui.viewmodels.viewmodel.FileHubViewModel

class FileHubListFragment : HubListFragment<FileItemView>() {

    private lateinit var binding: FragmentHubListBinding
    override val adapter = FileHubListAdapter()
    override val viewModel by activityViewModels<FileHubViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentHubListBinding.inflate(inflater)
        initView()
        return binding.root
    }

    private fun initView() {
        binding.apply {
            rvList.apply {
                adapter = this@FileHubListFragment.adapter
            }
        }
    }
}