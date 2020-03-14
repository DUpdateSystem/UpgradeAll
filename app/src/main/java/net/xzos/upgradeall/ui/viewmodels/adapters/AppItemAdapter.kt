package net.xzos.upgradeall.ui.viewmodels.adapters

import android.annotation.SuppressLint
import android.app.Application
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.xzos.dupdatesystem.core.data_manager.HubDatabaseManager
import net.xzos.dupdatesystem.core.server_manager.module.BaseApp
import net.xzos.dupdatesystem.core.server_manager.module.app.App
import net.xzos.dupdatesystem.core.server_manager.module.app.Updater
import net.xzos.upgradeall.R
import net.xzos.upgradeall.ui.viewmodels.view.ItemCardView
import net.xzos.upgradeall.ui.viewmodels.view.holder.CardViewRecyclerViewHolder
import net.xzos.upgradeall.ui.viewmodels.viewmodel.AppListContainerViewModel
import net.xzos.upgradeall.utils.IconPalette
import net.xzos.upgradeall.utils.getByHolder
import net.xzos.upgradeall.utils.notifyObserver

open class AppItemAdapter(private val appListPageViewModel: AppListContainerViewModel,
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
        // 长按强制检查版本
        holder.versionCheckButton.setOnLongClickListener {
            mItemCardViewList.getByHolder(holder).extraData.app?.run {
                this.renew()
                setAppStatusUI(holder, this)
                with(holder.versionCheckButton.context) {
                    val name = holder.nameTextView.text.toString()
                    val text = getString(R.string.checking_update).replace("%name", name)
                    Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
                }
            }
            true
        }
        return holder
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
                holder.hubNameTextView.visibility = View.GONE
            }
            holder.nameTextView.text = itemCardView.name
            holder.typeTextView.text = itemCardView.type
            holder.hubNameTextView.text = itemCardView.hubName
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
        } else null
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
            val latestVersioning = if (app is App)
                Updater(app).getLatestVersioning()
            else null
            withContext(Dispatchers.Main) {
                when (updateStatus) {
                    Updater.APP_LATEST -> holder.versionCheckButton.setImageResource(R.drawable.ic_check_mark_circle)
                    Updater.APP_OUTDATED -> holder.versionCheckButton.setImageResource(R.drawable.ic_check_needupdate)
                    Updater.APP_NO_LOCAL -> holder.versionCheckButton.setImageResource(R.drawable.ic_local_error)
                    Updater.NETWORK_ERROR -> holder.versionCheckButton.setImageResource(R.drawable.ic_del_or_error)
                    else -> holder.versionCheckButton.setImageResource(R.drawable.ic_del_or_error)
                }
                // 如果本地未安装，则显示最新版本号
                if (installedVersioning == null && latestVersioning != null)
                    @SuppressLint("SetTextI18n")
                    holder.versioningTextView.text = "NEW: $latestVersioning"
                setUpdateStatus(holder, false)
            }
            with(appListPageViewModel.needUpdateAppsLiveData) {
                this.value?.let {
                    if (updateStatus == 2 && !it.contains(app)) {
                        it.add(app)
                    } else if (updateStatus != 2 && it.contains(app)) {
                        it.remove(app)
                    } else return@let
                    withContext(Dispatchers.Main) {
                        this@with.notifyObserver()
                    }
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
}
