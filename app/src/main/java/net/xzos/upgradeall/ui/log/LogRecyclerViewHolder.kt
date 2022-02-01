package net.xzos.upgradeall.ui.log

import android.util.TypedValue
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class LogRecyclerViewHolder(view: LogItemAdapter.LogItemView) : RecyclerView.ViewHolder(view) {
    var logTextView: TextView = view.also {
        it.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10F)
    }
}
