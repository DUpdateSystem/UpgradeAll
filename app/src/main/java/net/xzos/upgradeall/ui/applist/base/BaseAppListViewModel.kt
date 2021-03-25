package net.xzos.upgradeall.ui.applist.base

import android.app.Application
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.ui.data.livedata.AppViewModel
import net.xzos.upgradeall.utils.setValueBackground

abstract class BaseAppListViewModel(private val _application: Application) : AppViewModel(_application) {
    private lateinit var item: BaseAppListItemView
    override fun initObserve(owner: LifecycleOwner) {
        super.initObserve(owner)
        versionList.observe(owner, { renewVersionNumber() })
        appName.observe(owner, { item.appName.set(it) })
        packageName.observe(owner, { item.renewAppIcon(it, _application) })
    }

    fun initData(item: BaseAppListItemView, app: App) {
        this.item = item
        initData(app)
    }

    private val showingVersionNumber: MutableLiveData<String> by lazy {
        MutableLiveData<String>().apply {
            observeForever { item.showingVersionNumber.set(it) }
        }
    }

    override fun updateDatabase() {
        super.updateDatabase()
        renewVersionNumber()
    }

    private fun renewVersionNumber() {
        showingVersionNumber.setValueBackground(getShowingVersionNumber())
    }

    private fun getShowingVersionNumber(): String {
        val latestVersionNumber = app.getLatestVersionNumber()
        val installedVersionNumber = app.installedVersionNumber
        return when {
            installedVersionNumber == null -> latestVersionNumber ?: ""
            latestVersionNumber == installedVersionNumber -> installedVersionNumber
            else -> "$installedVersionNumber > $latestVersionNumber"
        }
    }
}