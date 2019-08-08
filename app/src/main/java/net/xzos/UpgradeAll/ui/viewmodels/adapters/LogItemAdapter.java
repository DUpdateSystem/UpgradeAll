package net.xzos.UpgradeAll.ui.viewmodels.adapters;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.RecyclerView;

import net.xzos.UpgradeAll.ui.viewmodels.ViewHolder.LogRecyclerViewHolder;

import org.apache.commons.text.StringEscapeUtils;

import java.util.ArrayList;
import java.util.List;

public class LogItemAdapter extends RecyclerView.Adapter<LogRecyclerViewHolder> {
    private ArrayList<String> mLogMessages = new ArrayList<>();

    public LogItemAdapter(LiveData<LiveData<List<String>>> mLogList, LifecycleOwner owner) {
        mLogList.observe(owner, logListLiveData -> logListLiveData.observe(owner, stringList -> {
            if (!mLogMessages.equals(stringList)) {
                int startChangeIndex;
                int index = mLogMessages.size() - 1;
                if (index == -1 || index > stringList.size() || !mLogMessages.get(index).equals(stringList.get(index))) {
                    startChangeIndex = 0;
                    mLogMessages.clear();
                    for (String logMessage : stringList)
                        mLogMessages.add(StringEscapeUtils.unescapeJava(logMessage));
                } else {
                    startChangeIndex = index + 1;
                    for (int i = index + 1; i < stringList.size(); i++)
                        mLogMessages.add(StringEscapeUtils.unescapeJava(stringList.get(i)));
                }
                notifyItemRangeChanged(startChangeIndex, mLogMessages.size());
            }
        }));
    }

    @NonNull
    @Override
    public LogRecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final LogRecyclerViewHolder holder = new LogRecyclerViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_expandable_list_item_1, parent, false));
        final TextView logTextView = holder.logTextView;
        logTextView.setOnClickListener(view -> {
            Context context = logTextView.getContext();
            ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData mClipData = ClipData.newPlainText("Label", logTextView.getText());
            if (cm != null) cm.setPrimaryClip(mClipData);
            Toast.makeText(context, "已复制到粘贴板", Toast.LENGTH_SHORT).show();
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull LogRecyclerViewHolder holder, int position) {
        String logMessage = mLogMessages.get(position);
        holder.logTextView.setText(logMessage);
    }

    @Override
    public int getItemCount() {
        return mLogMessages.size();
    }
}
