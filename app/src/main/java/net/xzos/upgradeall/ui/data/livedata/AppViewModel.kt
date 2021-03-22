package net.xzos.upgradeall.ui.data.livedata

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.module.app.Version
import net.xzos.upgradeall.core.utils.android_app.getPackageId
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

    open fun updateData() {
        appName.setValueBackground(app.name)
        packageName.setValueBackground(app.appId.getPackageId()?.second)
        showingVersionNumber.setValueBackground(getShowingVersionNumber())
        versionList.setValueBackground(runBlocking { app.versionList }.asReversed())
    }
}