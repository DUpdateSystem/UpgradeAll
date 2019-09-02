package net.xzos.upgradeAll.ui.viewmodels.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView

import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.server.hub.HubManager
import net.xzos.upgradeAll.ui.activity.HubLocalActivity
import net.xzos.upgradeAll.ui.viewmodels.view.ItemCardView
import net.xzos.upgradeAll.ui.viewmodels.view.holder.CardViewRecyclerViewHolder


class LocalHubItemAdapter(private val mItemCardViewList: MutableList<ItemCardView>) : RecyclerView.Adapter<CardViewRecyclerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewRecyclerViewHolder {
        val holder = CardViewRecyclerViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.cardview_item, parent, false))
        // 长按菜单
        holder.itemCardView.setOnLongClickListener { v ->
            val position = holder.adapterPosition
            val itemCardView = mItemCardViewList[position]
            val uuid = itemCardView.extraData.uuid
            val popupMenu = PopupMenu(holder.itemCardView.context, v)
            val menuInflater = popupMenu.menuInflater
            menuInflater.inflate(R.menu.menu_long_click_cardview_item, popupMenu.menu)
            popupMenu.show()
            //设置item的点击事件
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    // 修改按钮
                    R.id.setting_button -> {
                        val intent = Intent(holder.itemCardView.context, HubLocalActivity::class.java)
                        intent.putExtra("hub_uuid", uuid)
                        holder.itemCardView.context.startActivity(intent)
                    }
                    // 删除按钮
                    R.id.del_button -> {
                        // 删除数据库
                        if (uuid != null)
                            HubManager.del(uuid)
                        // 删除指定数据库
                        mItemCardViewList.removeAt(holder.adapterPosition)
                        notifyItemRemoved(holder.adapterPosition)
                        notifyItemRangeChanged(holder.adapterPosition, mItemCardViewList.size)
                    }
                }// 删除 CardView
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