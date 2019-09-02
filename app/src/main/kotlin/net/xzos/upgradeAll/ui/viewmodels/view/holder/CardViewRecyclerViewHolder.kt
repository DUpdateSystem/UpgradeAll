package net.xzos.upgradeAll.ui.viewmodels.view.holder

import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView

import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

import net.xzos.upgradeAll.R

class CardViewRecyclerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    var name: TextView
    var descTextView: TextView
    var api: TextView
    var endTextView: TextView
    var itemCardView: CardView
    var versionCheckingBar: ProgressBar
    var versionCheckButton: ImageView

    init {
        name = view.findViewById(R.id.nameTextView)
        descTextView = view.findViewById(R.id.descTextView)
        api = view.findViewById(R.id.apiTextView)
        itemCardView = view.findViewById(R.id.item_card_view)
        versionCheckingBar = view.findViewById(R.id.statusChangingBar)
        versionCheckButton = view.findViewById(R.id.statusCheckButton)
        endTextView = view.findViewById(R.id.end_text_view)
    }
}
