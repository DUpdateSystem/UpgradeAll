package net.xzos.upgradeAll.ui.viewmodels.adapters

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView

import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.server.hub.CloudHub
import net.xzos.upgradeAll.server.hub.HubManager
import net.xzos.upgradeAll.ui.viewmodels.view.ItemCardView
import net.xzos.upgradeAll.ui.viewmodels.view.holder.CardViewRecyclerViewHolder


class CloudHubItemAdapter(private val mItemCardViewList: List<ItemCardView>, private val mCloudHub: CloudHub) : RecyclerView.Adapter<CardViewRecyclerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewRecyclerViewHolder {
        val holder = CardViewRecyclerViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.cardview_item, parent, false))
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
                when (item.itemId) {
                    // 下载
                    R.id.download -> {
                        Toast.makeText(holder.itemCardView.context, "开始下载", Toast.LENGTH_LONG).show()
                        // 下载数据
                        Thread {
                            val cloudHubConfigGson = mCloudHub.getHubConfig(itemCardView.extraData.configFileName!!)
                            val addHubSuccess: Boolean
                            if (cloudHubConfigGson != null) {
                                val cloudHubConfigJS = mCloudHub.getHubConfigJS(cloudHubConfigGson.webCrawler.filePath
                                        ?: "")
                                if (cloudHubConfigJS != null)
                                    addHubSuccess = HubManager.add(cloudHubConfigGson, cloudHubConfigJS)
                                else {
                                    addHubSuccess = false
                                    Handler(Looper.getMainLooper()).post { Toast.makeText(holder.itemCardView.context, "获取 JS 代码失败", Toast.LENGTH_LONG).show() }
                                }
                            } else {
                                addHubSuccess = false
                                Handler(Looper.getMainLooper()).post { Toast.makeText(holder.itemCardView.context, "数据下载失败", Toast.LENGTH_LONG).show() }
                            }
                            Handler(Looper.getMainLooper()).post {
                                if (addHubSuccess) {
                                    Toast.makeText(holder.itemCardView.context, "数据添加成功", Toast.LENGTH_LONG).show()
                                } else
                                    Toast.makeText(holder.itemCardView.context, "什么？数据库添加失败！", Toast.LENGTH_LONG).show()
                            }
                        }.start()
                    }
                }// 添加数据库
                true
            }
            true
        }
        return holder
    }

    override fun onBindViewHolder(holder: CardViewRecyclerViewHolder, position: Int) {
        val itemCardView = mItemCardViewList[position]
        // 底栏设置
        if (itemCardView.extraData.isEmpty) {
            holder.itemCardView.visibility = View.GONE
            holder.endTextView.visibility = View.VISIBLE
        } else {
            holder.itemCardView.visibility = View.VISIBLE
            holder.endTextView.visibility = View.GONE
            holder.name.text = itemCardView.name
            holder.api.text = itemCardView.api
            holder.descTextView.text = itemCardView.desc
            holder.descTextView.isEnabled = false
        }
    }

    override fun getItemCount(): Int {
        return mItemCardViewList.size
    }
}