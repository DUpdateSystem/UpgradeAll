package net.xzos.UpgradeAll;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;


public class UpgradeItemCardViewAdapter extends RecyclerView.Adapter<UpgradeItemCardViewAdapter.ViewHolder> {

    private Context mContext;


    private List<UpgradeItemCardView> mFruitList;

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView name;
        TextView version;
        TextView url;
        TextView api;

        public ViewHolder(View view) {
            super(view);
            cardView = (CardView) view;
            name = view.findViewById(R.id.nameTextView);
            version = view.findViewById(R.id.versionTextView);
            url = view.findViewById(R.id.urlTextView);
            api = view.findViewById(R.id.apiTextView);
        }
    }

    public UpgradeItemCardViewAdapter(List<UpgradeItemCardView> fruitList) {
        mFruitList = fruitList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (mContext == null) {
            mContext = parent.getContext();
        }
        View view = LayoutInflater.from(mContext).inflate(R.layout.upgrade_item_card_view, parent, false);
        final ViewHolder holder = new ViewHolder(view);
        holder.cardView.setOnClickListener(v -> {
            int position = holder.getAdapterPosition();
            UpgradeItemCardView upgradeItemCardView = mFruitList.get(position);
            Intent intent = new Intent(mContext, RepoSettingActivity.class);
            mContext.startActivity(intent);
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        UpgradeItemCardView upgradeItemCardView = mFruitList.get(position);
        holder.name.setText(upgradeItemCardView.getName());
        holder.version.setText(upgradeItemCardView.getVersion());
        holder.api.setText(upgradeItemCardView.getApi());
        holder.url.setText(upgradeItemCardView.getUrl());
    }

    @Override
    public int getItemCount() {
        return mFruitList.size();
    }

}

