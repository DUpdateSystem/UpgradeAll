package net.xzos.UpgradeAll.viewmodels.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import net.xzos.UpgradeAll.activity.HubSettingActivity;
import net.xzos.UpgradeAll.R;
import net.xzos.UpgradeAll.database.HubDatabase;
import net.xzos.UpgradeAll.viewmodels.ItemCardView;

import org.litepal.LitePal;

import java.util.List;


public class HubItemAdapter extends RecyclerView.Adapter<HubItemAdapter.ViewHolder> {

    private List<ItemCardView> mItemCardViewList;

    public HubItemAdapter(List<ItemCardView> updateList) {
        mItemCardViewList = updateList;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView descTextView;
        TextView api;
        ProgressBar versionCheckingBar;
        ImageView versionCheckButton;
        CardView delButton;
        CardView settingButton;
        RecyclerView updateItemCardList;

        ViewHolder(View view) {
            super(view);
            name = view.findViewById(R.id.nameTextView);
            descTextView = view.findViewById(R.id.descTextView);
            api = view.findViewById(R.id.apiTextView);
            versionCheckingBar = view.findViewById(R.id.statusChangingBar);
            versionCheckButton = view.findViewById(R.id.statusCheckButton);
            delButton = view.findViewById(R.id.delButton);
            settingButton = view.findViewById(R.id.settingButton);
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
        ItemCardView itemCardView = mItemCardViewList.get(position);
        int databaseId = itemCardView.getDatabaseId();
        holder.name.setText(itemCardView.getName());
        holder.api.setText(itemCardView.getApi());
        holder.descTextView.setText(itemCardView.getDesc());
        holder.descTextView.setEnabled(false);

        // 修改按钮
        holder.settingButton.setOnClickListener(v -> {
            Intent intent = new Intent(holder.settingButton.getContext(), HubSettingActivity.class);
            intent.putExtra("database_id", databaseId);
            holder.settingButton.getContext().startActivity(intent);
        });
        // 删除按钮
        holder.delButton.setOnClickListener(v -> {
            // 删除数据库
            LitePal.delete(HubDatabase.class, databaseId);
            // 删除指定数据库
            mItemCardViewList.remove(holder.getAdapterPosition());
            notifyItemRemoved(holder.getAdapterPosition());
            notifyItemRangeChanged(holder.getAdapterPosition(), mItemCardViewList.size());
            // 删除 CardView
        });
    }

    @Override
    public int getItemCount() {
        return mItemCardViewList.size();
    }
}