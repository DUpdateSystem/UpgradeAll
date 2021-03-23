package net.xzos.upgradeall.ui.data.livedata

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.manager.AppManager
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.module.app.Version
import net.xzos.upgradeall.core.utils.android_app.getPackageId
import net.xzos.upgradeall.core.utils.oberver.observeWithLifecycleOwner
import net.xzos.upgradeall.utils.setValueBackground

open class AppViewModel : ViewModel() {
    private lateinit var app: App

    val appName: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    val packageName: MutableLiveData<String?> by lazy {
        MutableLiveData<String?>()
    }

    val showingVersionNumber: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    val versionList: MutableLiveData<List<Version>> by lazy {
        MutableLiveData<List<Version>>()
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


    fun setApp(app: App) {
        this.app = app
        updateData()
    }

    private fun updateDatabase() {
        appName.setValueBackground(app.name)
        packageName.setValueBackground(app.appId.getPackageId()?.second)
    }

    private fun updateVersionList() {
        showingVersionNumber.setValueBackground(getShowingVersionNumber())
        versionList.setValueBackground(runBlocking { app.versionList }.asReversed())
    }

    open fun updateData() {
        updateDatabase()
        updateVersionList()
    }

    fun initObserve(owner: LifecycleOwner) {
        AppManager.observeWithLifecycleOwner<Unit>(
                AppManager.getAppChangedNotifyTag(app.appDatabase), owner,
                { updateDatabase() }
        )
        AppManager.observeWithLifecycleOwner<Unit>(
                AppManager.getAppUpdatedNotifyTag(app.appDatabase), owner,
                { updateVersionList() }
        )
    }
}