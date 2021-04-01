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
import kotlinx.coroutines.runBlocking
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

    val failDownload = {
        downloadData.setDownloadProgress(-1)
        downloadData.setDownloadStatus(Status.FAILED)
    }

    fun getDownloadDataOb() = DownloadOb(obFun, obFun, obFun, obFun, obFun, obFun)

    var currentVersion: Version? = null

    suspend fun download(fileAsset: FileAsset) {
        startDownload(fileAsset, fun(_) { waitDownload() }, failDownload, getDownloadDataOb())
    }

    fun clickDownload(
            taskStartedFun: (Int) -> Unit,
            taskStartFailedFun: () -> Unit,
            downloadOb: DownloadOb,
    ) {
        val version = currentVersion ?: return
        if (version == versionListLiveData.value?.firstOrNull()) {
            runBlocking {
                app.updater.upgradeApp(taskStartedFun, taskStartFailedFun, downloadOb)
            }
        } else {

        }
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
        val (prefixString, versionName, suffixString) = version.getShowName()
        var sb = SpannableStringBuilder()
        val fixColor = ContextCompat.getColor(getApplication(), R.color.text_low_priority_color)
        val prefixColorSpan = ForegroundColorSpan(fixColor)
        sb = setSpannableStringBuilder(prefixColorSpan, prefixString, sb)
        if (version.isIgnored) {
            val versionColor = ContextCompat.getColor(getApplication(), R.color.colorPrimary)
            val colorSpan = ForegroundColorSpan(versionColor)
            sb = setSpannableStringBuilder(colorSpan, versionName, sb)
        } else {
            sb.append(versionName)
        }
        val suffixColorSpan = ForegroundColorSpan(fixColor)
        sb = setSpannableStringBuilder(suffixColorSpan, suffixString, sb)
        return sb
    }

    private fun setSpannableStringBuilder(cs: ForegroundColorSpan, s: String, sb: SpannableStringBuilder): SpannableStringBuilder {
        val oldLength = sb.length
        sb.append(s)
        sb.setSpan(cs, oldLength, oldLength + s.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return sb
    }

    private fun setVersionAdapter(versionNumberList: List<SpannableStringBuilder>) {
        val tvMoreVersion = binding.tvMoreVersion
        val adapter = ArrayAdapter(tvMoreVersion.context, R.layout.item_more_version, versionNumberList)
        tvMoreVersion.setAdapter(adapter)
    }

    companion object {
        fun Version.getShowName(): Triple<String, String, String> {
            val prefixList = mutableSetOf<String>()
            val suffixList = mutableSetOf<String>()
            assetList.forEach {
                val list = it.versionNumber.split(name, limit = 2)
                list.firstOrNull()?.run {
                    if (isNotBlank()) prefixList.add(this)
                }
                list.lastOrNull()?.run {
                    if (this.isNotBlank()) suffixList.add(this)
                }
            }
            val prefixString = if (prefixList.isNotEmpty())
                prefixList.joinToString(separator = "/")
            else ""
            val suffixString = if (suffixList.isNotEmpty())
                suffixList.joinToString(separator = "/")
            else ""
            return Triple(prefixString, name, suffixString)
        }
    }
}