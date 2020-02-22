package net.xzos.upgradeall.ui.viewmodels.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeall.R
import net.xzos.upgradeall.data_manager.database.manager.HubDatabaseManager
import net.xzos.upgradeall.server_manager.runtime.manager.AppManager
import net.xzos.upgradeall.ui.viewmodels.view.ItemCardView
import net.xzos.upgradeall.ui.viewmodels.view.holder.CardViewRecyclerViewHolder
import net.xzos.upgradeall.utils.IconInfo
import net.xzos.upgradeall.utils.IconPalette
import net.xzos.upgradeall.utils.MiscellaneousUtils

class CloudAppItemAdapter(private val mItemCardViewList: List<ItemCardView>, private val context: Context?) : RecyclerView.Adapter<CardViewRecyclerViewHolder>() {

    private val cloudConfigGetter = MiscellaneousUtils.cloudConfigGetter

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
                    val appUuid = itemCardView.extraData.uuid
                    if (appUuid != null) {
                        Toast.makeText(holder.itemCardView.context, "开始下载", Toast.LENGTH_LONG).show()
                        // 下载数据
                        setDownloadStatus(holder, true)
                        GlobalScope.launch {
                            val appDatabase = cloudConfigGetter.downloadCloudAppConfig(appUuid)  // 下载数据
                            launch(Dispatchers.Main) {
                                setDownloadStatus(holder, false)
                                if (appDatabase != null) {
                                    holder.versionCheckButton.visibility = View.VISIBLE
                                    checkHubDependency(hubUuid = appDatabase.api_uuid)
                                    AppManager.addSingleApp(databaseId = appDatabase.id)
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
        if (itemCardView.extraData.uuid == null) {
            holder.appPlaceholderImageView.setImageDrawable(IconPalette.appItemPlaceholder)
            holder.appPlaceholderImageView.visibility = View.VISIBLE
            holder.itemCardView.visibility = View.GONE
        } else {
            holder.appPlaceholderImageView.visibility = View.GONE
            holder.itemCardView.visibility = View.VISIBLE
            // 加载跟踪项信息
            holder.nameTextView.text = itemCardView.name
            holder.descTextView.text = itemCardView.desc
            checkAppConfigLocalStatus(holder, itemCardView.extraData.uuid)
            GlobalScope.launch { loadCloudAppIcon(holder.appIconImageView, itemCardView.extraData.uuid) }
        }
    }

    override fun getItemCount(): Int {
        return mItemCardViewList.size
    }

    private fun loadCloudAppIcon(iconImageView: ImageView, appUuid: String?) {
        if (appUuid != null) {
            val cloudAppConfigGson = cloudConfigGetter.getAppCloudConfig(appUuid)
            val appModule = cloudAppConfigGson?.appConfig?.targetChecker?.extraString
            IconPalette.loadAppIconView(iconImageView, iconInfo = IconInfo(app_package = appModule))
        }
    }

    private fun checkAppConfigLocalStatus(holder: CardViewRecyclerViewHolder, appUuid: String) {
        val versionCheckButton = holder.versionCheckButton
        AppManager.getSingleApp(uuid = appUuid)?.run {
            versionCheckButton.visibility = View.VISIBLE
            setDownloadStatus(holder, true)
            GlobalScope.launch {
                val appConfigGson = cloudConfigGetter.getAppCloudConfig(appUuid)
                this@run.appInfo.extraData.let {
                    val cloudAppVersion = it?.cloudAppConfigGson?.info?.configVersion
                    val localAppVersion = appConfigGson?.info?.configVersion
                    launch(Dispatchers.Main) {
                        if (cloudAppVersion != null && localAppVersion != null && cloudAppVersion > localAppVersion)
                            versionCheckButton.setImageResource(R.drawable.ic_check_needupdate)
                        setDownloadStatus(holder, false)
                    }
                }
            }
        } ?: kotlin.run {
            versionCheckButton.visibility = View.GONE
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

    private fun checkHubDependency(hubUuid: String?) {
        if (!HubDatabaseManager.exists(hubUuid)) {
            context?.let {
                AlertDialog.Builder(it).apply {
                    setMessage(R.string.whether_download_dependency_hub)
                    setPositiveButton(R.string.ok) { dialog, _ ->
                        Toast.makeText(context, R.string.start_download_dependency_hub, Toast.LENGTH_LONG).show()
                        GlobalScope.launch { cloudConfigGetter.downloadCloudHubConfig(hubUuid) }
                        dialog.cancel()
                    }
                    setNegativeButton(R.string.cancel) { dialog, _ ->
                        dialog.cancel()
                    }

                    setCancelable(false)
                }.create().show()
            }
        }
    }
}