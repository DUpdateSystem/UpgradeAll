package net.xzos.UpgradeAll.ui.viewmodels.adapters;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import net.xzos.UpgradeAll.R;
import net.xzos.UpgradeAll.gson.HubConfig;
import net.xzos.UpgradeAll.server.hub.CloudHub;
import net.xzos.UpgradeAll.server.hub.HubManager;
import net.xzos.UpgradeAll.ui.viewmodels.ItemCardView;

import java.util.List;


public class CloudHubItemAdapter extends RecyclerView.Adapter<CardViewRecyclerViewHolder> {

    private static final String TAG = "CloudHubItemAdapter";

    private List<ItemCardView> mItemCardViewList;

    private CloudHub mCloudHub;

    public CloudHubItemAdapter(List<ItemCardView> updateList, CloudHub cloudHub) {
        mItemCardViewList = updateList;
        mCloudHub = cloudHub;
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
            PopupMenu popupMenu = new PopupMenu(holder.itemCardView.getContext(), v);
            MenuInflater menuInflater = popupMenu.getMenuInflater();
            menuInflater.inflate(R.menu.menu_long_click_cardview_item_cloud_hub, popupMenu.getMenu());
            popupMenu.show();
            //设置item的点击事件
            popupMenu.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    // 下载
                    case R.id.download:
                        Toast.makeText(holder.itemCardView.getContext(), "开始下载", Toast.LENGTH_LONG).show();
                        // 下载数据
                        new Thread(() -> {
                            HubConfig cloudHubConfigGson = mCloudHub.getHubConfig(itemCardView.getExtraData().getConfigFileName());
                            boolean addHubSuccess;
                            if (cloudHubConfigGson != null) {
                                String cloudHubConfigJS = mCloudHub.getHubConfigJS(cloudHubConfigGson.getWebCrawler().getFilePath());
                                if (cloudHubConfigJS != null)
                                    addHubSuccess = HubManager.addHubDatabase(0, cloudHubConfigGson, cloudHubConfigJS);
                                else {
                                    addHubSuccess = false;
                                    new Handler(Looper.getMainLooper()).post(() -> {
                                        Toast.makeText(holder.itemCardView.getContext(), "获取 JS 代码失败", Toast.LENGTH_LONG).show();
                                    });
                                }
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