package net.xzos.upgradeall.ui.apphub.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import androidx.core.content.ContextCompat
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.server_manager.AppManager
import net.xzos.upgradeall.ui.viewmodels.view.CloudConfigListItemView

class DiscoveryAdapter : BaseQuickAdapter<CloudConfigListItemView, BaseViewHolder>(R.layout.item_hub_app) {

    override fun convert(holder: BaseViewHolder, item: CloudConfigListItemView) {
        holder.setText(R.id.tv_app_name, item.name)
        holder.setText(R.id.tv_package_name, item.hubName)

        AppManager.getSingleApp(uuid = item.uuid)?.run {
            holder.itemView.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.material_green_200))
        } ?: run {
            holder.itemView.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
        }
    }
}