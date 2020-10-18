package net.xzos.upgradeall.ui.viewmodels.view.holder

import android.util.TypedValue
import android.view.View
import android.widget.TextView

import androidx.recyclerview.widget.RecyclerView


class LogRecyclerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    var logTextView: TextView = view.findViewById<TextView>(android.R.id.text1).also {
        it.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10F)
    }
}
