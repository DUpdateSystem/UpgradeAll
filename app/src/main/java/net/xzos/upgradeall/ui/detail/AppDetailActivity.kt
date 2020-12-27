package net.xzos.upgradeall.ui.detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.NetworkOnMainThreadException
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.text.HtmlCompat
import androidx.core.view.marginBottom
import androidx.core.view.marginTop
import com.absinthe.libraries.utils.utils.UiUtils
import kotlinx.android.synthetic.main.activity_app_detail.*
import kotlinx.android.synthetic.main.layout_appbar.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.route.ReleaseListItem
import net.xzos.upgradeall.core.server_manager.module.app.*
import net.xzos.upgradeall.databinding.ActivityAppDetailBinding
import net.xzos.upgradeall.ui.base.AppBarActivity
import net.xzos.upgradeall.ui.viewmodels.dialog.DownloadListDialog
import net.xzos.upgradeall.utils.IconPalette


class AppDetailActivity : AppBarActivity() {

    private lateinit var binding: ActivityAppDetailBinding
    private lateinit var app: App

    private var versioningPosition: Int = 0
    private val releaseInfoList: List<ReleaseListItem> by lazy {
        return@lazy try {
            runBlocking { app.getReleaseList() } ?: listOf()
        } catch (ignore: NetworkOnMainThreadException) {
            listOf()
        }
    }

    override fun initBinding(): View {
        binding = ActivityAppDetailBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun getAppBar(): Toolbar = binding.toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bundleApp?.run { app = this } ?: onBackPressed()
    }

    override fun initView() {
        binding.btnUpdate.apply {
            layoutParams = (layoutParams as CoordinatorLayout.LayoutParams).apply {
                setMargins(marginStart, marginTop, marginEnd, marginBottom + UiUtils.getNavBarHeight(contentResolver))
            }
        }
        val items = releaseInfoList.map { it.versionNumber }
        val adapter = ArrayAdapter(this, R.layout.item_more_version, items)
        binding.tvMoreVersion.setAdapter(adapter)
        loadAppInfo()
    }

    private fun showDownloadDialog() {
        val fileNameList = if (versioningPosition < releaseInfoList.size)
            releaseInfoList[versioningPosition].assetsList?.map { asset ->
                asset.fileName
            } ?: listOf()
        else listOf()
        DownloadListDialog.show(this, fileNameList,
                fun(position: Int, externalDownloader: Boolean) {
                    GlobalScope.launch {
                        app.downloadReleaseFile(Pair(versioningPosition, position), externalDownloader)
                    }
                })
    }

    private fun loadAppInfo() {
        val appDatabase = app.appDatabase
        val releaseInfoBean = releaseInfoList[versioningPosition]
        val latestChangeLog = releaseInfoBean.changeLog

        binding.tvAppName.text = appDatabase.name
        binding.tvPackageName.text = appDatabase.packageId?.extraString ?: ""
        IconPalette.loadAppIconView(binding.ivAppIcon, app = app)
        app_logo_image_view.let {
            IconPalette.loadAppIconView(it, app = app)
            it.visibility = View.VISIBLE
        }
        val installedVersioning = app.installedVersionNumber
        binding.tvVersion.text = installedVersioning ?: ""

        binding.changelog.text = if (latestChangeLog.isNullOrBlank()) {
            getString(R.string.null_english)
        } else {
            HtmlCompat.fromHtml(latestChangeLog, HtmlCompat.FROM_HTML_MODE_LEGACY)
        }
    }

    companion object {
        private var bundleApp: App? = null

        fun startActivity(context: Context, app: App) {
            bundleApp = app
            context.startActivity(Intent(context, AppDetailActivity::class.java))
        }
    }
}
