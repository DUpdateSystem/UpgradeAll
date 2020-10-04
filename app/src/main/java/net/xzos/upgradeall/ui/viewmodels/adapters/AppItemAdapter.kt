package net.xzos.upgradeall.ui.viewmodels.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.data_manager.HubDatabaseManager
import net.xzos.upgradeall.core.server_manager.module.BaseApp
import net.xzos.upgradeall.core.server_manager.module.app.App
import net.xzos.upgradeall.core.server_manager.module.app.Updater
import net.xzos.upgradeall.core.server_manager.module.applications.Applications
import net.xzos.upgradeall.ui.viewmodels.view.ItemCardView
import net.xzos.upgradeall.ui.viewmodels.view.holder.CardViewRecyclerViewHolder
import net.xzos.upgradeall.utils.*

open class AppItemAdapter(internal val mItemCardViewList: MutableList<ItemCardView>,
                          private val needUpdateAppsLiveData: MutableLiveData<MutableList<BaseApp>>
) : RecyclerView.Adapter<CardViewRecyclerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewRecyclerViewHolder {
        return CardViewRecyclerViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.cardview_item, parent, false))
    }

    override fun onBindViewHolder(holder: CardViewRecyclerViewHolder, position: Int) {
        val itemCardView = mItemCardViewList[position]
        itemCardView.extraData.app?.run {
            holder.itemCardView.visibility = View.VISIBLE
            holder.appPlaceholderImageView.visibility = View.GONE
            holder.nameTextView.text = itemCardView.name
            holder.typeTextView.text = itemCardView.type
            val appIconImageView = holder.appIconImageView
            when (this) {
                is App ->
                    IconPalette.loadAppIconView(appIconImageView, app = this)
                is Applications -> {
                    val hubIconUrl = HubDatabaseManager.getDatabase(this.appDatabase.hubUuid)
                            ?.hubConfig?.info?.hubIconUrl
                    IconPalette.loadApplicationsIconView(appIconImageView, hubIconUrl)
                }
            }
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

    fun setItemList(newList: MutableList<ItemCardView>) {
        val operationSteps = list1ToList2(mItemCardViewList, newList.toList())
        for (operationStep in operationSteps) {
            when (operationStep) {
                is ListDelOperationStep<*> -> {
                    with(operationStep.element) {
                        if (this is ItemCardView)
                            onItemDismiss(this)
                    }
                }
                is ListAddOperationStep<*> -> {
                    onAddItem(operationStep.index, operationStep.element as ItemCardView)
                }
                is ListSwapOperationStep -> {
                    onItemMove(operationStep.rowIndex, operationStep.newIndex)
                }
            }
        }
    }

    fun onAddItem(position: Int = 0, element: ItemCardView) {
        mItemCardViewList.add(position, element)
    }

    fun onItemDismiss(element: ItemCardView): ItemCardView? {
        val position = mItemCardViewList.indexOf(element)
        val removedItemCardView = mItemCardViewList.removeAt(position)
        notifyItemRemoved(position)
        return removedItemCardView
    }

    fun onItemDismiss(position: Int): ItemCardView? {
        val removedItemCardView = mItemCardViewList.removeAt(position)
        notifyItemRemoved(position)
        return removedItemCardView
    }

    fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
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
        GlobalScope.launch(Dispatchers.IO) {
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
                with(needUpdateAppsLiveData) {
                    value?.let {
                        if (updateStatus == 2 && !it.contains(app)) {
                            it.add(app)
                        } else if (updateStatus != 2 && it.contains(app)) {
                            it.remove(app)
                        } else return@let
                        this.notifyObserver()
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
