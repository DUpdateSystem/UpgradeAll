package net.xzos.upgradeall.ui.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import net.xzos.upgradeall.core.log.ObjectTag
import net.xzos.upgradeall.databinding.FragmentLogBinding
import net.xzos.upgradeall.ui.viewmodels.adapters.LogItemAdapter
import net.xzos.upgradeall.ui.viewmodels.viewmodel.LogPageViewModel


class LogPlaceholderFragment(
        private val bundleLogObjectTag: ObjectTag
) : Fragment() {

    private lateinit var binding: FragmentLogBinding
    private lateinit var mContext: Context
    private lateinit var logPageViewModel: LogPageViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentLogBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = requireContext()
        logPageViewModel = ViewModelProvider(this).get(LogPageViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        logPageViewModel.setLogObjectTag(bundleLogObjectTag)
        renewLogList()
    }

    private fun renewLogList() {
        val layoutManager = GridLayoutManager(mContext, 1)
        binding.logListRecyclerView.layoutManager = layoutManager
        val adapter = LogItemAdapter(logPageViewModel.logList, this@LogPlaceholderFragment)
        binding.logListRecyclerView.adapter = adapter
    }

    companion object {

        internal fun newInstance(logObjectTag: ObjectTag): LogPlaceholderFragment {
            return LogPlaceholderFragment(logObjectTag)
        }
    }
}