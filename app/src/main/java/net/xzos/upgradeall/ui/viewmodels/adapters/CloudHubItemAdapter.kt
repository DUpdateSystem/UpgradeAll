package net.xzos.upgradeall.ui.viewmodels.adapters

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.PopupMenu
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.data_manager.CloudConfigGetter
import net.xzos.upgradeall.core.data_manager.HubDatabaseManager
import net.xzos.upgradeall.ui.viewmodels.view.CloudConfigListItemView
import net.xzos.upgradeall.ui.viewmodels.view.holder.CardViewRecyclerViewHolder
import net.xzos.upgradeall.utils.IconPalette


class CloudHubItemAdapter(override var mItemCardViewList: List<CloudConfigListItemView>)
    : CloudItemAdapter(mItemCardViewList) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewRecyclerViewHolder {
        val holder = super.onCreateViewHolder(parent, viewType)
        // 长按菜单
        holder.itemCardView.setOnLongClickListener { v ->
            val position = holder.adapterPosition
            val itemCardView = mItemCardViewList[position]
            val popupMenu = PopupMenu(holder.itemCardView.context, v)
            val menuInflater = popupMenu.menuInflater
            menuInflater.inflate(R.menu.menu_long_click_cardview_item_cloud_hub, popupMenu.menu)
            popupMenu.show()
            //设置item的点击事件
            popupMenu.setOnMenuItemClickListener { item ->
                if (item.itemId == R.id.download) {
                    setDownloadStatus(holder, true)
                    GlobalScope.launch {
                        val addHubStatus = CloudConfigGetter.downloadCloudHubConfig(itemCardView.uuid)  // 下载数据
                        withContext(Dispatchers.Main) {
                            setDownloadStatus(holder, false)
                            if (addHubStatus == 3) {
                                holder.versionCheckButton.visibility = View.VISIBLE
                            }
                        }
                    }
                }
                return@setOnMenuItemClickListener true
            }
            return@setOnLongClickListener true
        }
        return holder
    }

    override fun onBindViewHolder(holder: CardViewRecyclerViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        val itemCardView = mItemCardViewList[position]
        if (itemCardView.uuid != null) {
            // 加载仓库信息
            holder.nameTextView.text = itemCardView.name
            itemCardView.type?.let {
                holder.typeTextView.setText(it)
            }
            holder.hubNameTextView.visibility = View.GONE
            checkHubConfigLocalStatus(holder, itemCardView.uuid)
            GlobalScope.launch { loadCloudHubIcon(holder.appIconImageView, itemCardView.uuid) }
        }
    }

    private fun loadCloudHubIcon(iconImageView: ImageView, hubUuid: String?) {
        if (hubUuid != null) {
            val cloudHubConfigGson = CloudConfigGetter.getHubCloudConfig(hubUuid)
            val hubIconUrl = cloudHubConfigGson?.info?.hubIconUrl
            IconPalette.loadHubIconView(iconImageView, hubIconUrl)
        }
    }

    private fun checkHubConfigLocalStatus(holder: CardViewRecyclerViewHolder, hubUuid: String?) {
        val versionCheckButton = holder.versionCheckButton
        if (HubDatabaseManager.exists(hubUuid)) {
            versionCheckButton.visibility = View.VISIBLE
            setDownloadStatus(holder, true)
            val hubConfigGson = CloudConfigGetter.getHubCloudConfig(hubUuid)
            HubDatabaseManager.getDatabase(uuid = hubUuid)?.hubConfig?.let {
                val localHubVersion = it.info.configVersion
                val cloudHubVersion = hubConfigGson?.info?.configVersion
                if (cloudHubVersion != null && cloudHubVersion > localHubVersion)
                    versionCheckButton.setImageResource(R.drawable.ic_check_needupdate)
                setDownloadStatus(holder, false)
            }
        } else
            versionCheckButton.visibility = View.GONE
    }
}
