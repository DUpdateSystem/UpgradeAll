package net.xzos.upgradeall.ui.apphub.adapter

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.android.material.card.MaterialCardView
import com.tonyodev.fetch2.Status
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.downloader.Downloader
import net.xzos.upgradeall.ui.viewmodels.view.AppListItemView
import net.xzos.upgradeall.utils.runUiFun

class AppHubListAdapter : BaseQuickAdapter<AppListItemView, BaseViewHolder>(R.layout.item_hub_app) {

    override fun convert(holder: BaseViewHolder, item: AppListItemView) {
        holder.setText(R.id.iv_icon, item.name.first().toString())
        holder.setText(R.id.tv_app_name, item.name)
        holder.setText(R.id.tv_version_name,item.version )
        holder.getView<MaterialCardView>(R.id.item_view).setOnClickListener {

        }
    }
}