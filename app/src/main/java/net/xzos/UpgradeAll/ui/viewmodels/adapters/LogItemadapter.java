package net.xzos.UpgradeAll.ui.viewmodels.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.xzos.UpgradeAll.R;
import net.xzos.UpgradeAll.ui.viewmodels.LogItemView;

import java.util.List;

public class LogItemadapter extends RecyclerView.Adapter<LogItemadapter.ViewHolder> {

    private List<LogItemView> mLogItemViewList;

    public LogItemadapter(List<LogItemView> updateList) {
        mLogItemViewList = updateList;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView logMessage;
        RecyclerView updateItemCardList;

        ViewHolder(View view) {
            super(view);
            logMessage = view.findViewById(R.id.log_message_textView);
            updateItemCardList = view.findViewById(R.id.update_item_recycler_view);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LogItemView logItemView = mLogItemViewList.get(position);
        holder.logMessage.setText(logItemView.getLogMessage());
    }

    @Override
    public int getItemCount() {
        return mLogItemViewList.size();
    }
}
