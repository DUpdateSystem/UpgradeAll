package net.xzos.upgradeall.ui.viewmodels.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.ui.viewmodels.view.ItemCardView
import net.xzos.upgradeall.utils.mutableLiveDataOf
import net.xzos.upgradeall.utils.setValueBackground

abstract class AppListContainerViewModel : ViewModel() {
    val appListLiveData: MutableLiveData<List<App>> = mutableLiveDataOf()  // 列表中所有的 APP

    internal fun setAppList(list: List<App>) {
        appListLiveData.setValueBackground(list)
    }

    internal val needUpdateAppsLiveData = mutableLiveDataOf<MutableList<App>>().also {
        it.setValueBackground(mutableListOf())
    }  // 需要升级的 APP
    private val context = MyApplication.context

    // 列表中所有的 APP 项的信息
    internal val appCardViewList: LiveData<MutableList<ItemCardView>> by lazy {
        Transformations.map(appListLiveData) { apps ->
            return@map apps.map {
                ItemCardView(it)
            }.toMutableList()
        }
    }
}