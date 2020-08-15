package net.xzos.upgradeall.ui.activity.detail

import android.content.Intent
import android.os.Bundle
import android.os.NetworkOnMainThreadException
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import jonathanfinerty.once.Once
import kotlinx.android.synthetic.main.activity_app_detail.*
import kotlinx.android.synthetic.main.fragment_app_info.appChangelogTextView
import kotlinx.android.synthetic.main.fragment_app_info.appIconImageView
import kotlinx.android.synthetic.main.fragment_app_info.appModuleName
import kotlinx.android.synthetic.main.fragment_app_info.appUrlTextView
import kotlinx.android.synthetic.main.fragment_app_info.cloudVersioningTextView
import kotlinx.android.synthetic.main.fragment_app_info.cloud_versioning_text_view
import kotlinx.android.synthetic.main.fragment_app_info.ib_edit
import kotlinx.android.synthetic.main.fragment_app_info.localVersioningTextView
import kotlinx.android.synthetic.main.fragment_app_info.nameTextView
import kotlinx.android.synthetic.main.fragment_app_info.versionMarkImageView
import kotlinx.android.synthetic.main.fragment_app_info.versioningTextView
import kotlinx.android.synthetic.main.layout_appbar.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.route.ReleaseInfoItem
import net.xzos.upgradeall.core.server_manager.module.app.*
import net.xzos.upgradeall.data.constants.OnceTag
import net.xzos.upgradeall.ui.activity.BaseActivity
import net.xzos.upgradeall.ui.viewmodels.dialog.DownloadListDialog
import net.xzos.upgradeall.utils.IconPalette
import net.xzos.upgradeall.utils.MiscellaneousUtils
import net.xzos.upgradeall.utils.ToastUtil

class AppDetailActivity : BaseActivity(), Toolbar.OnMenuItemClickListener {

    private lateinit var app: App

    private var versioningPosition: Int = 0
    private val releaseInfoList: List<ReleaseInfoItem> by lazy {
        return@lazy try {
            runBlocking { Updater(app).getReleaseInfo() ?: listOf() }
        } catch (ignore: NetworkOnMainThreadException) {
            listOf<ReleaseInfoItem>()
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

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        bundleApp?.run {
            app = this
        } ?: if (!::app.isLateinit) onBackPressed()
        initUi()

        toolbar.setOnMenuItemClickListener(this)
        ib_edit.setOnClickListener {
            AppSettingActivity.bundleApp = app
            startActivity(Intent(this, AppSettingActivity::class.java))
        }
        floatingActionButton.setOnClickListener {
            showDownloadDialog()
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            IGNORE_VERSION -> {
                app.setIgnoreUpdate(cloudVersioningTextView.text.toString())
            }
            REMOVE_IGNORE_VERSION -> {
                app.removeIgnoreUpdate()
            }
            IGNORE_APP -> {
                app.setIgnoreUpdate()
            }
            REMOVE_IGNORE_APP -> {
                app.removeIgnoreUpdate()
            }
        }
        renewVersionRelatedItems()
        return true
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
        }
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
        val markProcessedVersionNumber = app.markProcessedVersionNumber
        tv_more_editions.setOnClickListener { view ->
            // 选择版本号
            PopupMenu(view.context, view).let { popupMenu ->
                for (i in versionNumberList.indices)
                    popupMenu.menu.add(versionNumberList[i].plus(
                            if (markProcessedVersionNumber == versionNumberList[i])
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
        appModuleName.text = appDatabase.targetChecker?.extraString ?: ""
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

        renewVersionRelatedItems()
    }

    private fun toastPromptMarkedVersionNumber() {
        lifecycleScope.launch {
            if (Updater(app).getUpdateStatus() != Updater.APP_LATEST) {
                if (app.markProcessedVersionNumber != null) {
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

    private fun renewVersionRelatedItems() {
        toolbar.menu.clear()
        if (app.getParentApplications() != null) {
            if (app.isIgnoreUpdate())
                toolbar.menu.add(Menu.NONE, REMOVE_IGNORE_APP, Menu.NONE, R.string.remove_ignore_app)
            else
                toolbar.menu.add(Menu.NONE, IGNORE_APP, Menu.NONE, R.string.ignore_app)
        }

        if (app.markProcessedVersionNumber == cloudVersioningTextView.text) {
            // 当前版本被标记忽略
            versionMarkImageView.visibility = View.VISIBLE
            toolbar.menu.add(Menu.NONE, REMOVE_IGNORE_VERSION, Menu.NONE, R.string.remove_ignore_version)
        } else {
            // 当前版本被未被标记
            versionMarkImageView.visibility = View.GONE
            toolbar.menu.add(Menu.NONE, IGNORE_VERSION, Menu.NONE, R.string.ignore_version)
        }
        loadVersioningPopupMenu()
    }

    companion object {
        internal var bundleApp: App? = null
            get() {
                val app = field
                field = null
                return app
            }

        private const val IGNORE_APP = Menu.FIRST
        private const val REMOVE_IGNORE_APP = IGNORE_APP + 1
        private const val IGNORE_VERSION = REMOVE_IGNORE_APP + 1
        private const val REMOVE_IGNORE_VERSION = IGNORE_VERSION + 1
    }
}