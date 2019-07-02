package net.xzos.UpgradeAll.ui.viewmodels.adapters;

import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import net.xzos.UpgradeAll.R;

class CardViewRecyclerViewHolder extends RecyclerView.ViewHolder {
    TextView name;
    TextView descTextView;
    TextView api;
    TextView endTextView;
    CardView itemCardView;
    ProgressBar versionCheckingBar;
    ImageView versionCheckButton;
    RecyclerView updateItemCardList;

    CardViewRecyclerViewHolder(View view) {
        super(view);
        name = view.findViewById(R.id.nameTextView);
        descTextView = view.findViewById(R.id.descTextView);
        api = view.findViewById(R.id.apiTextView);
        itemCardView = view.findViewById(R.id.item_card_view);
        versionCheckingBar = view.findViewById(R.id.statusChangingBar);
        versionCheckButton = view.findViewById(R.id.statusCheckButton);
        updateItemCardList = view.findViewById(R.id.update_item_recycler_view);
        endTextView = view.findViewById(R.id.end_text_view);
    }
}
