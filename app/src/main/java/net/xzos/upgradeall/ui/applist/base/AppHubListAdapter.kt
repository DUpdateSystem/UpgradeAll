package net.xzos.upgradeall.ui.applist.base

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import net.xzos.upgradeall.R
import net.xzos.upgradeall.databinding.ItemHubAppBinding
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewAdapter
import net.xzos.upgradeall.utils.AppUtils

class AppHubListAdapter : RecyclerViewAdapter<AppListItemView, AppHubListViewHolder>() {

    override fun getViewHolder(layoutInflater: LayoutInflater, viewGroup: ViewGroup): AppHubListViewHolder {
        val binding = ItemHubAppBinding.inflate(layoutInflater, viewGroup, false)
        return AppHubListViewHolder(binding)
    }

    override fun onBindViewHolder(viewHolder: AppHubListViewHolder, position: Int) {
        super.onBindViewHolder(viewHolder, position)
        viewHolder.itemView.findViewById<TextView>(R.id.iv_icon).apply {
            val packageName = dataSet[position].app.appId["android_app_package"] ?: error("")
            if (AppUtils.isAppInstalled(packageName)) {
                setBackgroundDrawable(context.packageManager.getApplicationIcon(packageName))
            }
        }
    }
}