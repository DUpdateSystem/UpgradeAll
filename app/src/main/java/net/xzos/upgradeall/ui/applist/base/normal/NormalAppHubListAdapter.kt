package net.xzos.upgradeall.ui.applist.base.normal

import android.view.LayoutInflater
import android.view.ViewGroup
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.databinding.ItemHubAppNormalBinding
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewAdapter

class NormalAppHubListAdapter(
    listContainerViewConvertFun: (App) -> NormalAppListItemView,
    override val handler: NormalAppHubListItemHandler = NormalAppHubListItemHandler()
) : RecyclerViewAdapter<App, NormalAppListItemView, NormalAppHubListItemHandler, NormalAppHubListViewHolder>(
    listContainerViewConvertFun
) {

    override fun getViewHolder(
        layoutInflater: LayoutInflater, viewGroup: ViewGroup
    ): NormalAppHubListViewHolder {
        val binding = ItemHubAppNormalBinding.inflate(layoutInflater, viewGroup, false)
        return NormalAppHubListViewHolder(binding)
    }
}