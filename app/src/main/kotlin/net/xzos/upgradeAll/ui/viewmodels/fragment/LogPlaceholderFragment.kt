package net.xzos.upgradeAll.ui.viewmodels.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.android.synthetic.main.fragment_log.*
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.ui.viewmodels.adapters.LogItemAdapter
import net.xzos.upgradeAll.ui.viewmodels.viewmodel.LogPageViewModel

class LogPlaceholderFragment : Fragment() {

    private lateinit var mContext: Context
    private lateinit var logPageViewModel: LogPageViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_log, container, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = context!!
        logPageViewModel = ViewModelProvider(this).get(LogPageViewModel::class.java)
        if (arguments != null) {
            arguments?.getStringArray(ARG_SECTION_NUMBER)?.run {
                val logObjectTag = Pair(this[0], this[1])
                logPageViewModel.setLogObjectTag(logObjectTag)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        renewLogList()
    }

    private fun renewLogList() {
        val layoutManager = GridLayoutManager(mContext, 1)
        logListRecyclerView.layoutManager = layoutManager
        val adapter = LogItemAdapter(logPageViewModel.logList, this@LogPlaceholderFragment)
        logListRecyclerView.adapter = adapter
    }

    companion object {

        private const val ARG_SECTION_NUMBER = "LogObjectTag"

        internal fun newInstance(logObjectTag: Pair<String, String>): LogPlaceholderFragment {
            val fragment = LogPlaceholderFragment()
            val bundle = Bundle()
            bundle.putStringArray(ARG_SECTION_NUMBER, logObjectTag.toList().toTypedArray())
            fragment.arguments = bundle
            return fragment
        }
    }
}