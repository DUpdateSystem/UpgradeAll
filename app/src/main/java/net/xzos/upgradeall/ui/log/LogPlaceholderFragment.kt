package net.xzos.upgradeall.ui.log

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.databinding.FragmentLogBinding


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
        binding.logListRecyclerView.also {
            it.layoutManager = GridLayoutManager(mContext, 1)
            it.adapter = LogItemAdapter(logPageViewModel.logList, this@LogPlaceholderFragment)
            it.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }
    }

    companion object {

        internal fun newInstance(logObjectTag: ObjectTag): LogPlaceholderFragment {
            return LogPlaceholderFragment(logObjectTag)
        }
    }
}