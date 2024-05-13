package net.xzos.upgradeall.ui.discover

import android.view.LayoutInflater
import android.view.ViewGroup
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.databinding.ItemDiscoverAppBinding
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewAdapter
import net.xzos.upgradeall.ui.discover.DiscoverListItemView.Companion.getCloudAppItemCardView
import net.xzos.upgradeall.websdk.data.json.AppConfigGson

class DiscoveryAdapter(
    override val handler: DiscoverListItemHandler
) : RecyclerViewAdapter<AppConfigGson, DiscoverListItemView, DiscoverListItemHandler, DiscoverListViewHolder>(
    { getCloudAppItemCardView(it, MyApplication.context) }
) {

    override fun getViewHolder(
        layoutInflater: LayoutInflater,
        viewGroup: ViewGroup
    ): DiscoverListViewHolder {
        val binding = ItemDiscoverAppBinding.inflate(layoutInflater, viewGroup, false)
        return DiscoverListViewHolder(binding)
    }

    override fun getItemId(position: Int) = getAdapterData()[position].hashCode().toLong()
}