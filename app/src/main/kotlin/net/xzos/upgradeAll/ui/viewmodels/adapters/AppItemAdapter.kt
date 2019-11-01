package net.xzos.upgradeAll.ui.viewmodels.adapters

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.database.RepoDatabase
import net.xzos.upgradeAll.server.ServerContainer
import net.xzos.upgradeAll.server.app.manager.module.Updater
import net.xzos.upgradeAll.ui.activity.MainActivity
import net.xzos.upgradeAll.ui.viewmodels.view.ItemCardView
import net.xzos.upgradeAll.ui.viewmodels.view.holder.CardViewRecyclerViewHolder
import net.xzos.upgradeAll.utils.IconPalette
import org.litepal.LitePal


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

    private val uiConfig = ServerContainer.UIConfig

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
        holder.itemCardView.setOnClickListener {
            PopupMenu(it.context, it).let { popupMenu ->
                popupMenu.menu.add(it.context.getString(R.string.delete)).let { menuItem ->
                    menuItem.setOnMenuItemClickListener {
                        val position = holder.adapterPosition
                        val itemCardView = mItemCardViewList[position]
                        val appDatabaseId = itemCardView.extraData.databaseId
                        AppManager.delApp(appDatabaseId)
                        LitePal.delete(RepoDatabase::class.java, appDatabaseId)
                        onItemDismiss(position)
                        true
                    }
                    popupMenu.show()
                }
            }
        }

        // 长按强制检查版本
        holder.versionCheckButton.setOnLongClickListener {
            val position = holder.adapterPosition
            val itemCardView = mItemCardViewList[position]
            val appDatabaseId = itemCardView.extraData.databaseId
            AppManager.delApp(appDatabaseId)
            AppManager.setApp(appDatabaseId)
            setAppStatusUI(appDatabaseId, holder)
            Toast.makeText(holder.versionCheckButton.context, String.format("检查 %s 的更新", holder.name.text.toString()),
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
            setAppIcon(holder.appIconImageView, itemCardView.iconInfo)
            val appDatabaseId = itemCardView.extraData.databaseId
            holder.name.text = itemCardView.name
            holder.descTextView.text = itemCardView.desc
            setAppStatusUI(appDatabaseId, holder)
        }
    }

    override fun getItemCount(): Int {
        return mItemCardViewList.size
    }

    fun onAddItem(position: Int = 0, element: ItemCardView) {
        if (position < mItemCardViewList.size) {
            mItemCardViewList.add(position, element)
            notifyItemRangeChanged(position, itemCount)
            uiConfig.appList.add(position, element.extraData.databaseId)
            uiConfig.save()
        }
    }

    fun onItemDismiss(position: Int): ItemCardView? {
        return if (position != -1 && position < mItemCardViewList.size) {
            val removedItemCardView = mItemCardViewList[position]
            mItemCardViewList.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, itemCount)
            if (uiConfig.appList.isNullOrEmpty()) {
                uiConfig.appList.removeAt(position)
                uiConfig.save()
            }
            removedItemCardView
        } else
            null
    }

    fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        // TODO: 菜单集成
        val appList = uiConfig.appList
        appList[fromPosition] = appList[toPosition]
                .also { appList[toPosition] = appList[fromPosition] }
        uiConfig.appList = appList
        uiConfig.save()
        mItemCardViewList[fromPosition] = mItemCardViewList[toPosition]
                .also { mItemCardViewList[toPosition] = mItemCardViewList[fromPosition] }
        notifyItemMoved(fromPosition, toPosition)
        return true
    }

    private fun setAppIcon(iconImageView: ImageView, iconInfo: Pair<String?, String?>) {
        Glide.with(iconImageView).load(iconInfo.first ?: "").let {
            if (iconInfo.first == null) {
                try {
                    it.placeholder(
                            iconImageView.context.packageManager.getApplicationIcon(iconInfo.second!!)
                    )
                } catch (e: PackageManager.NameNotFoundException) {
                    return@let
                }
            }
            it.into(iconImageView)
        }
    }

    private fun setAppStatusUI(appDatabaseId: Long, holder: CardViewRecyclerViewHolder) {
        val app = AppManager.getApp(appDatabaseId)
        val updater = Updater(app.engine)
        // 预先显示本地版本号，避免 0.0.0 example 版本号
        val installedVersioning = app.installedVersioning
        holder.versioningTextView.text = installedVersioning ?: ""

        // 检查新版本
        GlobalScope.launch {
            val isSuccessRenew = updater.isSuccessRenew()
            val isLatest = app.isLatest()
            val latestVersioning = updater.getLatestVersioning()
            val updateStatus =  // 0: 404; 1: latest; 2: need update; 3: no app
                    //检查是否取得云端版本号
                    if (isSuccessRenew) {
                        // 检查是否获取本地版本号
                        if (installedVersioning != null) {
                            // 检查本地版本
                            if (isLatest) {
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
            runBlocking(Dispatchers.Main) {
                setUpdateStatus(holder, true)
                when (updateStatus) {
                    0 -> holder.versionCheckButton.setImageResource(R.drawable.ic_del_or_error)
                    1 -> holder.versionCheckButton.setImageResource(R.drawable.ic_check_mark)
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
                        }
                        return@let
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
            holder.versionCheckButton.visibility = View.INVISIBLE
            holder.versionCheckingBar.visibility = View.VISIBLE
        } else {
            holder.versionCheckButton.visibility = View.VISIBLE
            holder.versionCheckingBar.visibility = View.INVISIBLE
        }
    }

    companion object {
        private val AppManager = ServerContainer.AppManager
    }
}