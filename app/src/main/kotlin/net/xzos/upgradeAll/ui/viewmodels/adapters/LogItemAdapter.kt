package net.xzos.upgradeAll.ui.viewmodels.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeAll.server.log.LogUtil
import net.xzos.upgradeAll.ui.viewmodels.view.holder.LogRecyclerViewHolder
import net.xzos.upgradeAll.utils.FileUtil
import org.apache.commons.text.StringEscapeUtils
import java.util.*

class LogItemAdapter(mLogList: LiveData<LiveData<List<String>>>, owner: LifecycleOwner) : RecyclerView.Adapter<LogRecyclerViewHolder>() {
    private val mLogMessages = ArrayList<String>()

    init {
        mLogList.observe(owner, Observer { logListLiveData ->
            logListLiveData.observe(owner, Observer { stringList ->
                GlobalScope.launch {
                    LogUtil.mutex.withLock {
                        renewLogMessage(stringList)
                    }
                }
            })
        })
    }

    private fun renewLogMessage(stringList: List<String>) {
        if (mLogMessages != stringList) {
            val startChangeIndex: Int
            val index = mLogMessages.size - 1
            if (index == -1 || index > stringList.size || mLogMessages[index] != stringList[index]) {
                startChangeIndex = 0
                mLogMessages.clear()
                for (logMessage in stringList)
                    mLogMessages.add(StringEscapeUtils.unescapeJava(logMessage))
            } else {
                startChangeIndex = index + 1
                for (i in index + 1 until stringList.size)
                    mLogMessages.add(StringEscapeUtils.unescapeJava(stringList[i]))
            }
            notifyItemRangeChanged(startChangeIndex, mLogMessages.size)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogRecyclerViewHolder {
        val holder = LogRecyclerViewHolder(
                LayoutInflater.from(parent.context).inflate(android.R.layout.simple_expandable_list_item_1, parent, false))
        val logTextView = holder.logTextView
        logTextView.setOnClickListener {
            val context = logTextView.context
            FileUtil.clipStringToClipboard(logTextView.text, context)
        }
        return holder
    }

    override fun onBindViewHolder(holder: LogRecyclerViewHolder, position: Int) {
        val logMessage = mLogMessages[position]
        holder.logTextView.text = logMessage
    }

    override fun getItemCount(): Int {
        return mLogMessages.size
    }
}
