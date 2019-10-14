package net.xzos.upgradeAll.ui.viewmodels.adapters

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.coroutines.*
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.server.ServerContainer
import net.xzos.upgradeAll.server.app.manager.module.Updater
import net.xzos.upgradeAll.ui.activity.AppSettingActivity
import net.xzos.upgradeAll.ui.viewmodels.view.ItemCardView
import net.xzos.upgradeAll.ui.viewmodels.view.holder.CardViewRecyclerViewHolder
import net.xzos.upgradeAll.utils.IconPalette


class AppItemAdapter(private val needUpdateAppIdLiveLiveData: MutableLiveData<MutableList<Long>>, itemCardViewLiveData: LiveData<MutableList<ItemCardView>>, owner: LifecycleOwner) : RecyclerView.Adapter<CardViewRecyclerViewHolder>() {

    private var mItemCardViewList: MutableList<ItemCardView> = mutableListOf()

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
            showDialogWindow(holder)
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

        // 打开指向Url
        holder.descTextView.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(holder.descTextView.text.toString())
            val chooser = Intent.createChooser(intent, "请选择浏览器")
            if (intent.resolveActivity(holder.descTextView.context.packageManager) != null) {
                holder.descTextView.context.startActivity(chooser)
            }
        }
        return holder
    }

    override fun onBindViewHolder(holder: CardViewRecyclerViewHolder, position: Int) {
        val itemCardView = mItemCardViewList[position]
        // 底栏设置
        if (itemCardView.extraData.isEmpty) {
            holder.appPlaceholderImageView.setImageDrawable(IconPalette.AppItemPlaceholder)
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
        if (iconInfo.first != null) {
            Glide.with(iconImageView)
                    .load(iconInfo.first)
                    .into(iconImageView)
        } else if (iconInfo.second != null) {
            val packageName = iconInfo.second!!
            val packageManager = iconImageView.context.packageManager
            try {
                packageManager.getPackageInfo(packageName, 0)
                val icon = packageManager.getApplicationIcon(packageName)
                Glide.with(iconImageView)
                        .load("")
                        .placeholder(icon)
                        .into(iconImageView)
            } catch (e: PackageManager.NameNotFoundException) {
            }
        }
    }

    private fun setAppStatusUI(appDatabaseId: Long, holder: CardViewRecyclerViewHolder) {
        val app = AppManager.getApp(appDatabaseId)
        val updater = Updater(app.engine)
        setUpdateStatus(holder, true)
        GlobalScope.launch {
            val isSuccessRenew = async { updater.isSuccessRenew() }
            val isLatest = async { app.isLatest() }
            val latestVersioning = async { app.latestVersioning }
            val installedVersioning = async { app.installedVersioning }
            val updateStatus =   // 0: 404; 1: latest; 2: need update; 3: no app
                    //检查是否取得云端版本号
                    if (isSuccessRenew.await()) {
                        // 检查是否获取本地版本号
                        if (app.installedVersioning != null) {
                            // 检查本地版本
                            if (isLatest.await()) {
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
                when (updateStatus) {
                    0 -> holder.versionCheckButton.setImageResource(R.drawable.ic_del_or_error)
                    1 -> holder.versionCheckButton.setImageResource(R.drawable.ic_check_mark)
                    2 -> holder.versionCheckButton.setImageResource(R.drawable.ic_check_needupdate)
                    3 -> holder.versionCheckButton.setImageResource(R.drawable.ic_local_error)
                }
                with(needUpdateAppIdLiveLiveData) {
                    this.value?.let {
                        if (updateStatus == 2 && !it.contains(appDatabaseId)) {
                            it.add(appDatabaseId)
                        } else if (updateStatus != 2 && it.contains(appDatabaseId)) {
                            it.remove(appDatabaseId)
                        }
                        return@let
                    }
                }
                setUpdateStatus(holder, false)
                holder.versioningTextView.text = installedVersioning.await()
                        ?: "NEW: ${latestVersioning.await()}" ?: ""
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

    private fun showDialogWindow(holder: CardViewRecyclerViewHolder) {
        val position = holder.adapterPosition
        val itemCardView = mItemCardViewList[position]
        val appDatabaseId = itemCardView.extraData.databaseId
        val app = AppManager.getApp(appDatabaseId)
        val updater = Updater(app.engine)
        val builder = AlertDialog.Builder(holder.versionCheckingBar.context)

        val dialog = builder.setView(R.layout.dialog_app_info).create()
        dialog.show()
        val dialogWindow = dialog.window
        if (dialogWindow != null) {
            val editButton = dialogWindow.findViewById<Button>(R.id.editButton)
            editButton.setOnClickListener {
                // 修改按钮
                val intent = Intent(holder.itemCardView.context, AppSettingActivity::class.java)
                intent.putExtra("database_id", appDatabaseId)
                holder.itemCardView.context.startActivity(intent)
            }
            val localReleaseTextView = dialogWindow.findViewById<TextView>(R.id.localReleaseTextView)
            // 显示本地版本号
            val installedVersion = app.installedVersioning
            if (installedVersion != null)
                localReleaseTextView.text = installedVersion
            else
                localReleaseTextView.text = "获取失败"

            GlobalScope.launch {
                val latestVersionString = async { updater.getLatestVersion() }
                val latestVersionChangelogString = async { updater.getLatestChangelog() }
                val latestFileDownloadUrl = async { updater.getLatestReleaseDownload() }
                runBlocking(Dispatchers.Main) {
                    // 版本号
                    val cloudReleaseTextView = dialogWindow.findViewById<TextView>(R.id.cloudReleaseTextView)
                    cloudReleaseTextView.text = latestVersionString.await()
                    dialogWindow.findViewById<View>(R.id.cloudReleaseProgressBar).visibility = View.GONE// 隐藏等待提醒条

                    // 更新日志
                    if (latestVersionChangelogString.await().isNullOrBlank()) {
                        dialogWindow.findViewById<View>(R.id.releaseChangelogLinearLayout).visibility = View.GONE
                    } else {
                        val changelogTextView = dialogWindow.findViewById<TextView>(R.id.changelogTextView)
                        changelogTextView.text = latestVersionChangelogString.await()
                        dialogWindow.findViewById<View>(R.id.changelogProgressBar).visibility = View.GONE// 隐藏等待提醒条
                    }

                    // 云端文件
                    val itemList = latestFileDownloadUrl.await().keys.toList()
                    if (itemList.isEmpty()) {
                        // 无Release文件，不显示网络文件列表
                        dialogWindow.findViewById<View>(R.id.releaseFileListLinearLayout).visibility = View.GONE
                    } else {
                        // 构建文件列表
                        val adapter = ArrayAdapter(
                                dialog.context, android.R.layout.simple_list_item_1, itemList)
                        val cloudReleaseList = dialogWindow.findViewById<ListView>(R.id.cloudReleaseList)
                        // 设置文件列表点击事件
                        cloudReleaseList.setOnItemClickListener { _, _, i, _ ->
                            // 下载文件
                            GlobalScope.launch { updater.downloadReleaseFile(Pair(0, i)) }
                        }
                        cloudReleaseList.adapter = adapter
                        dialogWindow.findViewById<View>(R.id.fileListProgressBar).visibility = View.GONE// 隐藏等待提醒条
                    }
                }
            }
        }
    }

    companion object {
        private val AppManager = ServerContainer.AppManager
    }
}