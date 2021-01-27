package net.xzos.upgradeall.ui.apphub

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import net.xzos.upgradeall.databinding.FragmentHubListBinding
import net.xzos.upgradeall.ui.viewmodels.view.ListView
import net.xzos.upgradeall.ui.viewmodels.viewmodel.ListContainerViewModel

abstract class HubListFragment<T: ListView> : Fragment() {

    private lateinit var binding: FragmentHubListBinding
    protected abstract val adapter: BaseQuickAdapter<T, BaseViewHolder>
    protected abstract val viewModel: ListContainerViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentHubListBinding.inflate(inflater)
        initView()
        return binding.root
    }

    private fun initView() {
        binding.rvList.adapter = this.adapter

        viewModel.getList().observe(viewLifecycleOwner, {
            @Suppress("UNCHECKED_CAST")
            adapter.setList(it as Collection<T>)
        })
    }
}
