package net.xzos.UpgradeAll.ui.viewmodels.view.holder;

import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import net.xzos.UpgradeAll.R;

public class CardViewRecyclerViewHolder extends RecyclerView.ViewHolder {
    public TextView name;
    public TextView descTextView;
    public TextView api;
    public TextView endTextView;
    public CardView itemCardView;
    public ProgressBar versionCheckingBar;
    public ImageView versionCheckButton;

    public CardViewRecyclerViewHolder(View view) {
        super(view);
        name = view.findViewById(R.id.nameTextView);
        descTextView = view.findViewById(R.id.descTextView);
        api = view.findViewById(R.id.apiTextView);
        itemCardView = view.findViewById(R.id.item_card_view);
        versionCheckingBar = view.findViewById(R.id.statusChangingBar);
        versionCheckButton = view.findViewById(R.id.statusCheckButton);
        endTextView = view.findViewById(R.id.end_text_view);
    }
}
