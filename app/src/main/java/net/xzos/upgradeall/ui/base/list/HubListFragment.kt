package net.xzos.upgradeall.ui.base.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import net.xzos.upgradeall.databinding.FragmentHubListBinding
import net.xzos.upgradeall.ui.base.recycleview.ListContainerViewModel

abstract class HubListFragment<T : ListItemView> : Fragment() {

    private lateinit var binding: FragmentHubListBinding
    protected abstract val adapter: BaseQuickAdapter<T, BaseViewHolder>
    protected abstract val viewModel: ListContainerViewModel<T>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentHubListBinding.inflate(inflater)
        initView()
        return binding.root
    }

    private fun initView() {
        binding.rvList.adapter = this.adapter

        viewModel.getList().observe(viewLifecycleOwner, {
            adapter.setList(it)
        })
    }
}
