package net.xzos.upgradeall.ui.detail

import android.annotation.SuppressLint
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.widget.ArrayAdapter
import androidx.lifecycle.MutableLiveData
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Status
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.downloader.DownloadOb
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.module.app.FileAsset
import net.xzos.upgradeall.core.module.app.Version
import net.xzos.upgradeall.databinding.ActivityAppDetailBinding
import net.xzos.upgradeall.server.downloader.startDownload
import net.xzos.upgradeall.ui.data.livedata.AppViewModel
import net.xzos.upgradeall.utils.setValueBackground

@SuppressLint("StaticFieldLeak")
class AppDetailViewModel(
        private val activity: AppDetailActivity, private val binding: ActivityAppDetailBinding,
        private val app: App, private val item: AppDetailItem
) : AppViewModel() {
    init {
        setApp(app)
        appName.observe(activity, { item.appName.set(it) })
        packageName.observe(activity, { item.appPackageId.set(it) })
    }

    private val installedVersionNumber: MutableLiveData<String> by lazy {
        MutableLiveData<String>().apply {
            observe(activity, { item.showingVersionNumber.set(it) })
        }
    }

    val version: String? get() = app.installedVersionNumber

    val downloadData = item.downloadData

    override fun updateData() {
        app.installedVersionNumber?.run {
            installedVersionNumber.setValueBackground(this)
        }
        super.updateData()
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
        if (version == versionList.value?.firstOrNull()) {
            runBlocking {
                app.updater.upgradeApp(taskStartedFun, taskStartFailedFun, downloadOb)
            }
        } else {

        }
    }

    fun setVersionInfo(position: Int) {
        val versionList = versionList.value ?: return
        if (position >= versionList.size) return
        val index = if (position < 0)
            versionList.size + position
        else position
        val versionItem = versionList[index]
        currentVersion = versionItem
        item.selectedVersion = currentVersion
    }

    fun renewMenu() {
        updateData()
        val versionList = versionList.value ?: return
        val versionNumberList = versionList.map { getVersionName(it) }
        val tvMoreVersion = binding.tvMoreVersion
        val oldVersion = tvMoreVersion.text.toString()
        var position = versionNumberList.map { it.toString() }.indexOf(oldVersion)
        if (position == -1) position = 0
        setVersionInfo(position)
        if (position == 0)
            tvMoreVersion.setText(versionNumberList[position], false)
        setVersionAdapter(versionNumberList)
    }

    private fun getVersionName(version: Version): SpannableStringBuilder {
        val versionName = version.name
        val sb = SpannableStringBuilder()
        sb.append(versionName)
        if (version.isIgnored) {
            val colorSpan = ForegroundColorSpan(Color.BLUE)
            sb.setSpan(colorSpan, 0, versionName.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return sb
    }

    private fun setVersionAdapter(versionNumberList: List<SpannableStringBuilder>) {
        val adapter = ArrayAdapter(activity, R.layout.item_more_version, versionNumberList)

        binding.tvMoreVersion.setAdapter(adapter)
    }
}