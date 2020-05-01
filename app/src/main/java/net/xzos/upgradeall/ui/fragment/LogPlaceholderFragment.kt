package net.xzos.upgradeall.ui.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.android.synthetic.main.fragment_log.*
import net.xzos.upgradeall.core.data.json.nongson.ObjectTag
import net.xzos.upgradeall.R
import net.xzos.upgradeall.ui.viewmodels.adapters.LogItemAdapter
import net.xzos.upgradeall.ui.viewmodels.viewmodel.LogPageViewModel


class LogPlaceholderFragment : Fragment() {

    private lateinit var mContext: Context
    private lateinit var logPageViewModel: LogPageViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_log, container, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = requireContext()
        logPageViewModel = ViewModelProvider(this).get(LogPageViewModel::class.java)
        val logObjectTag = bundleLogObjectTag
        if (logObjectTag != null) {
            logPageViewModel.setLogObjectTag(logObjectTag)
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

        private var bundleLogObjectTag: ObjectTag? = null
            get() {
                return field.also {
                    bundleLogObjectTag = null
                }
            }

        internal fun newInstance(logObjectTag: ObjectTag): LogPlaceholderFragment {
            bundleLogObjectTag = logObjectTag
            return LogPlaceholderFragment()
        }
    }
}
