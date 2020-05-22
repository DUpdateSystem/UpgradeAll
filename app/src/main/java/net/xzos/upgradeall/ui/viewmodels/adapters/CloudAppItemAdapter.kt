package net.xzos.upgradeall.ui.viewmodels.adapters

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.data_manager.HubDatabaseManager
import net.xzos.upgradeall.core.server_manager.AppManager
import net.xzos.upgradeall.ui.viewmodels.view.CloudConfigListItemView
import net.xzos.upgradeall.ui.viewmodels.view.holder.CardViewRecyclerViewHolder
import net.xzos.upgradeall.utils.IconInfo
import net.xzos.upgradeall.utils.IconPalette
import net.xzos.upgradeall.utils.MiscellaneousUtils

class CloudAppItemAdapter(
        private val mItemCardViewList: List<CloudConfigListItemView>,
        private val context: Context?
) : CloudItemAdapter(mItemCardViewList) {

    private val cloudConfigGetter = MiscellaneousUtils.cloudConfigGetter

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewRecyclerViewHolder {
        val holder = super.onCreateViewHolder(parent, viewType)
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
                    val appUuid = itemCardView.uuid
                    if (appUuid != null) {
                        Toast.makeText(holder.itemCardView.context, R.string.download_start, Toast.LENGTH_LONG).show()
                        // 下载数据
                        setDownloadStatus(holder, true)
                        GlobalScope.launch {
                            val appDatabase = cloudConfigGetter.downloadCloudAppConfig(appUuid)  // 下载数据
                            withContext(Dispatchers.Main) {
                                setDownloadStatus(holder, false)
                                if (appDatabase != null) {
                                    holder.versionCheckButton.visibility = View.VISIBLE
                                    checkHubDependency(hubUuid = appDatabase.hubUuid)
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
        super.onBindViewHolder(holder, position)
        val itemCardView = mItemCardViewList[position]
        // 加载跟踪项信息
        if (itemCardView.uuid != null) {
            holder.nameTextView.text = itemCardView.name
            holder.typeTextView.text = itemCardView.type
            holder.hubNameTextView.text = itemCardView.hubName
            checkAppConfigLocalStatus(holder, itemCardView.uuid)
            GlobalScope.launch { loadCloudAppIcon(holder.appIconImageView, itemCardView.uuid) }
        }
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
                this@run.appDatabase.extraData.let {
                    val cloudAppVersion = it?.cloudAppConfig?.info?.configVersion
                    val localAppVersion = appConfigGson?.info?.configVersion
                    withContext(Dispatchers.Main) {
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
