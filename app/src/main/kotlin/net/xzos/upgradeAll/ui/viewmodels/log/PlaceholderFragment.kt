package net.xzos.upgradeAll.ui.viewmodels.log

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

/**
 * A placeholder fragment containing a simple view.
 */
class PlaceholderFragment : Fragment() {

    private lateinit var mContext: Context

    private lateinit var pageViewModel: PageViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = context!!
        pageViewModel = ViewModelProvider(this).get(PageViewModel::class.java)
        if (arguments != null) {
            val logObjectTag = arguments?.getStringArray(ARG_SECTION_NUMBER)
            if (logObjectTag != null)
                pageViewModel.setLogObjectTag(logObjectTag)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_log, container, false)
        val logListView = root.findViewById<RecyclerView>(R.id.log_list)
        val layoutManager = GridLayoutManager(mContext, 1)
        logListView.layoutManager = layoutManager
        val adapter = LogItemAdapter(pageViewModel.logList, this)
        logListView.adapter = adapter
        return root
    }

    companion object {

        private const val ARG_SECTION_NUMBER = "LogObjectTag"

        internal fun newInstance(logObjectTag: Array<String>): PlaceholderFragment {
            val fragment = PlaceholderFragment()
            val bundle = Bundle()
            bundle.putStringArray(ARG_SECTION_NUMBER, logObjectTag)
            fragment.arguments = bundle
            return fragment
        }
    }
}