package net.xzos.upgradeall.ui.viewmodels.view.holder

import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.cardview_content.view.*
import kotlinx.android.synthetic.main.cardview_item.view.*

class CardViewRecyclerViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
    val appIconImageView: ImageView = view.appIconImageView
    val nameTextView: TextView = view.nameTextView
    val versioningTextView: TextView = view.versioningTextView
    val descLinearLayout: LinearLayout = view.descLinearLayout
    val typeTextView: TextView = view.typeTextView
    val hubNameTextView: TextView = view.hubNameTextView
    val versionCheckingBar: ProgressBar = view.statusChangingBar
    val versionCheckButton: ImageView = view.statusCheckImageView
    val itemCardView: CardView = view.itemCardView
    val appPlaceholderImageView: ImageView = view.AppPlaceholderImageView
}
