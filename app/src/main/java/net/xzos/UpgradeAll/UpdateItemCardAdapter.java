package net.xzos.UpgradeAll;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.yanzhenjie.recyclerview.SwipeMenuLayout;

import org.json.JSONException;
import org.json.JSONObject;
import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class UpdateItemCardAdapter extends RecyclerView.Adapter<UpdateItemCardAdapter.ViewHolder> {

    private List<UpdateCard> mUpdateList;

    final private Updater updater = MyApplication.getUpdater();

    UpdateItemCardAdapter(List<UpdateCard> updateList) {
        mUpdateList = updateList;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        SwipeMenuLayout cardView;
        TextView name;
        TextView url;
        TextView api;
        ProgressBar versionCheckingBar;
        ImageView versionCheckButton;
        CardView delButton;
        CardView settingButton;
        RecyclerView updateItemCardList;

        ViewHolder(View view) {
            super(view);
            cardView = (SwipeMenuLayout) view;
            name = view.findViewById(R.id.nameTextView);
            url = view.findViewById(R.id.urlTextView);
            api = view.findViewById(R.id.apiTextView);
            versionCheckingBar = view.findViewById(R.id.versionCheckingBar);
            versionCheckButton = view.findViewById(R.id.versionCheckTextButton);
            delButton = view.findViewById(R.id.del_button);
            settingButton = view.findViewById(R.id.setting_button);
            updateItemCardList = view.findViewById(R.id.item_list_view);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview_update, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UpdateCard updateCard = mUpdateList.get(position);
        int databaseId = updateCard.getDatabaseId();
        holder.name.setText(updateCard.getName());
        holder.api.setText(updateCard.getApi());
        holder.url.setText(updateCard.getUrl());
        refreshUpdater(true, databaseId, holder);

        // 单击展开 Release 详情页
        holder.versionCheckButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(holder.versionCheckingBar.getContext());
            final AlertDialog dialog = builder.setView(R.layout.dialog_version).create();
            dialog.show();

            // 显示本地版本号
            TextView cloudReleaseTextView = dialog.getWindow().findViewById(R.id.cloud_release_text_view);
            String latestVersion = updater.getLatestVersion(databaseId);
            if (latestVersion != null)
                cloudReleaseTextView.setText(latestVersion);
            else
                cloudReleaseTextView.setText("获取失败");
            TextView localReleaseTextView = dialog.getWindow().findViewById(R.id.local_release_text_view);
            String installedVersion = updater.getInstalledVersion(databaseId);
            if (installedVersion != null)
                localReleaseTextView.setText(installedVersion);
            else
                localReleaseTextView.setText("获取失败");

            // 获取云端文件
            JSONObject latestDownloadUrl = updater.getLatestDownloadUrl(databaseId);
            List<String> itemList = new ArrayList<>();
            Iterator<String> sIterator = latestDownloadUrl.keys();
            while (sIterator.hasNext()) {
                String key = sIterator.next();
                itemList.add(key);
            }

            // 无Release文件，不显示网络文件列表
            if (itemList.size() == 0) {
                dialog.getWindow().findViewById(R.id.releaseTextView).setVisibility(View.INVISIBLE);
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
                    url = latestDownloadUrl.getString(itemList.get(position1));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                intent.setData(Uri.parse(url));
                intent = Intent.createChooser(intent, "请选择浏览器");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                MyApplication.getContext().startActivity(intent);
            });
            cloudReleaseList.setAdapter(adapter);
        });

        // 长按强制检查版本
        holder.versionCheckButton.setOnLongClickListener(v1 -> {
            refreshUpdater(false, databaseId, holder);
            return true;
        });

        // 打开指向Url
        holder.url.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(holder.url.getText().toString()));
            intent = Intent.createChooser(intent, "请选择浏览器");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            MyApplication.getContext().startActivity(intent);
        });
        // 修改按钮
        holder.settingButton.setOnClickListener(v -> {
            Intent intent = new Intent(holder.settingButton.getContext(), UpdateItemSettingActivity.class);
            intent.putExtra("database_id", databaseId);
            holder.settingButton.getContext().startActivity(intent);
        });
        // 删除按钮
        holder.delButton.setOnClickListener(v -> {
            // 删除数据库
            LitePal.delete(RepoDatabase.class, databaseId);
            // 删除指定数据库
            Intent intent = new Intent(holder.delButton.getContext(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            holder.delButton.getContext().startActivity(intent);
            // 重新跳转刷新界面
            // TODO: 需要优化刷新方法
        });
    }

    private void refreshUpdater(boolean isAuto, int databaseId, ViewHolder holder) {
        if (!isAuto) {
            Toast.makeText(holder.versionCheckButton.getContext(), String.format("检查 %s 的更新", holder.name.getText().toString()),
                    Toast.LENGTH_SHORT).show();
        }
        holder.versionCheckButton.setVisibility(View.INVISIBLE);
        holder.versionCheckingBar.setVisibility(View.VISIBLE);
        new Thread(() -> {
            if (isAuto) {
                updater.autoRefresh(databaseId);
            } else {
                updater.refresh(databaseId);
            }
            // 刷新数据库
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
        return mUpdateList.size();
    }

}

