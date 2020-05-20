package net.xzos.upgradeall.ui.viewmodels.adapters

import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.data.database.AppDatabase.Companion.APP_TYPE_TAG
import net.xzos.upgradeall.core.server_manager.module.BaseApp
import net.xzos.upgradeall.core.server_manager.module.app.App
import net.xzos.upgradeall.data.gson.UIConfig
import net.xzos.upgradeall.ui.activity.MainActivity.Companion.setNavigationItemId
import net.xzos.upgradeall.ui.fragment.AppInfoFragment
import net.xzos.upgradeall.ui.viewmodels.view.ItemCardView
import net.xzos.upgradeall.ui.viewmodels.view.holder.CardViewRecyclerViewHolder
import net.xzos.upgradeall.ui.viewmodels.viewmodel.ApplicationsPageViewModel
import net.xzos.upgradeall.utils.getByHolder

class ApplicationsItemAdapter(
        applicationsPageViewModel: ApplicationsPageViewModel,
        itemCardViewLiveData: LiveData<MutableList<ItemCardView>>,
        owner: LifecycleOwner
) : AppItemAdapter(applicationsPageViewModel, itemCardViewLiveData.value!!) {

    init {
        itemCardViewLiveData.observe(owner, Observer { list ->
            mItemCardViewList = list
            notifyDataSetChanged()
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewRecyclerViewHolder {
        val holder = super.onCreateViewHolder(parent, viewType)
        holder.hubNameTextView.visibility = View.GONE
        // 单击展开 Release 详情页
        holder.itemCardView.setOnClickListener {
            val app = mItemCardViewList.getByHolder(holder).extraData.app
            if (app is App) {
                AppInfoFragment.bundleApp = app
                setNavigationItemId(R.id.appInfoFragment)
            }
        }
        holder.itemCardView.setOnLongClickListener { view ->
            mItemCardViewList.getByHolder(holder).extraData.app?.run {
                val context = view.context
                PopupMenu(context, view).let { popupMenu ->
                    popupMenu.menu.let { menu ->
                        // TODO: 隐藏 APP
                        // 保存
                        menu.add(R.string.save).let { menuItem ->
                            menuItem.setOnMenuItemClickListener {
                                if (this.appDatabase.save(true))
                                    Toast.makeText(context, R.string.save_successfully, Toast.LENGTH_SHORT).show()
                                return@setOnMenuItemClickListener true
                            }
                        }
                        // 保存到其他分组
                        menu.add(R.string.add_to_group).let { menuItem ->
                            menuItem.setOnMenuItemClickListener {
                                showSelectGroupPopMenu(view, this)
                                return@setOnMenuItemClickListener true
                            }
                        }
                        popupMenu.show()
                    }
                }
            }
            return@setOnLongClickListener true
        }
        return holder
    }

    private fun showSelectGroupPopMenu(view: View, app: BaseApp) {
        PopupMenu(view.context, view).let { popupMenu ->
            popupMenu.menu.let { menu ->
                val tabInfoList = UIConfig.uiConfig.userTabList
                for (containerTabListBean in tabInfoList)
                    menu.add(containerTabListBean.name).let { menuItem: MenuItem ->
                        menuItem.setOnMenuItemClickListener {
                            if (app.appDatabase.save(true))
                                containerTabListBean.itemList.add(
                                        UIConfig.CustomContainerTabListBean.ItemListBean(
                                                APP_TYPE_TAG,
                                                app.appDatabase.name,
                                                mutableListOf(app.appDatabase.id)
                                        )
                                )
                            return@setOnMenuItemClickListener true
                        }
                    }
            }
            popupMenu.show()
        }
    }
}
