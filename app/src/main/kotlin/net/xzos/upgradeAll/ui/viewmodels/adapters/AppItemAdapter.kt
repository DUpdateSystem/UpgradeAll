package net.xzos.upgradeAll.ui.viewmodels.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.data.database.manager.AppDatabaseManager
import net.xzos.upgradeAll.server.ServerContainer
import net.xzos.upgradeAll.server.app.manager.module.Updater
import net.xzos.upgradeAll.ui.activity.MainActivity
import net.xzos.upgradeAll.ui.viewmodels.view.ItemCardView
import net.xzos.upgradeAll.ui.viewmodels.view.holder.CardViewRecyclerViewHolder
import net.xzos.upgradeAll.utils.FileUtil
import net.xzos.upgradeAll.utils.IconPalette


class AppItemAdapter(private val needUpdateAppIdLiveData: MutableLiveData<MutableList<Long>>,
                     itemCardViewLiveData: LiveData<MutableList<ItemCardView>>,
                     owner: LifecycleOwner)
    : RecyclerView.Adapter<CardViewRecyclerViewHolder>() {

    private var mItemCardViewList: MutableList<ItemCardView> = mutableListOf()
    // TODO: 数据无法自动更新（需修复）

    init {
        itemCardViewLiveData.observe(owner, Observer { list ->
            mItemCardViewList = list
            notifyDataSetChanged()
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewRecyclerViewHolder {
        val holder = CardViewRecyclerViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.cardview_item, parent, false))
        // 单击展开 Release 详情页
        holder.itemCardView.setOnClickListener {
            val position = holder.adapterPosition
            val itemCardView = mItemCardViewList[position]
            MainActivity.navigationItemId.value = Pair(R.id.appInfoFragment, itemCardView.extraData.databaseId)
        }
        // TODO: 长按删除，暂时添加删除功能
        holder.itemCardView.setOnLongClickListener {
            val context = it.context
            PopupMenu(context, it).let { popupMenu ->
                popupMenu.menu.let { menu ->
                    // 导出
                    menu.add(context.getString(R.string.export)).let { menuItem ->
                        menuItem.setOnMenuItemClickListener {
                            val position = holder.adapterPosition
                            val itemCardView = mItemCardViewList[position]
                            val appDatabaseId = itemCardView.extraData.databaseId
                            val appConfigGson = AppDatabaseManager.getAppConfig(appDatabaseId)
                            FileUtil.clipStringToClipboard(
                                    GsonBuilder().setPrettyPrinting().create().toJson(appConfigGson),
                                    context
                            )
                            return@setOnMenuItemClickListener true
                        }
                    }
                    // 删除
                    menu.add(context.getString(R.string.delete)).let { menuItem ->
                        menuItem.setOnMenuItemClickListener {
                            val position = holder.adapterPosition
                            val itemCardView = mItemCardViewList[position]
                            val appDatabaseId = itemCardView.extraData.databaseId
                            AppManager.delApp(appDatabaseId)
                            AppDatabaseManager.del(appDatabaseId)
                            onItemDismiss(position)
                            return@setOnMenuItemClickListener true
                        }
                    }
                    popupMenu.show()
                }
            }
            return@setOnLongClickListener true
        }

        // 长按强制检查版本
        holder.versionCheckButton.setOnLongClickListener {
            val position = holder.adapterPosition
            val itemCardView = mItemCardViewList[position]
            val appDatabaseId = itemCardView.extraData.databaseId
            AppManager.setApp(appDatabaseId)
            setAppStatusUI(appDatabaseId, holder)
            Toast.makeText(holder.versionCheckButton.context, "检查 ${holder.nameTextView.text} 的更新",
                    Toast.LENGTH_SHORT).show()
            true
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
            holder.appIconImageView.let {
                IconPalette.loadAppIconView(it, iconInfo = itemCardView.iconInfo)
            }
            val appDatabaseId = itemCardView.extraData.databaseId
            holder.nameTextView.text = itemCardView.name
            holder.descTextView.text = itemCardView.desc
            setAppStatusUI(appDatabaseId, holder)
        }
    }

    override fun getItemCount() =
            mItemCardViewList.size

    fun onAddItem(position: Int = 0, element: ItemCardView) {
        if (position < mItemCardViewList.size) {
            mItemCardViewList.add(position, element)
            notifyItemRangeChanged(position, itemCount)
        }
    }

    fun onItemDismiss(position: Int): ItemCardView? {
        return if (position != -1 && position < mItemCardViewList.size) {
            val removedItemCardView = mItemCardViewList[position]
            mItemCardViewList.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, itemCount)
            removedItemCardView
        } else
            null
    }

    fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        // TODO: 菜单集成
        mItemCardViewList[fromPosition] = mItemCardViewList[toPosition]
                .also { mItemCardViewList[toPosition] = mItemCardViewList[fromPosition] }
        notifyItemMoved(fromPosition, toPosition)
        return true
    }

    private fun setAppStatusUI(appDatabaseId: Long, holder: CardViewRecyclerViewHolder) {
        val app = AppManager.getApp(appDatabaseId)
        val updater = Updater(app.engine)
        // 预先显示本地版本号，避免 0.0.0 example 版本号
        val installedVersioning = app.installedVersioning
        holder.versioningTextView.text = installedVersioning ?: ""

        // 检查新版本
        setUpdateStatus(holder, true)
        GlobalScope.launch {
            val isSuccessRenew = updater.isSuccessRenew()
            val latestVersioning = updater.getLatestVersioning()
            val updateStatus =  // 0: 404; 1: latest; 2: need update; 3: no app
                    //检查是否取得云端版本号
                    if (isSuccessRenew) {
                        // 检查是否获取本地版本号
                        if (installedVersioning != null || app.markProcessedVersionNumber != null) {
                            // 检查本地版本
                            if (app.isLatest()) {
                                1
                            } else {
                                2
                            }
                        } else {
                            3
                        }
                    } else {
                        0
                    }
            launch(Dispatchers.Main) {
                when (updateStatus) {
                    0 -> holder.versionCheckButton.setImageResource(R.drawable.ic_del_or_error).also {
                        AppManager.delApp(appDatabaseId)  // 刷新错误删除缓存数据
                    }
                    1 -> holder.versionCheckButton.setImageResource(R.drawable.ic_check_mark_circle)
                    2 -> holder.versionCheckButton.setImageResource(R.drawable.ic_check_needupdate)
                    3 -> holder.versionCheckButton.setImageResource(R.drawable.ic_local_error)
                }
                setUpdateStatus(holder, false)
                with(needUpdateAppIdLiveData) {
                    this.value?.let {
                        if (updateStatus == 2 && !it.contains(appDatabaseId)) {
                            it.add(appDatabaseId)
                        } else if (updateStatus != 2 && it.contains(appDatabaseId)) {
                            it.remove(appDatabaseId)
                        } else return@let
                        this.notifyObserver()
                    }
                }
                // 如果本地未安装，则显示最新版本号
                if (installedVersioning == null)
                    @SuppressLint("SetTextI18n")
                    holder.versioningTextView.text = "NEW: $latestVersioning"
            }
        }
    }

    private fun setUpdateStatus(holder: CardViewRecyclerViewHolder, renew: Boolean) {
        if (renew) {
            holder.versionCheckButton.visibility = View.GONE
            holder.versionCheckingBar.visibility = View.VISIBLE
        } else {
            holder.versionCheckButton.visibility = View.VISIBLE
            holder.versionCheckingBar.visibility = View.GONE
        }
    }

    companion object {
        private val AppManager = ServerContainer.AppManager

        /**
         * 拓展 LiveData 监听列表元素添加、删除操作的支持
         */
        private fun <T> MutableLiveData<T>.notifyObserver() {
            this.value = this.value
        }
    }
}