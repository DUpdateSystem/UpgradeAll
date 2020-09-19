package net.xzos.upgradeall.ui.activity.detail

import android.content.res.ColorStateList
import android.os.Bundle
import android.os.NetworkOnMainThreadException
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import jonathanfinerty.once.Once
import kotlinx.android.synthetic.main.activity_app_detail.*
import kotlinx.android.synthetic.main.layout_appbar.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.route.ReleaseListItem
import net.xzos.upgradeall.core.server_manager.module.app.*
import net.xzos.upgradeall.data.constants.OnceTag
import net.xzos.upgradeall.ui.activity.BaseActivity
import net.xzos.upgradeall.ui.activity.detail.setting.AppSettingActivity
import net.xzos.upgradeall.ui.viewmodels.dialog.DownloadListDialog
import net.xzos.upgradeall.utils.IconPalette
import net.xzos.upgradeall.utils.MiscellaneousUtils
import net.xzos.upgradeall.utils.ToastUtil


class AppDetailActivity : BaseActivity() {

    private lateinit var app: App

    private var versioningPosition: Int = 0
    private val releaseInfoList: List<ReleaseListItem> by lazy {
        return@lazy try {
            runBlocking { Updater(app).getReleaseList()?.filterNotNull() } ?: listOf()
        } catch (ignore: NetworkOnMainThreadException) {
            listOf()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_detail)
        initView()
    }

    private fun initView() {
        val color = ContextCompat.getColor(this, R.color.coolapk_green)
        window.statusBarColor = color
        collapsingToolbarLayout.setContentScrimColor(color)
        toolbar_backdrop_image.setBackgroundColor(color)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        bundleApp?.run { app = this }
        initUi()

        ib_edit.setOnClickListener {
            AppSettingActivity.getInstance(this, app.appDatabase)
        }
        floatingActionButton.apply {
            setImageDrawable(IconPalette.fabDownloadIcon)
            backgroundTintList = ColorStateList.valueOf((IconPalette.getColorInt(R.color.coolapk_green)))
            setColorFilter(IconPalette.getColorInt(R.color.white))
            setOnClickListener {
                showDownloadDialog()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initUi() {
        initEmptyUi()
        loadAppInfo()
        toastPromptMarkedVersionNumber()
        if (releaseInfoList.isNotEmpty()) {
            loadAppVersioningInfo(0)
            loadVersioningPopupMenu()
            cloudVersioningTextView.setOnClickListener {
                loadReleaseIgnorePopupMenu()
            }
        }
    }

    private fun loadReleaseIgnore() {
        versionMarkImageView.visibility =
                if (app.ignoreVersionNumber == cloudVersioningTextView.text) {
                    // 当前版本被标记忽略
                    View.VISIBLE
                } else View.GONE
    }

    private fun initEmptyUi() {
        cloudVersioningTextView.setText(R.string.null_english)
        versionMarkImageView.visibility = View.GONE
        tv_more_editions.visibility = View.GONE
        appChangelogTextView.setText(R.string.null_english)
    }

    private fun loadVersioningPopupMenu() {
        tv_more_editions.visibility = View.GONE
        val versionNumberList = releaseInfoList.map {
            it.versionNumber
        }
        tv_more_editions.setOnClickListener { view ->
            // 选择版本号
            PopupMenu(view.context, view).let { popupMenu ->
                for (i in versionNumberList.indices)
                    popupMenu.menu.add(versionNumberList[i].plus(
                            if (app.ignoreVersionNumber == versionNumberList[i])
                                getString(R.string.o_mark)
                            else ""
                    )).let {
                        it.setOnMenuItemClickListener {
                            loadAppVersioningInfo(i)
                            true
                        }
                    }
                popupMenu.show()
            }
        }
        tv_more_editions.visibility = View.VISIBLE
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
                        Updater(app).downloadReleaseFile(Pair(versioningPosition, position), externalDownloader)
                    }
                })
    }

    private fun loadAppInfo() {
        val appDatabase = app.appDatabase
        nameTextView.text = appDatabase.name
        appModuleName.text = appDatabase.packageId?.extraString ?: ""
        val url = appDatabase.url
        with(appUrlTextView) {
            text = url
            // 打开指向Url
            setOnClickListener { MiscellaneousUtils.accessByBrowser(url, context) }
        }
        IconPalette.loadAppIconView(appIconImageView, app = app)
        app_logo_image_view.let {
            IconPalette.loadAppIconView(it, app = app)
            it.visibility = View.VISIBLE
        }
        val installedVersioning = app.installedVersionNumber
        versioningTextView.text = installedVersioning ?: ""
        localVersioningTextView.text = installedVersioning ?: getString(R.string.null_english)
    }

    private fun loadAppVersioningInfo(versioningPosition: Int) {
        this.versioningPosition = versioningPosition
        versionMarkImageView.visibility = View.GONE
        val releaseInfoBean = releaseInfoList[versioningPosition]
        val versionNumber = releaseInfoBean.versionNumber
        val latestChangeLog = releaseInfoBean.changeLog

        cloud_versioning_text_view.text = if (versioningPosition == 0) {
            getString(R.string.latest_version_number)
        } else {
            getString(R.string.cloud_version_number)
        }
        cloudVersioningTextView.text = versionNumber
        appChangelogTextView.text = if (latestChangeLog.isNullOrBlank()) {
            getString(R.string.null_english)
        } else {
            latestChangeLog
        }

        loadReleaseIgnore()
    }

    private fun toastPromptMarkedVersionNumber() {
        lifecycleScope.launch {
            if (Updater(app).getUpdateStatus() != Updater.APP_LATEST) {
                if (app.ignoreVersionNumber != null) {
                    ToastUtil.makeText(R.string.marked_version_number_is_behind_latest, Toast.LENGTH_LONG)
                } else {
                    if (!Once.beenDone(Once.THIS_APP_INSTALL, OnceTag.APP_INFO_TOOLBAR_MENU_TIP)) {
                        ToastUtil.makeText(R.string.mark_version_number, Toast.LENGTH_LONG)
                        Once.markDone(OnceTag.APP_INFO_TOOLBAR_MENU_TIP)
                    }
                }
            }
        }
    }

    private fun loadReleaseIgnorePopupMenu() {
        PopupMenu(cloudVersioningTextView.context, cloudVersioningTextView).also { popupMenu ->
            if (app.getParentApplications() != null) {
                if (app.isIgnoreUpdate())
                    popupMenu.menu.add(R.string.remove_ignore_app).let {
                        it.setOnMenuItemClickListener {
                            app.removeIgnoreUpdate()
                            true
                        }
                    }
                else
                    popupMenu.menu.add(R.string.ignore_app).let {
                        it.setOnMenuItemClickListener {
                            app.setIgnoreUpdate()
                            true
                        }
                    }
            }

            if (app.ignoreVersionNumber == cloudVersioningTextView.text) {
                // 当前版本被标记忽略
                versionMarkImageView.visibility = View.VISIBLE
                popupMenu.menu.add(R.string.remove_ignore_version).let {
                    it.setOnMenuItemClickListener {
                        app.removeIgnoreUpdate()
                        versionMarkImageView.visibility = View.GONE
                        true
                    }
                }
            } else {
                // 当前版本被未被标记
                versionMarkImageView.visibility = View.GONE
                popupMenu.menu.add(R.string.ignore_version).let {
                    it.setOnMenuItemClickListener {
                        app.setIgnoreUpdate(cloudVersioningTextView.text.toString())
                        versionMarkImageView.visibility = View.VISIBLE
                        true
                    }
                }
            }
        }.show()
    }

    companion object {
        internal var bundleApp: App? = null
            get() {
                val app = field
                field = null
                return app
            }
    }
}
