package net.xzos.UpgradeAll.adapters;

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

import net.xzos.UpgradeAll.activity.MainActivity;
import net.xzos.UpgradeAll.activity.UpdateItemSettingActivity;
import net.xzos.UpgradeAll.R;
import net.xzos.UpgradeAll.Updater.Updater;
import net.xzos.UpgradeAll.data.MyApplication;
import net.xzos.UpgradeAll.database.RepoDatabase;
import net.xzos.UpgradeAll.viewmodels.Repo;

import org.json.JSONException;
import org.json.JSONObject;
import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class UpdateItemCardAdapter extends RecyclerView.Adapter<UpdateItemCardAdapter.ViewHolder> {

    private List<Repo> mUpdateList;

    final private Updater updater = MyApplication.getUpdater();

    public UpdateItemCardAdapter(List<Repo> updateList) {
        mUpdateList = updateList;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
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
            name = view.findViewById(R.id.nameTextView);
            url = view.findViewById(R.id.urlTextView);
            api = view.findViewById(R.id.apiTextView);
            versionCheckingBar = view.findViewById(R.id.versionCheckingBar);
            versionCheckButton = view.findViewById(R.id.versionCheckButton);
            delButton = view.findViewById(R.id.delButton);
            settingButton = view.findViewById(R.id.settingButton);
            updateItemCardList = view.findViewById(R.id.updateItemRecyclerView);
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
        Repo repo = mUpdateList.get(position);
        int databaseId = repo.getDatabaseId();
        holder.name.setText(repo.getName());
        holder.api.setText(repo.getApi());
        holder.url.setText(repo.getUrl());
        refreshUpdater(true, databaseId, holder);

        // 单击展开 Release 详情页
        holder.versionCheckButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(holder.versionCheckingBar.getContext());
            final AlertDialog dialog = builder.setView(R.layout.dialog_version).create();
            dialog.show();

            // 显示本地版本号
            TextView cloudReleaseTextView = dialog.getWindow().findViewById(R.id.cloudReleaseTextView);
            String latestVersion = updater.getLatestVersion(databaseId);
            if (latestVersion != null)
                cloudReleaseTextView.setText(latestVersion);
            else
                cloudReleaseTextView.setText("获取失败");
            TextView localReleaseTextView = dialog.getWindow().findViewById(R.id.localReleaseTextView);
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
            // 刷新数据库
            if (isAuto) {
                updater.autoRefresh(databaseId);
            } else {
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
        return mUpdateList.size();
    }

}

