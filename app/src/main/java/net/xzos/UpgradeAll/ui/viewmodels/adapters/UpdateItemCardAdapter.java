package net.xzos.UpgradeAll.ui.viewmodels.adapters;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import net.xzos.UpgradeAll.R;
import net.xzos.UpgradeAll.activity.UpdaterSettingActivity;
import net.xzos.UpgradeAll.application.MyApplication;
import net.xzos.UpgradeAll.database.RepoDatabase;
import net.xzos.UpgradeAll.server.updater.Updater;
import net.xzos.UpgradeAll.ui.viewmodels.ItemCardView;

import org.json.JSONException;
import org.json.JSONObject;
import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class UpdateItemCardAdapter extends RecyclerView.Adapter<CardViewRecyclerViewHolder> {

    private List<ItemCardView> mItemCardViewList;

    private static final Updater updater = MyApplication.getUpdater();

    public UpdateItemCardAdapter(List<ItemCardView> updateList) {
        mItemCardViewList = updateList;
    }

    @NonNull
    @Override
    public CardViewRecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new CardViewRecyclerViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewRecyclerViewHolder holder, int position) {
        ItemCardView itemCardView = mItemCardViewList.get(position);
        // 底栏设置
        if (itemCardView.getName() == null && itemCardView.getApi() == null && itemCardView.getDesc() == null) {
            holder.itemCardView.setVisibility(View.GONE);
            holder.endTextView.setVisibility(View.VISIBLE);
        }
        int databaseId = itemCardView.getExtraData().getDatabaseId();
        holder.name.setText(itemCardView.getName());
        holder.api.setText(itemCardView.getApi());
        holder.descTextView.setText(itemCardView.getDesc());
        refreshUpdater(true, databaseId, holder);

        // 单击展开 Release 详情页
        holder.itemCardView.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(holder.versionCheckingBar.getContext());
            final AlertDialog dialog = builder.setView(R.layout.dialog_version).create();
            dialog.show();

            Window dialogWindow = dialog.getWindow();

            if (dialogWindow != null) {
                TextView cloudReleaseTextView = dialogWindow.findViewById(R.id.cloudReleaseTextView);
                TextView localReleaseTextView = dialogWindow.findViewById(R.id.localReleaseTextView);
                // 显示本地版本号
                String latestVersion = updater.getLatestVersion(databaseId);
                if (latestVersion != null)
                    cloudReleaseTextView.setText(latestVersion);
                else
                    cloudReleaseTextView.setText("获取失败");
                String installedVersion = updater.getInstalledVersion(databaseId);
                if (installedVersion != null)
                    localReleaseTextView.setText(installedVersion);
                else
                    localReleaseTextView.setText("获取失败");
            }

            // 获取云端文件
            new Thread(() -> {
                // 刷新数据库
                JSONObject latestDownloadUrl = updater.getLatestDownloadUrl(databaseId);
                if (latestDownloadUrl == null) latestDownloadUrl = new JSONObject();
                final JSONObject latestDownloadUrlCopy = latestDownloadUrl;
                new Handler(Looper.getMainLooper()).post(() -> {
                    List<String> itemList = new ArrayList<>();
                    Iterator<String> sIterator = latestDownloadUrlCopy.keys();
                    while (sIterator.hasNext()) {
                        String key = sIterator.next();
                        itemList.add(key);
                    }

                    // 无Release文件，不显示网络文件列表
                    if (itemList.size() == 0) {
                        dialog.getWindow().findViewById(R.id.releaseFileListLinearLayout).setVisibility(View.GONE);
                    }

                    // 构建文件列表
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            dialog.getContext(), android.R.layout.simple_list_item_1, itemList);
                    ListView cloudReleaseList = dialog.getWindow().findViewById(R.id.cloudReleaseList);
                    // 设置文件列表点击事件
                    cloudReleaseList.setOnItemClickListener((parent, view, position1, id) -> {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        String url = null;
                        try {
                            url = latestDownloadUrlCopy.getString(itemList.get(position1));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if (url != null && !url.startsWith("http://") && !url.startsWith("https://"))
                            url = "http://" + url;
                        intent.setData(Uri.parse(url));
                        Intent chooser = Intent.createChooser(intent, "请选择浏览器");
                        if (intent.resolveActivity(dialog.getContext().getPackageManager()) != null) {
                            dialog.getContext().startActivity(chooser);
                        }
                    });
                    cloudReleaseList.setAdapter(adapter);
                    dialog.getWindow().findViewById(R.id.fileListProgressBar).setVisibility(View.INVISIBLE);  // 隐藏等待提醒条
                });
            }).start();
        });

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
                        Intent intent = new Intent(holder.itemCardView.getContext(), UpdaterSettingActivity.class);
                        intent.putExtra("database_id", databaseId);
                        holder.itemCardView.getContext().startActivity(intent);
                        break;
                    // 删除按钮
                    case R.id.del_button:
                        // 删除数据库
                        LitePal.delete(RepoDatabase.class, databaseId);
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

        // 长按强制检查版本
        holder.versionCheckButton.setOnLongClickListener(v1 -> {
            refreshUpdater(false, databaseId, holder);
            return true;
        });

        // 打开指向Url
        holder.descTextView.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(holder.descTextView.getText().toString()));
            Intent chooser = Intent.createChooser(intent, "请选择浏览器");
            if (intent.resolveActivity(holder.descTextView.getContext().getPackageManager()) != null) {
                holder.descTextView.getContext().startActivity(chooser);
            }
        });
    }

    private void refreshUpdater(boolean isAuto, int databaseId, CardViewRecyclerViewHolder holder) {
        if (databaseId == 0) return;
        if (!isAuto) {
            Toast.makeText(holder.versionCheckButton.getContext(), String.format("检查 %s 的更新", holder.name.getText().toString()),
                    Toast.LENGTH_SHORT).show();
        }
        holder.versionCheckButton.setVisibility(View.INVISIBLE);
        holder.versionCheckingBar.setVisibility(View.VISIBLE);
        new Thread(() -> {
            // 刷新数据库
            if (isAuto) {
                updater.autoRefresh(databaseId);
            } else {
                updater.renewUpdateItem(databaseId);
                updater.refresh(databaseId);
            }
            new Handler(Looper.getMainLooper()).post(() -> {
                holder.versionCheckButton.setVisibility(View.VISIBLE);
                holder.versionCheckingBar.setVisibility(View.INVISIBLE);
                //检查是否取得云端版本号
                if (updater.getLatestVersion(databaseId) != null) {
                    // 检查是否获取本地版本号
                    if (updater.getInstalledVersion(databaseId) != null) {
                        // 检查本地版本
                        if (updater.isLatest(databaseId)) {
                            holder.versionCheckButton.setImageResource(R.drawable.ic_check_latest);
                        } else {
                            holder.versionCheckButton.setImageResource(R.drawable.ic_check_needupdate);
                        }
                    } else {
                        holder.versionCheckButton.setImageResource(R.drawable.ic_local_error);
                    }
                } else {
                    holder.versionCheckButton.setImageResource(R.drawable.ic_404);
                }
            });
        }).start();
    }

    @Override
    public int getItemCount() {
        return mItemCardViewList.size();
    }

}

