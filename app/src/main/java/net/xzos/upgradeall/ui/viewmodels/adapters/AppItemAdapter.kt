package net.xzos.upgradeall.ui.viewmodels.adapters

import android.annotation.SuppressLint
import android.app.Application
import android.view.LayoutInflater
import android.view.MenuItem
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
import kotlinx.coroutines.withContext
import net.xzos.dupdatesystem.core.data_manager.AppDatabaseManager
import net.xzos.dupdatesystem.core.data_manager.HubDatabaseManager
import net.xzos.dupdatesystem.core.server_manager.module.BaseApp
import net.xzos.dupdatesystem.core.server_manager.module.app.App
import net.xzos.dupdatesystem.core.server_manager.module.app.Updater
import net.xzos.upgradeall.R
import net.xzos.upgradeall.ui.activity.MainActivity
import net.xzos.upgradeall.ui.viewmodels.fragment.AppInfoFragment
import net.xzos.upgradeall.ui.viewmodels.view.ItemCardView
import net.xzos.upgradeall.ui.viewmodels.view.holder.CardViewRecyclerViewHolder
import net.xzos.upgradeall.ui.viewmodels.viewmodel.AppListPageViewModel
import net.xzos.upgradeall.utils.FileUtil
import net.xzos.upgradeall.utils.IconPalette


class AppItemAdapter(private val appListPageViewModel: AppListPageViewModel,
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
            val app = getItemCardView(holder).extraData.app
            if (app is App) {
                AppInfoFragment.bundleApp = app
                MainActivity.navigationItemId.value = R.id.appInfoFragment
            }
        }
        // TODO: 长按删除，暂时添加删除功能
        holder.itemCardView.setOnLongClickListener { view ->
            getItemCardView(holder).extraData.app?.run {
                val context = view.context
                PopupMenu(context, view).let { popupMenu ->
                    popupMenu.menu.let { menu ->
                        menu.add(context.getString(
                                if (appListPageViewModel.editableTab.value == true) R.string.edit_group
                                else R.string.add_to_group
                        )).let { menuItem ->
                            menuItem.setOnMenuItemClickListener {
                                showSelectGroupPopMenu(view, holder)
                                return@setOnMenuItemClickListener true
                            }
                        }
                        // 从分组中删除
                        if (appListPageViewModel.editableTab.value == true) {
                            menu.add(context.getString(R.string.delete_from_group)).let { menuItem ->
                                menuItem.setOnMenuItemClickListener {
                                    if (appListPageViewModel.removeItemFromGroup(holder.adapterPosition))
                                        onItemDismiss(holder.adapterPosition)
                                    return@setOnMenuItemClickListener true
                                }
                            }
                        }
                        // 导出
                        menu.add(context.getString(R.string.export)).let { menuItem ->
                            menuItem.setOnMenuItemClickListener {
                                val appConfigGson = AppDatabaseManager.translateAppConfig(this.appInfo)
                                FileUtil.clipStringToClipboard(
                                        GsonBuilder().setPrettyPrinting().create().toJson(appConfigGson),
                                        context
                                )
                                return@setOnMenuItemClickListener true
                            }
                        }
                        // 删除数据库
                        menu.add(context.getString(R.string.delete)).let { menuItem ->
                            menuItem.setOnMenuItemClickListener {
                                this.appInfo.delete()
                                onItemDismiss(holder.adapterPosition)
                                return@setOnMenuItemClickListener true
                            }
                        }
                        popupMenu.show()
                    }
                }
            }
            return@setOnLongClickListener true
        }
        // 长按强制检查版本
        holder.versionCheckButton.setOnLongClickListener {
            getItemCardView(holder).extraData.app?.run {
                this.renew()
                setAppStatusUI(holder, this)
                Toast.makeText(holder.versionCheckButton.context, "检查 ${holder.nameTextView.text} 的更新",
                        Toast.LENGTH_SHORT).show()
            }
            true
        }
        return holder
    }

    private fun showSelectGroupPopMenu(view: View, holder: CardViewRecyclerViewHolder) {
        PopupMenu(view.context, view).let { popupMenu ->
            popupMenu.menu.let { menu ->
                val tabInfoList = appListPageViewModel.getTabIndexList()
                for (containerTabListBean in tabInfoList)
                    menu.add(containerTabListBean.name).let { menuItem: MenuItem ->
                        menuItem.setOnMenuItemClickListener {
                            appListPageViewModel.moveItemToOtherGroup(holder.adapterPosition, containerTabListBean)
                            return@setOnMenuItemClickListener true
                        }
                    }
            }
            popupMenu.show()
        }
    }

    override fun onBindViewHolder(holder: CardViewRecyclerViewHolder, position: Int) {
        val itemCardView = mItemCardViewList[position]
        itemCardView.extraData.app?.run {
            holder.itemCardView.visibility = View.VISIBLE
            holder.appPlaceholderImageView.visibility = View.GONE
            if (this is App) {
                IconPalette.loadAppIconView(holder.appIconImageView, app = this)
            } else if (this is Application) {
                IconPalette.loadHubIconView(
                        holder.appIconImageView,
                        HubDatabaseManager.getDatabase(
                                this.appInfo.apiUuid
                        )?.cloudHubConfig?.info?.hubIconUrl
                )
            }
            holder.nameTextView.text = itemCardView.name
            holder.descTextView.text = itemCardView.desc
            setAppStatusUI(holder, this)
        } ?: kotlin.run {
            // 底栏设置
            holder.appPlaceholderImageView.setImageDrawable(IconPalette.appItemPlaceholder)
            holder.appPlaceholderImageView.visibility = View.VISIBLE
            holder.itemCardView.visibility = View.GONE
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

    private fun setAppStatusUI(holder: CardViewRecyclerViewHolder, app: BaseApp) {
        // 预先显示本地版本号，避免 0.0.0 example 版本号
        val installedVersioning = if (app is App) app.installedVersionNumber else null
        holder.versioningTextView.text = installedVersioning

        // 检查新版本
        setUpdateStatus(holder, true)
        GlobalScope.launch {
            val updateStatus = app.getUpdateStatus()
            withContext(Dispatchers.Main) {
                when (updateStatus) {
                    Updater.NETWORK_404 -> holder.versionCheckButton.setImageResource(R.drawable.ic_del_or_error)
                    Updater.APP_LATEST -> holder.versionCheckButton.setImageResource(R.drawable.ic_check_mark_circle)
                    Updater.APP_OUTDATED -> holder.versionCheckButton.setImageResource(R.drawable.ic_check_needupdate)
                    Updater.APP_NO_LOCAL -> holder.versionCheckButton.setImageResource(R.drawable.ic_local_error)
                }
                setUpdateStatus(holder, false)
                with(appListPageViewModel.needUpdateAppsLiveData) {
                    this.value?.let {
                        if (updateStatus == 2 && !it.contains(app)) {
                            it.add(app)
                        } else if (updateStatus != 2 && it.contains(app)) {
                            it.remove(app)
                        } else return@let
                        this.notifyObserver()
                    }
                }
                // 如果本地未安装，则显示最新版本号
                if (app is App) {
                    val latestVersioning = Updater(app).getLatestVersioning()
                    if (installedVersioning == null)
                        @SuppressLint("SetTextI18n")
                        holder.versioningTextView.text = "NEW: $latestVersioning"
                }
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

    private fun getItemCardView(holder: CardViewRecyclerViewHolder): ItemCardView =
            mItemCardViewList[holder.adapterPosition]

    companion object {
        /**
         * 拓展 LiveData 监听列表元素添加、删除操作的支持
         */
        private fun <T> MutableLiveData<T>.notifyObserver() {
            this.value = this.value
        }
    }
}