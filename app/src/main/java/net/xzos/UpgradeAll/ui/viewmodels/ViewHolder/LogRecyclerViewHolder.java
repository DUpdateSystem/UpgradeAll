package net.xzos.UpgradeAll.ui.viewmodels.ViewHolder;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

public class LogRecyclerViewHolder extends RecyclerView.ViewHolder {
    public TextView logTextView;

    public LogRecyclerViewHolder(View view) {
        super(view);
        logTextView = view.findViewById(android.R.id.text1);
    }
}
