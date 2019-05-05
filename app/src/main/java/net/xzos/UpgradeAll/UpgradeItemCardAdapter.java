package net.xzos.UpgradeAll;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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


public class UpgradeItemCardAdapter extends RecyclerView.Adapter<UpgradeItemCardAdapter.ViewHolder> {

    private static final String TAG = "UpgradeItemCardAdapter";
    private List<upgradeCard> mUpgradeList;

    UpgradeItemCardAdapter(List<upgradeCard> upgradeList) {
        mUpgradeList = upgradeList;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        SwipeMenuLayout cardView;
        TextView name;
        TextView version;
        TextView url;
        TextView api;
        CardView del_button;
        RecyclerView upgradeItemCardList;

        ViewHolder(View view) {
            super(view);
            cardView = (SwipeMenuLayout) view;
            name = view.findViewById(R.id.nameTextView);
            version = view.findViewById(R.id.versionTextView);
            url = view.findViewById(R.id.urlTextView);
            api = view.findViewById(R.id.apiTextView);
            del_button = view.findViewById(R.id.del_button);
            upgradeItemCardList = view.findViewById(R.id.item_list_view);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview_upgrade, parent, false));
    }

    @SuppressLint("ShowToast")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        upgradeCard upgradeCard = mUpgradeList.get(position);
        holder.name.setText(upgradeCard.getName());
        holder.version.setText(upgradeCard.getVersion());
        holder.api.setText(upgradeCard.getApi());
        holder.url.setText(upgradeCard.getUrl());
        // 启动下载
        holder.version.setOnClickListener(v -> {
            JSONObject latestDownloadUrl = MyApplication.getUpdater().getLatestDownloadUrl(upgradeCard.getDatabaseId());
            List<String> itemList = new ArrayList<>();
            Iterator<String> sIterator = latestDownloadUrl.keys();
            while (sIterator.hasNext()) {
                String key = sIterator.next();
                itemList.add(key);
            }
            String[] itemStringArray = new String[itemList.size()];
            for (int i = 0; i < itemList.size(); i++) {
                itemStringArray[i] = itemList.get(i);
            }
            // 获取文件列表

            AlertDialog.Builder builder = new AlertDialog.Builder(holder.version.getContext());
            builder.setItems(itemStringArray, (dialog, which) -> {
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
            Toast.makeText(MyApplication.getContext(), String.format("%s已删除", api_url), Toast.LENGTH_LONG);
            Intent intent = new Intent(holder.del_button.getContext(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            MyApplication.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return mUpgradeList.size();
    }

}

