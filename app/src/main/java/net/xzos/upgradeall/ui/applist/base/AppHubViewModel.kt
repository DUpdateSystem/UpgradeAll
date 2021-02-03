package net.xzos.upgradeall.ui.applist.base

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import net.xzos.upgradeall.core.manager.AppManager
import net.xzos.upgradeall.core.module.app.Updater
import net.xzos.upgradeall.utils.setValueBackground


class AppHubViewModel(application: Application) : AndroidViewModel(application) {

    val itemCountLiveData: MutableLiveData<Int> by lazy {
        MutableLiveData(0).also {
            AppManager.appMapStatusChangedFun = { map ->
                val appType = mAppType.value
                val needUpdateNum = map[Updater.APP_OUTDATED]?.filter {
                    it.appId.containsKey(appType)
                }?.size ?: 0
                itemCountLiveData.setValueBackground(needUpdateNum)
            }
        }
    }

    private val mAppType = MutableLiveData<String>()

    internal fun setAppType(appType: String) {
        mAppType.value = appType
    }
}