package net.xzos.upgradeAll.ui.viewmodels.callback

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeAll.data.database.litepal.RepoDatabase
import net.xzos.upgradeAll.server.ServerContainer.Companion.AppManager
import net.xzos.upgradeAll.ui.viewmodels.adapters.AppItemAdapter
import org.litepal.LitePal


class AppItemTouchHelperCallback(private val mAdapter: AppItemAdapter) : ItemTouchHelper.Callback() {

    override fun isLongPressDragEnabled(): Boolean {
        return true
    }

    override fun isItemViewSwipeEnabled(): Boolean {
        return true
    }

    override fun getMovementFlags(recyclerView: RecyclerView,
                                  viewHolder: ViewHolder): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        val swipeFlags = ItemTouchHelper.START or ItemTouchHelper.END
        return makeMovementFlags(dragFlags, swipeFlags)
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: ViewHolder,
                        target: ViewHolder): Boolean {
        mAdapter.onItemMove(viewHolder.adapterPosition, target.adapterPosition)
        return true
    }

    override fun onSwiped(viewHolder: ViewHolder, direction: Int) {
        val adapterPosition = viewHolder.adapterPosition
        val removedItemCardView = mAdapter.onItemDismiss(adapterPosition)
        if (removedItemCardView != null) {
            val appDatabaseId = removedItemCardView.extraData.databaseId
            val repoDatabase: RepoDatabase? = LitePal.find(RepoDatabase::class.java, appDatabaseId)
            if (repoDatabase != null) {
                AppManager.delApp(appDatabaseId) // 删除正在运行的跟踪项
                GlobalScope.launch(Dispatchers.IO) {
                    repoDatabase.delete() // 删除数据库
                    runBlocking(Dispatchers.Main) {
                        Snackbar.make(viewHolder.itemView, "App 项已删除", Snackbar.LENGTH_INDEFINITE)
                                .setAction("撤销") {
                                    runBlocking(Dispatchers.IO) {
                                        repoDatabase.save() // 恢复数据库
                                        val newAppDatabaseId = repoDatabase.id
                                        AppManager.setApp(newAppDatabaseId) // 恢复正在运行的跟踪项
                                        removedItemCardView.extraData.databaseId = newAppDatabaseId
                                    }
                                    mAdapter.onAddItem(adapterPosition, removedItemCardView)
                                }.show()
                    }
                }
            }
        }
    }
}