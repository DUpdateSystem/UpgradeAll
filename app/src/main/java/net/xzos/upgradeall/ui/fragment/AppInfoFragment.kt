package net.xzos.upgradeall.ui.fragment

import android.content.res.ColorStateList
import android.os.Bundle
import android.os.NetworkOnMainThreadException
import android.view.*
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_app_info.*
import kotlinx.android.synthetic.main.fragment_app_info.view.*
import kotlinx.android.synthetic.main.layout_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.route.ReleaseInfoItem
import net.xzos.upgradeall.core.server_manager.module.app.*
import net.xzos.upgradeall.ui.activity.MainActivity
import net.xzos.upgradeall.ui.activity.MainActivity.Companion.setNavigationItemId
import net.xzos.upgradeall.ui.fragment.setting.AppSettingFragment
import net.xzos.upgradeall.ui.viewmodels.dialog.DownloadListDialog
import net.xzos.upgradeall.utils.IconPalette
import net.xzos.upgradeall.utils.MiscellaneousUtils


/**
 * 更新项详细数据展示页面
 * 作为框架嵌套到 主页[net.xzos.upgradeall.ui.activity.MainActivity]
 * 由点击 更新项 [net.xzos.upgradeall.ui.viewmodels.adapters.AppListItemAdapter] 动作 触发显示
 * 使用 [net.xzos.upgradeall.ui.activity.MainActivity.setFrameLayout] 方法跳转
 */
class AppInfoFragment : Fragment(), Toolbar.OnMenuItemClickListener {
    private lateinit var app: App

    private var versioningPosition: Int = 0
    private val releaseInfoList: List<ReleaseInfoItem> by lazy {
        return@lazy try {
            runBlocking { Updater(app).getReleaseInfo() ?: listOf() }
        } catch (ignore: NetworkOnMainThreadException) {
            listOf<ReleaseInfoItem>()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_app_info, container, false).apply {
                this.placeholderLayout.visibility = View.VISIBLE
            }.also {
                activity?.toolbar?.setOnMenuItemClickListener(this)
            }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        editImageView.setOnClickListener {
            AppSettingFragment.bundleApp = app
            setNavigationItemId(R.id.appSettingFragment)
        }
    }

    override fun onResume() {
        super.onResume()
        checkAppInfo()
        placeholderLayout.visibility = View.GONE
        MainActivity.actionBarDrawerToggle.isDrawerIndicatorEnabled = false  // 禁止开启侧滑栏，启用返回按钮响应事件
        activity?.apply {
            toolbar_backdrop_image.setBackgroundColor(IconPalette.getColorInt(R.color.coolapk_green))
            collapsingToolbarLayout.contentScrim = getDrawable(R.color.coolapk_green)
            addFloatingActionButton.visibility = View.GONE
            floatingActionButton.let { fab ->
                fab.setOnClickListener {
                    showDownloadDialog()
                }
                fab.setImageDrawable(IconPalette.fabDownloadIcon)
                fab.backgroundTintList = ColorStateList.valueOf((IconPalette.getColorInt(R.color.coolapk_green)))
                fab.setColorFilter(IconPalette.getColorInt(R.color.white))
                fab.visibility = View.VISIBLE
            }
        }
    }

    override fun onPause() {
        activity?.toolbar?.menu?.clear()
        super.onPause()
    }

    // 初始化展示的信息
    private fun initUi() {
        initEmptyUi()
        loadAppInfo()
        toastPromptMarkedVersionNumber()
        if (!releaseInfoList.isNullOrEmpty())
            loadAppVersioningInfo(0)
    }

    private fun initEmptyUi() {
        cloudVersioningTextView.setText(R.string.null_english)
        versionMarkImageView.visibility = View.GONE
        versioningSelectLayout.visibility = View.GONE
        appChangelogTextView.setText(R.string.null_english)
    }

    private fun checkAppInfo() {
        bundleApp?.run {
            app = this
        } ?: if (!::app.isLateinit) activity?.onBackPressed()
        initUi()
    }

    private fun loadVersioningPopupMenu() {
        versioningSelectLayout.visibility = View.GONE
        val versionNumberList = releaseInfoList.map {
            it.versionNumber
        }
        val markProcessedVersionNumber = app.markProcessedVersionNumber
        versioningSelectLayout.setOnClickListener { view ->
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
        versioningSelectLayout.visibility = View.VISIBLE
    }

    private fun showDownloadDialog() {
        val fileNameList = if (versioningPosition < releaseInfoList.size)
            releaseInfoList[versioningPosition].assetsList?.map { asset ->
                asset.fileName
            } ?: listOf()
        else listOf()
        context?.let {
            DownloadListDialog.show(it, fileNameList,
                    fun(position: Int, externalDownloader: Boolean) {
                        GlobalScope.launch {
                            Updater(app).downloadReleaseFile(Pair(versioningPosition, position), externalDownloader)
                        }
                    })
        }
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
        activity?.app_logo_image_view?.let {
            IconPalette.loadAppIconView(it, app = app)
            it.visibility = View.VISIBLE
        }
        val installedVersioning = app.installedVersionNumber
        versioningTextView.text = installedVersioning ?: ""
        localVersioningTextView.text = installedVersioning
                ?: getString(R.string.null_english)
    }

    private fun loadAppVersioningInfo(versioningPosition: Int) {
        this.versioningPosition = versioningPosition
        versionMarkImageView.visibility = View.GONE
        val releaseInfoBean = releaseInfoList[versioningPosition]
        val versionNumber = releaseInfoBean.versionNumber
        val latestChangeLog = releaseInfoBean.changeLog
        if (this@AppInfoFragment.isVisible) {
            cloud_versioning_text_view.text = if (versioningPosition == 0) {
                getString(R.string.latest_version_number)
            } else {
                getString(R.string.cloud_version_number)
            }
            cloudVersioningTextView.text = versionNumber
            appChangelogTextView.text = if (latestChangeLog.isNullOrBlank())
                getString(R.string.null_english)
            else latestChangeLog
        }
        renewVersionRelatedItems()
    }

    private fun toastPromptMarkedVersionNumber() {
        GlobalScope.launch {
            if (Updater(app).getUpdateStatus() != Updater.APP_LATEST)
                MiscellaneousUtils.showToast(if (app.markProcessedVersionNumber != null)
                            R.string.marked_version_number_is_behind_latest
                        else R.string.mark_version_number,
                        Toast.LENGTH_LONG
                )
        }
    }

    private fun renewVersionRelatedItems() {
        activity?.run {
            toolbar?.menu?.clear()
            if (app.getParentApplications() != null) {
                if (app.isIgnoreUpdate())
                    toolbar?.menu?.add(Menu.NONE, REMOVE_IGNORE_APP, Menu.NONE, R.string.remove_ignore_app)
                else
                    toolbar?.menu?.add(Menu.NONE, IGNORE_APP, Menu.NONE, R.string.ignore_app)

            }
            if (app.markProcessedVersionNumber == cloudVersioningTextView.text) {
                // 当前版本被标记忽略
                this@AppInfoFragment.versionMarkImageView.visibility = View.VISIBLE
                toolbar?.menu?.add(Menu.NONE, REMOVE_IGNORE_VERSION, Menu.NONE, R.string.remove_ignore_version)
            } else {
                // 当前版本被未被标记
                this@AppInfoFragment.versionMarkImageView.visibility = View.GONE
                toolbar?.menu?.add(Menu.NONE, IGNORE_VERSION, Menu.NONE, R.string.ignore_version)
            }
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
}
