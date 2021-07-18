package net.xzos.upgradeall.ui.log

import android.content.Context
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView
import net.xzos.upgradeall.utils.DimensionKtx.dp
import net.xzos.upgradeall.utils.file.FileUtil
import net.xzos.upgradeall.utils.getDrawableByAttr
import java.util.*


class LogItemAdapter(mLogList: LiveData<List<String>>, owner: LifecycleOwner) : RecyclerView.Adapter<LogRecyclerViewHolder>() {
    private val mLogMessages = ArrayList<String>()

    init {
        mLogList.value?.let {
            renewLogMessage(it)
        }
        mLogList.observe(owner, { logList ->
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
        val holder = LogRecyclerViewHolder(LogItemView(parent.context))
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

    class LogItemView(context: Context) : AppCompatTextView(context) {
        init {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(24.dp, 2.dp, 24.dp, 2.dp)
            isClickable = true
            background = context.getDrawableByAttr(android.R.attr.selectableItemBackground)
        }
    }
}
