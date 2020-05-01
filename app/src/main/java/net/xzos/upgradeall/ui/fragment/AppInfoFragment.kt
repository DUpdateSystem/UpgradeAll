package net.xzos.upgradeall.ui.fragment

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.fragment_app_info.*
import kotlinx.android.synthetic.main.fragment_app_info.placeholderLayout
import kotlinx.android.synthetic.main.fragment_app_info.view.*
import kotlinx.android.synthetic.main.layout_main.*
import kotlinx.android.synthetic.main.list_content.*
import kotlinx.coroutines.*
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.route.ReleaseInfoItem
import net.xzos.upgradeall.core.server_manager.module.app.App
import net.xzos.upgradeall.core.server_manager.module.app.Updater
import net.xzos.upgradeall.ui.activity.MainActivity
import net.xzos.upgradeall.ui.activity.MainActivity.Companion.setNavigationItemId
import net.xzos.upgradeall.utils.IconPalette
import net.xzos.upgradeall.utils.MiscellaneousUtils


/**
 * 更新项详细数据展示页面
 * 作为框架嵌套到 主页[net.xzos.upgradeall.ui.activity.MainActivity]
 * 由点击 更新项 [net.xzos.upgradeall.ui.viewmodels.adapters.AppListItemAdapter] 动作 触发显示
 * 使用 [net.xzos.upgradeall.ui.activity.MainActivity.setFrameLayout] 方法跳转
 */
class AppInfoFragment : Fragment() {
    private lateinit var app: App

    private var versioningPosition: Int = 0
    private var releaseInfoList: List<ReleaseInfoItem>? = null
        get() {
            return field ?: runBlocking {
                Updater(app).getReleaseInfo().also {
                    field = it
                }
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_app_info, container, false).apply {
                this.placeholderLayout.visibility = View.VISIBLE
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

    // 初始化展示的信息
    private fun initUi() {
        loadAllAppInfo()
        toastPromptMarkedVersionNumber()
        if (!releaseInfoList.isNullOrEmpty())
            loadAppVersioningInfo(0)
    }

    private fun checkAppInfo() {
        bundleApp?.run {
            app = this
            initUi()
        } ?: if (!::app.isLateinit) activity?.onBackPressed()
    }

    private fun loadVersioningPopupMenu() {
        versioningSelectLayout.visibility = View.GONE
        GlobalScope.launch {
            val versionNumberList = releaseInfoList!!.map {
                it.versionNumber
            }

            withContext(Dispatchers.Main) {
                if (this@AppInfoFragment.isVisible) {
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
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun showDownloadDialog() {
        val versioningPosition = this.versioningPosition
        context?.let {
            BottomSheetDialog(it).let { dialog ->
                dialog.setContentView(
                        layoutInflater.inflate(R.layout.list_content, null)
                )
                dialog.show()
                Toast.makeText(context, R.string.long_click_to_use_external_downloader, Toast.LENGTH_LONG).show()
                dialog.placeholderLayout.visibility = View.VISIBLE
                GlobalScope.launch {
                    val nameList = releaseInfoList?.run {
                        if (this.isNotEmpty()) {
                            this[versioningPosition].assetsList.map { asset ->
                                asset.fileName
                            }
                        } else listOf()
                    } ?: listOf()
                    withContext(Dispatchers.Main) {
                        if (this@AppInfoFragment.isVisible) {
                            if (nameList.isNotEmpty()) {
                                dialog.list.let { list ->
                                    list.adapter =
                                            ArrayAdapter(dialog.context, android.R.layout.simple_list_item_1, nameList)
                                    // 下载文件
                                    list.setOnItemClickListener { _, _, position, _ ->
                                        GlobalScope.launch {
                                            Updater(app).downloadReleaseFile(Pair(versioningPosition, position))
                                        }
                                    }
                                    list.setOnItemLongClickListener { _, _, position, _ ->
                                        GlobalScope.launch {
                                            Updater(app).downloadReleaseFile(Pair(versioningPosition, position), externalDownloader = true)
                                        }
                                        return@setOnItemLongClickListener true
                                    }
                                }
                            } else {
                                dialog.emptyPlaceHolderTextView.visibility = View.VISIBLE
                            }
                            dialog.placeholderLayout.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    private fun loadAllAppInfo() {
        app.appDatabase.let { appDatabase ->
            GlobalScope.launch {
                val installedVersioning = app.installedVersionNumber
                withContext(Dispatchers.Main) {
                    if (this@AppInfoFragment.isVisible) {
                        appIconImageView.let {
                            IconPalette.loadAppIconView(it, app = app)
                        }
                        activity?.run {
                            app_logo_image_view.let {
                                IconPalette.loadAppIconView(it, app = app)
                                it.visibility = View.VISIBLE
                            }
                        }
                        nameTextView.text = appDatabase.name
                        appModuleName.text = appDatabase.targetChecker?.extraString ?: ""
                        versioningTextView.text = installedVersioning ?: ""
                        localVersioningTextView.text = installedVersioning
                                ?: getString(R.string.null_english)
                        appUrlTextView.let {
                            val url = appDatabase.url
                            val context = it.context
                            it.text = url

                            // 打开指向Url
                            it.setOnClickListener {
                                MiscellaneousUtils.accessByBrowser(url, context)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun loadAppVersioningInfo(versioningPosition: Int) {
        this.versioningPosition = versioningPosition
        versionMarkImageView.visibility = View.GONE
        app.appId.let {
            GlobalScope.launch {
                val releaseInfoBean = releaseInfoList?.get(versioningPosition)
                val latestVersionNumber = releaseInfoBean?.versionNumber
                val latestChangeLog = releaseInfoBean?.changeLog
                withContext(Dispatchers.Main) {
                    if (this@AppInfoFragment.isVisible) {
                        cloud_versioning_text_view.text = if (versioningPosition == 0) {
                            getString(R.string.latest_version_number)
                        } else {
                            getString(R.string.cloud_version_number)
                        }
                        cloudVersioningTextView.let {
                            it.text = latestVersionNumber
                            it.setOnLongClickListener(View.OnLongClickListener {
                                markVersionNumber(latestVersionNumber)
                                renewVersionRelatedItems()
                                return@OnLongClickListener true
                            })
                            renewVersionRelatedItems()
                        }
                        appChangelogTextView.text = if (latestChangeLog.isNullOrBlank())
                            getString(R.string.null_english)
                        else latestChangeLog
                    }
                }
            }
        }
    }

    private fun toastPromptMarkedVersionNumber() {
        GlobalScope.launch {
            if (Updater(app).getUpdateStatus() == Updater.APP_LATEST) null
            else {
                if (app.markProcessedVersionNumber != null)
                    R.string.marked_version_number_is_behind_latest
                else R.string.long_click_version_number_to_mark_as_processed
            }?.let {
                withContext(Dispatchers.Main) {
                    context?.run { Toast.makeText(this, it, Toast.LENGTH_LONG).show() }
                }
            }
        }
    }

    private fun markVersionNumber(versionNumber: String?) {
        app.markProcessedVersionNumber = versionNumber
    }

    private fun renewVersionRelatedItems() {
        versionMarkImageView.visibility =
                if (app.markProcessedVersionNumber == cloudVersioningTextView.text)
                    View.VISIBLE
                else View.GONE
        loadVersioningPopupMenu()
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
