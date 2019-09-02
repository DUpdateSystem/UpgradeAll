package net.xzos.upgradeAll.ui.viewmodels.adapters

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import net.xzos.upgradeAll.ui.viewmodels.view.holder.LogRecyclerViewHolder
import org.apache.commons.text.StringEscapeUtils
import java.util.*

class LogItemAdapter(mLogList: LiveData<LiveData<List<String>>>, owner: LifecycleOwner) : RecyclerView.Adapter<LogRecyclerViewHolder>() {
    private val mLogMessages = ArrayList<String>()

    init {
        mLogList.observe(owner, Observer { logListLiveData ->
            logListLiveData.observe(owner, Observer { stringList ->
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
            })
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogRecyclerViewHolder {
        val holder = LogRecyclerViewHolder(
                LayoutInflater.from(parent.context).inflate(android.R.layout.simple_expandable_list_item_1, parent, false))
        val logTextView = holder.logTextView
        logTextView.setOnClickListener {
            val context = logTextView.context
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val mClipData = ClipData.newPlainText("Label", logTextView.text)
            cm.setPrimaryClip(mClipData)
            Toast.makeText(context, "已复制到粘贴板", Toast.LENGTH_SHORT).show()
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
