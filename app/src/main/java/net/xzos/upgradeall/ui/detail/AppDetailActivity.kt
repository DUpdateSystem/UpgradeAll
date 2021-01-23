package net.xzos.upgradeall.ui.detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.text.HtmlCompat
import androidx.core.view.marginBottom
import androidx.core.view.marginTop
import com.absinthe.libraries.utils.utils.UiUtils
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.module.app.Version
import net.xzos.upgradeall.core.utils.getPackageId
import net.xzos.upgradeall.databinding.ActivityAppDetailBinding
import net.xzos.upgradeall.ui.base.AppBarActivity
import net.xzos.upgradeall.utils.IconPalette


class AppDetailActivity : AppBarActivity() {

    private lateinit var binding: ActivityAppDetailBinding
    private lateinit var app: App

    private var versioningPosition: Int = 0
    private val versionList: List<Version> by lazy { runBlocking { app.versionList } }

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
        val items = versionList.map { it.name }
        val adapter = ArrayAdapter(this, R.layout.item_more_version, items)
        binding.tvMoreVersion.setAdapter(adapter)
        loadAppInfo()
    }

    private fun loadAppInfo() {
        val version = versionList[versioningPosition]
        var latestChangeLog = ""
        for (asset in version.assetList) {
            asset.changeLog?.run {
                latestChangeLog += "${asset.hub.name}\n$this\n"
            }
        }

        binding.tvAppName.text = app.name
        binding.tvPackageName.text = app.appId.getPackageId()?.second ?: ""
        IconPalette.loadAppIconView(binding.ivAppIcon, app = app)
        binding.ivAppIcon.let {
            IconPalette.loadAppIconView(it, app = app)
            it.visibility = View.VISIBLE
        }
        val installedVersioning = app.installedVersionNumber
        binding.tvVersion.text = installedVersioning ?: ""

        binding.changelog.text = if (latestChangeLog.isNotBlank()) {
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
