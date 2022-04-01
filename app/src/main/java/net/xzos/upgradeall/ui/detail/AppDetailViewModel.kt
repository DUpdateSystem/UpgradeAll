package net.xzos.upgradeall.ui.detail

import android.app.Application
import android.content.Context
import android.text.SpannableStringBuilder
import android.widget.ArrayAdapter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.androidutils.app_info.getPackageId
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.module.app.version.Version
import net.xzos.upgradeall.core.module.app.version_item.FileAsset
import net.xzos.upgradeall.databinding.ActivityAppDetailBinding
import net.xzos.upgradeall.ui.data.livedata.AppViewModel
import net.xzos.upgradeall.utils.setValueBackground
import net.xzos.upgradeall.wrapper.download.startDownload

class AppDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val appViewModel by lazy { AppViewModel() }

    private lateinit var binding: ActivityAppDetailBinding
    private lateinit var item: AppDetailItem
    private lateinit var app: App

    private val versionListLiveData: MutableLiveData<List<Version>> by lazy {
        MutableLiveData<List<Version>>()
    }

    val downloadData get() = item.downloadData

    override fun onCleared() {
        super.onCleared()
        appViewModel.clearObserve()
    }

    fun initData(binding: ActivityAppDetailBinding, item: AppDetailItem, app: App) {
        this.binding = binding
        this.item = item
        this.app = app
        appViewModel.addObserve(app, {}, {}, {
            updateInstalledVersion(it)
            item.appName.set(it.name)
            val packageId = it.appId.getPackageId()?.second
            item.appPackageId.set(packageId)
            item.renewAppIcon(packageId, getApplication())
        }, {
            updateInstalledVersion(it)
            val versionList = it.versionList
            versionListLiveData.setValueBackground(versionList)
            renewVersionList(versionList)
            item.setAppUrl(app)
        })
        appViewModel.updateData()
    }

    private fun updateInstalledVersion(app: App) {
        app.rawInstalledVersionStringList?.run {
            getShowInstalledVersion(this)?.run {
                item.renewVersionItem(app, getApplication())
            }
        }
    }

    private fun getShowInstalledVersion(rawInstalledVersionStringList: List<Pair<Char, Boolean>>?): SpannableStringBuilder? {
        rawInstalledVersionStringList ?: return null
        return getVersionNameSpannableString(
            rawInstalledVersionStringList, null, null
        )
    }

    fun updateData() {
        appViewModel.updateData()
    }

    var currentVersion: Version? = null

    suspend fun download(fileAsset: FileAsset, externalDownload: Boolean) {
        startDownload(
            getApplication(), externalDownload,
            app, fileAsset,
        )
    }

    private lateinit var versionNumberSpannableStringList: List<SpannableStringBuilder>

    fun setVersionInfo(position: Int, context: Context) {
        val versionList = versionListLiveData.value ?: return
        if (position >= versionList.size) return
        val index = if (position < 0)
            versionList.size + position
        else position
        val versionItem = versionList[index]
        currentVersion = versionItem
        setTvMoreVersion(position)
        item.setAssetInfo(currentVersion?.assetList, context)
    }

    private fun setTvMoreVersion(position: Int) {
        val tvMoreVersion = binding.tvMoreVersion
        tvMoreVersion.setText(versionNumberSpannableStringList[position], false)
    }

    private fun renewVersionList(versionList: List<Version>) {
        versionNumberSpannableStringList =
            versionList.map {
                getVersionNameSpannableStringWithRes(
                    it.rawVersionStringList,
                    if (it.isIgnored) R.color.colorPrimary else null, null,
                    getApplication()
                )
            }
        val tvMoreVersion = binding.tvMoreVersion
        val oldVersion = tvMoreVersion.text.toString()
        var position = versionNumberSpannableStringList.map { it.toString() }.indexOf(oldVersion)
        if (position == -1) position = 0
        setVersionInfo(position, getApplication())
        setVersionAdapter(versionNumberSpannableStringList)
    }

    private fun setVersionAdapter(versionNumberList: List<SpannableStringBuilder>) {
        val tvMoreVersion = binding.tvMoreVersion
        val adapter = ArrayAdapter(
            tvMoreVersion.context, R.layout.item_more_version,
            versionNumberList
        )
        tvMoreVersion.setAdapter(adapter)
    }
}