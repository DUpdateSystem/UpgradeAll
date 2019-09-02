package net.xzos.upgradeAll.ui.viewmodels.view.holder

import android.view.View
import android.widget.TextView

import androidx.recyclerview.widget.RecyclerView

class LogRecyclerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    var logTextView: TextView

    init {
        logTextView = view.findViewById(android.R.id.text1)
    }
}
