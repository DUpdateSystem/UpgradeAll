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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.data.database.manager.AppDatabaseManager
import net.xzos.upgradeAll.data.database.manager.CloudConfigGetter
import net.xzos.upgradeAll.ui.viewmodels.view.ItemCardView
import net.xzos.upgradeAll.ui.viewmodels.view.holder.CardViewRecyclerViewHolder
import net.xzos.upgradeAll.utils.IconPalette

class CloudAppItemAdapter(private val mItemCardViewList: List<ItemCardView>) : RecyclerView.Adapter<CardViewRecyclerViewHolder>() {

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
                    // 下载
                    val configFileName = itemCardView.extraData.configFileName
                    if (configFileName != null) {
                        Toast.makeText(holder.itemCardView.context, "开始下载", Toast.LENGTH_LONG).show()
                        // 下载数据
                        setDownloadStatus(holder, true)
                        GlobalScope.launch {
                            val cloudHubConfigGson = CloudConfigGetter.getAppConfig(configFileName)
                            // TODO: 配置文件地址与仓库地址分离
                            // addHubStatus: 1 获取 AppConfig 成功, 2 添加数据库成功, -1 获取 AppConfig 失败, -2 添加数据库失败
                            val addHubStatus: Int =
                                    if (cloudHubConfigGson != null) {
                                        // 添加数据库
                                        if (AppDatabaseManager.setDatabase(0, cloudHubConfigGson)) {
                                            2
                                        } else -2
                                    } else -1
                            runBlocking(Dispatchers.Main) {
                                setDownloadStatus(holder, false)
                                when (addHubStatus) {
                                    2 -> {
                                        Toast.makeText(holder.itemCardView.context, "数据添加成功", Toast.LENGTH_LONG).show()
                                        holder.versionCheckButton.visibility = View.VISIBLE
                                    }
                                    -1 -> Toast.makeText(holder.itemCardView.context, "获取基础配置文件失败", Toast.LENGTH_LONG).show()
                                    -2 -> Toast.makeText(holder.itemCardView.context, "什么？数据库添加失败！", Toast.LENGTH_LONG).show()
                                }
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
            // 加载跟踪项信息
            holder.name.text = itemCardView.name
            holder.descTextView.text = itemCardView.desc
            GlobalScope.launch {
                loadCloudAppIcon(holder.appIconImageView, itemCardView.extraData.configFileName)
            }
            holder.versionCheckButton.visibility = if (AppDatabaseManager.exists(itemCardView.extraData.uuid))
                View.VISIBLE
            else
                View.GONE
        }
    }

    override fun getItemCount(): Int {
        return mItemCardViewList.size
    }

    private fun loadCloudAppIcon(iconImageView: ImageView, configFileName: String?) {
        if (configFileName != null) {
            val cloudAppConfigGson = CloudConfigGetter.getAppConfig(configFileName)
            val appModule = cloudAppConfigGson?.appConfig?.targetChecker?.extraString
            IconPalette.loadAppIconView(iconImageView, iconInfo = Pair(null, appModule))
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
