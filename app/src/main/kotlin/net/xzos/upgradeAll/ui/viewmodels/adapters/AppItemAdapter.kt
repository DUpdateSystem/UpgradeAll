package net.xzos.upgradeAll.ui.viewmodels.adapters

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.database.RepoDatabase
import net.xzos.upgradeAll.server.ServerContainer
import net.xzos.upgradeAll.ui.activity.AppSettingActivity
import net.xzos.upgradeAll.ui.viewmodels.view.ItemCardView
import net.xzos.upgradeAll.ui.viewmodels.view.holder.CardViewRecyclerViewHolder
import org.json.JSONException
import org.litepal.LitePal
import java.util.*


class AppItemAdapter(private val mContext: Context, private val mItemCardViewList: MutableList<ItemCardView>) : RecyclerView.Adapter<CardViewRecyclerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewRecyclerViewHolder {
        val holder = CardViewRecyclerViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.cardview_item, parent, false))
        // 单击展开 Release 详情页
        holder.itemCardView.setOnClickListener {
            val position = holder.adapterPosition
            val itemCardView = mItemCardViewList[position]
            val appDatabaseId = itemCardView.extraData.databaseId
            val app = AppManager.getApp(appDatabaseId)
            val updater = app.updater
            val builder = AlertDialog.Builder(holder.versionCheckingBar.context)
            val dialog = builder.setView(R.layout.dialog_app_info).create()
            dialog.show()

            val dialogWindow = dialog.window

            if (dialogWindow != null) {
                val cloudReleaseTextView = dialogWindow.findViewById<TextView>(R.id.cloudReleaseTextView)
                val localReleaseTextView = dialogWindow.findViewById<TextView>(R.id.localReleaseTextView)
                val latestVersion = updater.latestVersion
                if (latestVersion != null)
                    cloudReleaseTextView.text = latestVersion
                else
                    cloudReleaseTextView.text = "获取失败"
                // 显示本地版本号
                val installedVersion = app.installedVersion
                if (installedVersion != null)
                    localReleaseTextView.text = installedVersion
                else
                    localReleaseTextView.text = "获取失败"
            }

            // 获取云端文件
            Thread {
                // 刷新数据库
                val latestFileDownloadUrl = updater.latestDownloadUrl
                Handler(Looper.getMainLooper()).post {
                    val itemList = ArrayList<String>()
                    val sIterator = latestFileDownloadUrl.keys()
                    while (sIterator.hasNext()) {
                        val key = sIterator.next()
                        itemList.add(key)
                    }

                    // 无Release文件，不显示网络文件列表
                    if (itemList.size == 0) {
                        dialog.window!!.findViewById<View>(R.id.releaseFileListLinearLayout).visibility = View.GONE
                    }

                    // 构建文件列表
                    val adapter = ArrayAdapter(
                            dialog.context, android.R.layout.simple_list_item_1, itemList)
                    val cloudReleaseList = dialog.window!!.findViewById<ListView>(R.id.cloudReleaseList)
                    // 设置文件列表点击事件
                    cloudReleaseList.setOnItemClickListener { _, _, i, _ ->
                        val intent = Intent(Intent.ACTION_VIEW)
                        var url: String? = null
                        try {
                            url = latestFileDownloadUrl.getString(itemList[i])
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }

                        if (url != null && !url.startsWith("http"))
                            url = "http://$url"
                        intent.data = Uri.parse(url)
                        val chooser = Intent.createChooser(intent, "请选择浏览器")
                        if (intent.resolveActivity(dialog.context.packageManager) != null) {
                            dialog.context.startActivity(chooser)
                        }
                    }
                    cloudReleaseList.adapter = adapter
                    dialog.window!!.findViewById<View>(R.id.fileListProgressBar).visibility = View.INVISIBLE  // 隐藏等待提醒条
                }
            }.start()
        }

        // 长按菜单
        holder.itemCardView.setOnLongClickListener { v ->
            val position = holder.adapterPosition
            val itemCardView = mItemCardViewList[position]
            val appDatabaseId = itemCardView.extraData.databaseId
            val popupMenu = PopupMenu(holder.itemCardView.context, v)
            val menuInflater = popupMenu.menuInflater
            menuInflater.inflate(R.menu.menu_long_click_cardview_item, popupMenu.menu)
            popupMenu.show()
            //设置item的点击事件
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    // 修改按钮
                    R.id.setting_button -> {
                        val intent = Intent(holder.itemCardView.context, AppSettingActivity::class.java)
                        intent.putExtra("database_id", appDatabaseId)
                        holder.itemCardView.context.startActivity(intent)
                    }
                    // 删除按钮
                    R.id.del_button -> {
                        // 删除正在运行的跟踪项
                        AppManager.delApp(appDatabaseId)
                        // 删除数据库
                        LitePal.delete(RepoDatabase::class.java, appDatabaseId.toLong())
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

        // 长按强制检查版本
        holder.versionCheckButton.setOnLongClickListener {
            val position = holder.adapterPosition
            val itemCardView = mItemCardViewList[position]
            val databaseId = itemCardView.extraData.databaseId
            AppManager.getApp(databaseId).updater.renew(false)
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
            holder.itemCardView.visibility = View.GONE
            holder.endTextView.visibility = View.VISIBLE
        } else {
            holder.itemCardView.visibility = View.VISIBLE
            holder.endTextView.visibility = View.GONE
            val appDatabaseId = itemCardView.extraData.databaseId
            holder.name.text = itemCardView.name
            holder.api.text = itemCardView.api
            holder.descTextView.text = itemCardView.desc
            setAppStatusUI(appDatabaseId, holder)
        }
    }

    private fun setAppStatusUI(appDatabaseId: Int, holder: CardViewRecyclerViewHolder) {
        val app = AppManager.getApp(appDatabaseId)
        val updater = app.updater
        val renewing = updater.renewing
        renewing.observe(mContext as LifecycleOwner, Observer { renew ->
            if (renew) {
                holder.versionCheckButton.visibility = View.INVISIBLE
                holder.versionCheckingBar.visibility = View.VISIBLE
            } else {
                holder.versionCheckButton.visibility = View.VISIBLE
                holder.versionCheckingBar.visibility = View.INVISIBLE
                //检查是否取得云端版本号
                if (updater.isSuccessRenew) {
                    // 检查是否获取本地版本号
                    if (app.installedVersion != null) {
                        // 检查本地版本
                        if (app.isLatest) {
                            holder.versionCheckButton.setImageResource(R.drawable.ic_check_latest)
                        } else {
                            holder.versionCheckButton.setImageResource(R.drawable.ic_check_needupdate)
                        }
                    } else {
                        holder.versionCheckButton.setImageResource(R.drawable.ic_local_error)
                    }
                } else {
                    holder.versionCheckButton.setImageResource(R.drawable.ic_404)
                }
            }
        })
    }

    override fun getItemCount(): Int {
        return mItemCardViewList.size
    }

    companion object {

        private val AppManager = ServerContainer.AppManager
    }
}