package net.xzos.upgradeall.ui.data.livedata

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.manager.AppManager
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.module.app.Version
import net.xzos.upgradeall.core.utils.android_app.getPackageId
import net.xzos.upgradeall.utils.setValueBackground

open class AppViewModel(application: Application) : AndroidViewModel(application) {
    protected lateinit var app: App

    fun initData(app: App) {
        this.app = app
        updateData()
        initObserve()
    }

    val appName: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    val packageName: MutableLiveData<String?> by lazy {
        MutableLiveData<String?>()
    }

    val versionList: MutableLiveData<List<Version>> by lazy {
        MutableLiveData<List<Version>>()
    }

    protected open fun updateDatabase() {
        appName.setValueBackground(app.name)
        packageName.setValueBackground(app.appId.getPackageId()?.second)
    }

    private fun updateVersionList() {
        versionList.setValueBackground(runBlocking { app.versionList }.asReversed())
    }

    open fun updateData() {
        updateDatabase()
        updateVersionList()
    }

    private val databaseObserver = fun(_: Unit) {
        updateDatabase()
    }

    private val versionObserver = fun(_: Unit) {
        updateVersionList()
    }

    open fun initObserve() {
        AppManager.observeForever(AppManager.getAppChangedNotifyTag(app.appDatabase), databaseObserver)
        AppManager.observeForever(AppManager.getAppUpdatedNotifyTag(app.appDatabase), versionObserver)
    }

    open fun removeObserve() {
        AppManager.removeObserver(databaseObserver)
        AppManager.removeObserver(versionObserver)
    }

    override fun onCleared() {
        super.onCleared()
        removeObserve()
    }
}