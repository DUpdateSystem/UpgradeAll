package net.xzos.UpgradeAll.ui.viewmodels.adapters;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import net.xzos.UpgradeAll.R;
import net.xzos.UpgradeAll.activity.HubLocalAddActivity;
import net.xzos.UpgradeAll.gson.HubConfig;
import net.xzos.UpgradeAll.server.cloud.CloudHub;
import net.xzos.UpgradeAll.ui.viewmodels.ItemCardView;

import java.util.List;


public class CloudHubItemAdapter extends RecyclerView.Adapter<CloudHubItemAdapter.ViewHolder> {

    private List<ItemCardView> mItemCardViewList;
    
    private CloudHub mCloudHub;

    public CloudHubItemAdapter(List<ItemCardView> updateList, CloudHub cloudHub) {
        mItemCardViewList = updateList;
        mCloudHub = cloudHub;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView descTextView;
        TextView api;
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
        holder.name.setText(itemCardView.getName());
        holder.api.setText(itemCardView.getApi());
        holder.descTextView.setText(itemCardView.getDesc());
        holder.descTextView.setEnabled(false);

        // 长按菜单
        holder.itemCardView.setOnLongClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(holder.itemCardView.getContext(), v);
            MenuInflater menuInflater = popupMenu.getMenuInflater();
            menuInflater.inflate(R.menu.menu_long_click_cardview_item_cloud_hub, popupMenu.getMenu());
            popupMenu.show();
            //设置item的点击事件
            popupMenu.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    // 下载
                    case R.id.download:
                        // 下载数据
                        new Thread(() -> {
                            HubConfig cloudHubConfigGson = mCloudHub.getHubConfig(holder.descTextView.getText().toString());
                            boolean addHubSuccess;
                            if (cloudHubConfigGson != null) {
                                addHubSuccess = HubLocalAddActivity.addHubDatabase(0, cloudHubConfigGson);
                            } else {
                                addHubSuccess = false;
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    Toast.makeText(holder.itemCardView.getContext(), "数据下载失败", Toast.LENGTH_LONG).show();
                                });
                            }
                            new Handler(Looper.getMainLooper()).post(() -> {
                                if (addHubSuccess) {
                                    Toast.makeText(holder.itemCardView.getContext(), "数据添加成功", Toast.LENGTH_LONG).show();
                                } else
                                    Toast.makeText(holder.itemCardView.getContext(), "什么？数据库添加失败！", Toast.LENGTH_LONG).show();
                            });
                        }).start();
                        // 添加数据库
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

