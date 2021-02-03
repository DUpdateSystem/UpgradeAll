package net.xzos.upgradeall.ui.applist.base

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.android.material.card.MaterialCardView
import net.xzos.upgradeall.R

class AppHubListAdapter : BaseQuickAdapter<AppListItemView, BaseViewHolder>(R.layout.item_hub_app) {

    override fun convert(holder: BaseViewHolder, item: AppListItemView) {
        holder.setText(R.id.iv_icon, item.name.first().toString())
        holder.setText(R.id.tv_app_name, item.name)
        holder.setText(R.id.tv_version_name,item.version )
        holder.getView<MaterialCardView>(R.id.item_view).setOnClickListener {

        }
    }
}