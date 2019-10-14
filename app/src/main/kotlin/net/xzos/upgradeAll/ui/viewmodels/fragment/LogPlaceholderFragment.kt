package net.xzos.upgradeAll.ui.viewmodels.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.ui.viewmodels.adapters.LogItemAdapter
import net.xzos.upgradeAll.ui.viewmodels.viewmodel.LogPageViewModel

class LogPlaceholderFragment : Fragment() {

    private lateinit var mContext: Context
    private lateinit var logPageViewModel: LogPageViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = context!!
        logPageViewModel = ViewModelProvider(this).get(LogPageViewModel::class.java)
        if (arguments != null) {
            val logObjectTag = arguments?.getStringArray(ARG_SECTION_NUMBER)
            if (logObjectTag != null)
                logPageViewModel.setLogObjectTag(logObjectTag)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_log, container, false)
        val logListRecyclerView = root.findViewById<RecyclerView>(R.id.logListRecyclerView)
        val layoutManager = GridLayoutManager(mContext, 1)
        logListRecyclerView.layoutManager = layoutManager
        val adapter = LogItemAdapter(logPageViewModel.logList, this)
        logListRecyclerView.adapter = adapter
        return root
    }

    companion object {

        private const val ARG_SECTION_NUMBER = "LogObjectTag"

        internal fun newInstance(logObjectTag: Array<String>): LogPlaceholderFragment {
            val fragment = LogPlaceholderFragment()
            val bundle = Bundle()
            bundle.putStringArray(ARG_SECTION_NUMBER, logObjectTag)
            fragment.arguments = bundle
            return fragment
        }
    }
}