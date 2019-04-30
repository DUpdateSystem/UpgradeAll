package net.xzos.UpgradeAll;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;


public class UpgradeItemCardAdapter extends RecyclerView.Adapter<UpgradeItemCardAdapter.ViewHolder> {

    private Context mContext;


    private List<UpgradeItemCard> mFruitList;

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView name;
        TextView version;
        TextView url;
        TextView api;

        ViewHolder(View view) {
            super(view);
            cardView = (CardView) view;
            name = view.findViewById(R.id.nameTextView);
            version = view.findViewById(R.id.versionTextView);
            url = view.findViewById(R.id.urlTextView);
            api = view.findViewById(R.id.apiTextView);
        }
    }

    UpgradeItemCardAdapter(List<UpgradeItemCard> fruitList) {
        mFruitList = fruitList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (mContext == null) {
            mContext = parent.getContext();
        }
        View view = LayoutInflater.from(mContext).inflate(R.layout.upgrade_item_card_view, parent, false);
        final ViewHolder holder = new ViewHolder(view);
        holder.cardView.setOnClickListener(v -> {
            Intent intent = new Intent(mContext, UpgradeItemSettingActivity.class);
            mContext.startActivity(intent);
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UpgradeItemCard upgradeItemCard = mFruitList.get(position);
        holder.name.setText(upgradeItemCard.getName());
        holder.version.setText(upgradeItemCard.getVersion());
        holder.api.setText(upgradeItemCard.getApi());
        holder.url.setText(upgradeItemCard.getUrl());
    }

    @Override
    public int getItemCount() {
        return mFruitList.size();
    }

}

