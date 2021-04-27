package net.xzos.upgradeall.ui.detail

import android.app.Application
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Status
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.downloader.DownloadOb
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.module.app.FileAsset
import net.xzos.upgradeall.core.module.app.Version
import net.xzos.upgradeall.core.utils.android_app.getPackageId
import net.xzos.upgradeall.databinding.ActivityAppDetailBinding
import net.xzos.upgradeall.server.downloader.startDownload
import net.xzos.upgradeall.ui.data.livedata.AppViewModel
import net.xzos.upgradeall.utils.setValueBackground

class AppDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val appViewModel by lazy { AppViewModel() }

    private lateinit var binding: ActivityAppDetailBinding
    private lateinit var item: AppDetailItem
    private lateinit var app: App

    private val installedVersionNumber: MutableLiveData<String> by lazy {
        MutableLiveData<String>().apply {
            observeForever {
                item.setInstallViewNumber(it)
            }
        }
    }

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
        app.installedVersionNumber?.run {
            installedVersionNumber.setValueBackground(this)
        }
    }

    fun updateData() {
        appViewModel.updateData()
    }

    // 下载状态信息
    private val obFun = fun(d: Download) { downloadData.setDownload(d) }
    val waitDownload = {
        downloadData.setDownloadProgress(0)
        downloadData.setDownloadStatus(Status.NONE)
    }

    val failDownload = fun(_: Throwable) {
        downloadData.setDownloadProgress(-1)
        downloadData.setDownloadStatus(Status.FAILED)
    }

    fun getDownloadDataOb() = DownloadOb(obFun, obFun, obFun, obFun, obFun, obFun)

    var currentVersion: Version? = null

    suspend fun download(fileAsset: FileAsset, externalDownload: Boolean) {
        startDownload(fileAsset, fun(_) { waitDownload() }, failDownload, getDownloadDataOb(), getApplication(), externalDownload)
    }

    private var versionNumberSpannableStringList: List<SpannableStringBuilder> = listOf()

    fun setVersionInfo(position: Int) {
        val versionList = versionListLiveData.value ?: return
        if (position >= versionList.size) return
        val index = if (position < 0)
            versionList.size + position
        else position
        val versionItem = versionList[index]
        currentVersion = versionItem
        setTvMoreVersion(position)
        item.setAssetInfo(currentVersion?.assetList)
    }

    private fun setTvMoreVersion(position: Int) {
        val tvMoreVersion = binding.tvMoreVersion
        tvMoreVersion.setText(versionNumberSpannableStringList[position], false)
    }

    private fun renewVersionList(versionList: List<Version>) {
        versionNumberSpannableStringList = versionList.map { getVersionNameSpannableString(it) }
        val tvMoreVersion = binding.tvMoreVersion
        val oldVersion = tvMoreVersion.text.toString()
        var position = versionNumberSpannableStringList.map { it.toString() }.indexOf(oldVersion)
        if (position == -1) position = 0
        setVersionInfo(position)
        setVersionAdapter(versionNumberSpannableStringList)
    }

    private fun getVersionNameSpannableString(version: Version): SpannableStringBuilder {
        val rawVersionStringList = version.rawVersionStringList
        val focusColor = if (version.isIgnored)
            ContextCompat.getColor(getApplication(), R.color.colorPrimary)
        else null
        val sb = SpannableStringBuilder()
        rawVersionStringList.forEach {
            setVersionNumberSpannableStringBuilder(it.first.toString(), sb, it.second, focusColor)
        }
        return sb
    }

    private fun setVersionNumberSpannableStringBuilder(s: String, sb: SpannableStringBuilder, focus: Boolean, focusColor: Int? = null) {
        sb.append(s)
        val color = if (focus && focusColor != null) focusColor
        else if (!focus)
            ContextCompat.getColor(getApplication(), R.color.text_low_priority_color)
        else null
        color?.run {
            val newLength = sb.length
            sb.setSpan(ForegroundColorSpan(this), newLength - s.length, newLength, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        }
    }

    private fun setVersionAdapter(versionNumberList: List<SpannableStringBuilder>) {
        val tvMoreVersion = binding.tvMoreVersion
        val adapter = ArrayAdapter(tvMoreVersion.context, R.layout.item_more_version, versionNumberList)
        tvMoreVersion.setAdapter(adapter)
    }
}