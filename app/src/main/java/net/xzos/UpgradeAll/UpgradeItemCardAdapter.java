package net.xzos.UpgradeAll;

import android.annotation.SuppressLint;
import android.content.Intent;
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

import org.litepal.LitePal;

import java.util.List;


public class UpgradeItemCardAdapter extends RecyclerView.Adapter<UpgradeItemCardAdapter.ViewHolder> {

    private List<UpgradeItemCard> mFruitList;

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

    UpgradeItemCardAdapter(List<UpgradeItemCard> fruitList) {
        mFruitList = fruitList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(R.layout.upgrade_item_card_view, parent, false));
    }

    @SuppressLint("ShowToast")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UpgradeItemCard upgradeItemCard = mFruitList.get(position);
        holder.name.setText(upgradeItemCard.getName());
        holder.version.setText(upgradeItemCard.getVersion());
        holder.api.setText(upgradeItemCard.getApi());
        holder.url.setText(upgradeItemCard.getUrl());
        holder.del_button.setOnClickListener(v -> {
            // 删除数据库
            String api_url = GithubApi.getApiUrl(holder.url.getText().toString())[0];
            LitePal.deleteAll(RepoDatabase.class, "api_url = ?", api_url);
            Toast.makeText(MyApplication.getContext(), String.format("%s已删除", api_url), Toast.LENGTH_LONG);
            int item = this.getItemCount();
            Intent intent = new Intent(MyApplication.getContext(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            MyApplication.getContext().startActivity(intent);
            Log.d("123", "onBindViewHolder:  " + item);
        });
    }

    @Override
    public int getItemCount() {
        return mFruitList.size();
    }

}

