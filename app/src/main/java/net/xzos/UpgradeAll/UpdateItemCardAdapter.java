package net.xzos.UpgradeAll;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
        CardView del_button;
        RecyclerView updateItemCardList;

        ViewHolder(View view) {
            super(view);
            cardView = (SwipeMenuLayout) view;
            name = view.findViewById(R.id.nameTextView);
            url = view.findViewById(R.id.urlTextView);
            api = view.findViewById(R.id.apiTextView);
            versionCheckingBar = view.findViewById(R.id.versionCheckingBar);
            versionCheckButton = view.findViewById(R.id.versionCheckTextButton);
            del_button = view.findViewById(R.id.del_button);
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
        holder.name.setText(updateCard.getName());
        holder.api.setText(updateCard.getApi());
        holder.url.setText(updateCard.getUrl());
        refreshUpdater(true, holder, updateCard);
        holder.versionCheckButton.setOnClickListener(v -> {
            // 单击展开 Release 详情页
            JSONObject latestDownloadUrl = MyApplication.getUpdater().getLatestDownloadUrl(updateCard.getDatabaseId());
            List<String> itemList = new ArrayList<>();
            Iterator<String> sIterator = latestDownloadUrl.keys();
            while (sIterator.hasNext()) {
                String key = sIterator.next();
                itemList.add(key);
            }
            holder.versionCheckButton.setOnLongClickListener(v1 -> {
                // 长按强制检查版本
                refreshUpdater(false, holder, updateCard);
                return true;
            });
            String[] itemStringArray = itemList.toArray(new String[0]);
            // 获取文件列表

            AlertDialog.Builder builder = new AlertDialog.Builder(holder.versionCheckingBar.getContext());
            builder.setItems(itemStringArray, (dialog, which) ->

            {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                String url = null;
                try {
                    url = latestDownloadUrl.getString(itemList.get(which));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                intent.setData(Uri.parse(url));
                intent = Intent.createChooser(intent, "请选择浏览器");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                MyApplication.getContext().startActivity(intent);
            });
            builder.show();
        });
        // 打开指向Url
        holder.url.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(holder.url.getText().toString()));
            intent = Intent.createChooser(intent, "请选择浏览器");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            MyApplication.getContext().startActivity(intent);
        });
        // 删除按钮
        holder.del_button.setOnClickListener(v -> {
            // 删除数据库
            String api_url = GithubApi.getApiUrl(holder.url.getText().toString())[0];
            LitePal.deleteAll(RepoDatabase.class, "api_url = ?", api_url);
            // 删除指定数据库
            Intent intent = new Intent(holder.del_button.getContext(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            MyApplication.getContext().startActivity(intent);
            // 重新跳转刷新界面
            // TODO: 需要优化刷新方法
        });
    }

    private void refreshUpdater(boolean isAuto, ViewHolder holder, UpdateCard updateCard) {
        if (!isAuto) {
            Toast.makeText(holder.versionCheckButton.getContext(), String.format("检查 %s 的更新", holder.name.getText().toString()),
                    Toast.LENGTH_SHORT).show();
        }
        holder.versionCheckingBar.setVisibility(View.VISIBLE);
        new Thread(() -> {
            int databaseId = updateCard.getDatabaseId();
            if (isAuto) {
                MyApplication.getUpdater().autoRefresh(databaseId);
            } else {
                MyApplication.getUpdater().refresh(databaseId);
            }
            // 刷新数据库
            new Handler(Looper.getMainLooper()).post(() -> {
                holder.versionCheckingBar.setVisibility(View.INVISIBLE);
                Updater updater = MyApplication.getUpdater();
                if (updater.isLatest(databaseId)) {
                    holder.versionCheckButton.setImageResource(R.drawable.ic_check_latest);
                } else {
                    holder.versionCheckButton.setImageResource(R.drawable.ic_check_needupdate);
                }
            });
        }).start();
    }

    @Override
    public int getItemCount() {
        return mUpdateList.size();
    }

}

