package net.xzos.UpgradeAll.ui.viewmodels.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import net.xzos.UpgradeAll.R;
import net.xzos.UpgradeAll.activity.HubLocalActivity;
import net.xzos.UpgradeAll.database.HubDatabase;
import net.xzos.UpgradeAll.ui.viewmodels.ItemCardView;

import org.litepal.LitePal;

import java.util.List;


public class LocalHubItemAdapter extends RecyclerView.Adapter<CardViewRecyclerViewHolder> {

    private List<ItemCardView> mItemCardViewList;

    public LocalHubItemAdapter(List<ItemCardView> updateList) {
        mItemCardViewList = updateList;
    }

    @NonNull
    @Override
    public CardViewRecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final CardViewRecyclerViewHolder holder = new CardViewRecyclerViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview_item, parent, false));
        // 长按菜单
        holder.itemCardView.setOnLongClickListener(v -> {
            final int position = holder.getAdapterPosition();
            final ItemCardView itemCardView = mItemCardViewList.get(position);
            final int databaseId = itemCardView.getExtraData().getDatabaseId();
            PopupMenu popupMenu = new PopupMenu(holder.itemCardView.getContext(), v);
            MenuInflater menuInflater = popupMenu.getMenuInflater();
            menuInflater.inflate(R.menu.menu_long_click_cardview_item, popupMenu.getMenu());
            popupMenu.show();
            //设置item的点击事件
            popupMenu.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    // 修改按钮
                    case R.id.setting_button:
                        Intent intent = new Intent(holder.itemCardView.getContext(), HubLocalActivity.class);
                        intent.putExtra("database_id", databaseId);
                        holder.itemCardView.getContext().startActivity(intent);
                        break;
                    // 删除按钮
                    case R.id.del_button:
                        // 删除数据库
                        LitePal.delete(HubDatabase.class, databaseId);
                        // 删除指定数据库
                        mItemCardViewList.remove(holder.getAdapterPosition());
                        notifyItemRemoved(holder.getAdapterPosition());
                        notifyItemRangeChanged(holder.getAdapterPosition(), mItemCardViewList.size());
                        // 删除 CardView
                        break;
                }
                return true;
            });
            return true;
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewRecyclerViewHolder holder, int position) {
        ItemCardView itemCardView = mItemCardViewList.get(position);
        // 底栏设置
        if (itemCardView.getExtraData().isEmpty()) {
            holder.itemCardView.setVisibility(View.GONE);
            holder.endTextView.setVisibility(View.VISIBLE);
        } else {
            holder.itemCardView.setVisibility(View.VISIBLE);
            holder.endTextView.setVisibility(View.GONE);
            holder.name.setText(itemCardView.getName());
            holder.api.setText(itemCardView.getApi());
            holder.descTextView.setText(itemCardView.getDesc());
            holder.descTextView.setEnabled(false);
        }
    }

    @Override
    public int getItemCount() {
        return mItemCardViewList.size();
    }
}