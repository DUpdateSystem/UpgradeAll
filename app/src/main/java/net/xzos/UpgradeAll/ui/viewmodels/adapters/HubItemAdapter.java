package net.xzos.UpgradeAll.ui.viewmodels.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import net.xzos.UpgradeAll.R;
import net.xzos.UpgradeAll.activity.HubLocalAddActivity;
import net.xzos.UpgradeAll.database.HubDatabase;
import net.xzos.UpgradeAll.ui.viewmodels.ItemCardView;

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
        TextView endTextView;
        CardView itemCardView;
        ProgressBar versionCheckingBar;
        ImageView versionCheckButton;
        RecyclerView updateItemCardList;

        ViewHolder(View view) {
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

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ItemCardView itemCardView = mItemCardViewList.get(position);
        // 底栏设置
        if (itemCardView.getName() == null && itemCardView.getApi() == null && itemCardView.getDesc() == null) {
            holder.itemCardView.setVisibility(View.GONE);
            holder.endTextView.setVisibility(View.VISIBLE);
        }
        int databaseId = itemCardView.getDatabaseId();
        holder.name.setText(itemCardView.getName());
        holder.api.setText(itemCardView.getApi());
        holder.descTextView.setText(itemCardView.getDesc());
        holder.descTextView.setEnabled(false);

        // 长按菜单
        holder.itemCardView.setOnLongClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(holder.itemCardView.getContext(), v);
            MenuInflater menuInflater = popupMenu.getMenuInflater();
            menuInflater.inflate(R.menu.menu_long_click_cardview_item, popupMenu.getMenu());
            popupMenu.show();
            //设置item的点击事件
            popupMenu.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    // 修改按钮
                    case R.id.setting_button:
                        Intent intent = new Intent(holder.itemCardView.getContext(), HubLocalAddActivity.class);
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
    }

    @Override
    public int getItemCount() {
        return mItemCardViewList.size();
    }
}