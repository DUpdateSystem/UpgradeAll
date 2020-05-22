package net.xzos.upgradeall.ui.viewmodels.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import net.xzos.upgradeall.ui.viewmodels.view.holder.LogRecyclerViewHolder
import net.xzos.upgradeall.utils.FileUtil
import java.util.*


class LogItemAdapter(mLogList: LiveData<List<String>>, owner: LifecycleOwner) : RecyclerView.Adapter<LogRecyclerViewHolder>() {
    private val mLogMessages = ArrayList<String>()

    init {
        mLogList.observe(owner, Observer { logList ->
            renewLogMessage(logList)
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
                    mLogMessages.add(logMessage)
            } else {
                startChangeIndex = index + 1
                for (i in index + 1 until stringList.size)
                    mLogMessages.add(stringList[i])
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
