package net.xzos.upgradeAll.ui.viewmodels.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.json.gson.HubConfig
import net.xzos.upgradeAll.server.hub.CloudHub
import net.xzos.upgradeAll.server.hub.HubManager
import net.xzos.upgradeAll.ui.viewmodels.view.ItemCardView
import net.xzos.upgradeAll.ui.viewmodels.view.holder.CardViewRecyclerViewHolder
import net.xzos.upgradeAll.utils.IconPalette


class CloudHubItemAdapter(private val mItemCardViewList: List<ItemCardView>, private val mCloudHub: CloudHub) : RecyclerView.Adapter<CardViewRecyclerViewHolder>() {

    private var cloudHubConfigGson: HubConfig? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewRecyclerViewHolder {
        val holder = CardViewRecyclerViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.cardview_item, parent, false))
        // 禁用无用按钮/信息
        holder.versioningTextView.visibility = View.GONE
        holder.versionCheckingBar.visibility = View.GONE
        holder.versionCheckButton.visibility = View.GONE
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
                    // 下载
                    Toast.makeText(holder.itemCardView.context, "开始下载", Toast.LENGTH_LONG).show()
                    // 下载数据
                    GlobalScope.launch {
                        if (cloudHubConfigGson == null)
                            cloudHubConfigGson = mCloudHub.getHubConfig(itemCardView.extraData.configFileName!!)
                        // TODO: 配置文件地址与仓库地址分离
                        // addHubStatus: 1 获取 HubConfig 成功, 2 获取 JS 成功, 3 添加数据库成功, -1 获取 HubConfig 失败, -2 解析 JS 失败, -3 添加数据库失败
                        val addHubStatus: Int =
                                if (cloudHubConfigGson != null) {
                                    val cloudHubConfigJS = mCloudHub.getHubConfigJS(cloudHubConfigGson?.webCrawler?.filePath
                                            ?: "")
                                    if (cloudHubConfigJS != null) {
                                        if (HubManager.add(cloudHubConfigGson, cloudHubConfigJS)) {
                                            3
                                        } else -3
                                    } else -2
                                } else -1
                        runBlocking(Dispatchers.Main) {
                            when (addHubStatus) {
                                3 -> Toast.makeText(holder.itemCardView.context, "数据添加成功", Toast.LENGTH_LONG).show()
                                -2 -> Toast.makeText(holder.itemCardView.context, "获取 JS 代码失败", Toast.LENGTH_LONG).show()
                                -3 -> Toast.makeText(holder.itemCardView.context, "什么？数据库添加失败！", Toast.LENGTH_LONG).show()
                            }
                        }
                    }// 添加数据库
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
            holder.itemCardView.visibility = View.VISIBLE
            holder.appPlaceholderImageView.visibility = View.GONE
            holder.name.text = itemCardView.name
            holder.descTextView.text = itemCardView.desc
            GlobalScope.launch { loadCloudHubIcon(holder.appIconImageView, itemCardView.extraData.configFileName) }
        }
    }

    override fun getItemCount(): Int {
        return mItemCardViewList.size
    }

    private fun loadCloudHubIcon(iconImageView: ImageView, configFileName: String?) {
        if (configFileName != null) {
            cloudHubConfigGson = mCloudHub.getHubConfig(configFileName)
            val hubIconUrl = cloudHubConfigGson?.info?.hubIconUrl
            if (hubIconUrl != null)
                Glide.with(iconImageView)
                        .load(hubIconUrl)
                        .apply(RequestOptions.circleCropTransform())
                        .into(iconImageView)
        }
    }
}