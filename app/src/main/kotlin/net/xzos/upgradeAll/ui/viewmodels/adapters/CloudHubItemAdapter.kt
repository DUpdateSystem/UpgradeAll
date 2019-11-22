package net.xzos.upgradeAll.ui.viewmodels.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.data.database.manager.CloudConfigGetter
import net.xzos.upgradeAll.data.database.manager.HubDatabaseManager
import net.xzos.upgradeAll.ui.viewmodels.view.ItemCardView
import net.xzos.upgradeAll.ui.viewmodels.view.holder.CardViewRecyclerViewHolder
import net.xzos.upgradeAll.utils.IconPalette


class CloudHubItemAdapter(private val mItemCardViewList: List<ItemCardView>) : RecyclerView.Adapter<CardViewRecyclerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewRecyclerViewHolder {
        val holder = CardViewRecyclerViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.cardview_item, parent, false))
        // 初始化页面，禁用无用按钮/信息
        holder.versioningTextView.visibility = View.GONE
        holder.versionCheckingBar.visibility = View.GONE
        with(holder.descTextView.parent as LinearLayout) {
            this.viewTreeObserver.addOnGlobalLayoutListener {
                this.layoutParams = (this.layoutParams as RelativeLayout.LayoutParams).apply {
                    this.marginEnd = 0
                }
                this.invalidate()
            }
        }
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
                        val addHubStatus = CloudConfigGetter.downloadCloudHubConfig(itemCardView.extraData.uuid)  // 下载数据
                        launch(Dispatchers.Main) {
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
        val itemCardView = mItemCardViewList[position]
        // 底栏设置
        if (itemCardView.extraData.isEmpty) {
            holder.appPlaceholderImageView.setImageDrawable(IconPalette.appItemPlaceholder)
            holder.appPlaceholderImageView.visibility = View.VISIBLE
            holder.itemCardView.visibility = View.GONE
        } else {
            holder.appPlaceholderImageView.visibility = View.GONE
            holder.itemCardView.visibility = View.VISIBLE
            // 加载仓库信息
            holder.name.text = itemCardView.name
            holder.descTextView.text = itemCardView.desc
            GlobalScope.launch {
                loadCloudHubIcon(holder.appIconImageView, itemCardView.extraData.uuid)
            }
            holder.versionCheckButton.visibility = if (HubDatabaseManager.exists(itemCardView.extraData.uuid))
                View.VISIBLE
            else
                View.GONE
        }
    }

    override fun getItemCount(): Int {
        return mItemCardViewList.size
    }

    private fun loadCloudHubIcon(iconImageView: ImageView, hubUuid: String?) {
        if (hubUuid != null) {
            val cloudHubConfigGson = CloudConfigGetter.getHubCloudConfig(hubUuid)
            val hubIconUrl = cloudHubConfigGson?.info?.hubIconUrl
            IconPalette.loadHubIconView(iconImageView, hubIconUrl)
        }
    }

    private fun setDownloadStatus(holder: CardViewRecyclerViewHolder, renew: Boolean) {
        // TODO: 并入一个工具类
        if (renew) {
            holder.versionCheckButton.visibility = View.GONE
            holder.versionCheckingBar.visibility = View.VISIBLE
        } else {
            holder.versionCheckButton.visibility = View.VISIBLE
            holder.versionCheckingBar.visibility = View.GONE
        }
    }
}