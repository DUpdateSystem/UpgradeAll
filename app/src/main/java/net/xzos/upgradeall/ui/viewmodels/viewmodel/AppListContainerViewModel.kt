package net.xzos.upgradeall.ui.viewmodels.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.dupdatesystem.core.server_manager.module.BaseApp
import net.xzos.upgradeall.ui.viewmodels.view.ItemCardView
import net.xzos.upgradeall.ui.viewmodels.view.ItemCardViewExtraData

abstract class AppListContainerViewModel : ViewModel() {
    internal val needUpdateAppsLiveData = MutableLiveData(mutableListOf<BaseApp>())  // 需要升级的 APP
    private val appListLiveData = MutableLiveData(mutableListOf<BaseApp>())  // 列表中所有的 APP

    fun setApps(apps: List<BaseApp>) {
        GlobalScope.launch(Dispatchers.Main) {
            appListLiveData.value = apps.toMutableList()
        }
    }

    // 列表中所有的 APP 项的信息
    internal val appCardViewList = Transformations.map(appListLiveData) { apps ->
        return@map mutableListOf<ItemCardView>().apply {
            for (app in apps) {
                this.add(getAppItemCardView(app))
            }
            if (apps.isNotEmpty()) {
                this.add(ItemCardView())
            }
        }
    }

    private fun getAppItemCardView(app: BaseApp): ItemCardView {
        return ItemCardView(
                app.appInfo.name,
                app.appInfo.url,
                extraData = ItemCardViewExtraData(app = app)
        )
    }
}
