package net.xzos.upgradeAll.ui.viewmodels.fragment

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.android.synthetic.main.fragment_app_info.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.data.database.manager.AppDatabaseManager
import net.xzos.upgradeAll.data.json.gson.AppDatabaseExtraData
import net.xzos.upgradeAll.server.ServerContainer.Companion.AppManager
import net.xzos.upgradeAll.server.app.manager.module.App
import net.xzos.upgradeAll.server.app.manager.module.Updater
import net.xzos.upgradeAll.ui.activity.MainActivity
import net.xzos.upgradeAll.utils.IconPalette
import net.xzos.upgradeAll.utils.MiscellaneousUtils

/**
 * 更新项详细数据展示页面
 * 作为框架嵌套到 主页[net.xzos.upgradeAll.ui.activity.MainActivity]
 * 由点击 更新项 [net.xzos.upgradeAll.ui.viewmodels.adapters.AppItemAdapter] 动作 触发显示
 * 使用 [net.xzos.upgradeAll.ui.activity.MainActivity.setFrameLayout] 方法跳转
 */
class AppInfoFragment : Fragment() {
    private var versioningPosition: Int = 0
    private var appDatabaseId: Long = 0
    private lateinit var app: App

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            appDatabaseId = it.getLong(APP_DATABASE_ID)
            app = AppManager.getApp(appDatabaseId)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_app_info, container, false).apply {
                this.findViewById<LinearLayout>(R.id.placeholderLayout).visibility = View.VISIBLE
            }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        MainActivity.actionBarDrawerToggle.isDrawerIndicatorEnabled = false  // 禁止开启侧滑栏，启用返回按钮响应事件
        loadAllAppInfo()
        toastPromptMarkedVersionNumber()
        loadAppVersioningInfo(0)
        placeholderLayout.visibility = View.GONE
        editImageView.setOnClickListener {
            MainActivity.navigationItemId.value = Pair(R.id.appSettingFragment, appDatabaseId)
        }
        activity?.apply {
            this as AppCompatActivity
            this.findViewById<ImageView>(R.id.toolbar_backdrop_image)?.setBackgroundColor(IconPalette.getColorInt(R.color.coolapk_green))
            this.findViewById<CollapsingToolbarLayout>(R.id.collapsingToolbarLayout)?.contentScrim = getDrawable(R.color.coolapk_green)
            this.findViewById<FloatingActionButton>(R.id.addFloatingActionButton)?.visibility = View.GONE
            this.findViewById<FloatingActionButton>(R.id.floatingActionButton)?.let { fab ->
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

    private fun loadVersioningPopupMenu() {
        versioningSelectLayout.visibility = View.GONE
        GlobalScope.launch {
            val engine = app.engine
            val versionNumberList: MutableList<String> = mutableListOf()
            for (i in 0..engine.getReleaseNum())
                engine.getReleaseInfo(i)?.version_number?.run {
                    versionNumberList.add(this)
                }

            launch(Dispatchers.Main) {
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
                dialog.findViewById<LinearLayout>(R.id.placeholderLayout)?.visibility = View.VISIBLE
                GlobalScope.launch {
                    val engine = app.engine
                    val releaseAsset = engine.getReleaseInfo(versioningPosition)
                    val nameList = releaseAsset?.assets?.map { asset ->
                        asset.name
                    } ?: return@launch
                    launch(Dispatchers.Main) {
                        if (this@AppInfoFragment.isVisible) {
                            if (nameList.isNotEmpty()) {
                                dialog.findViewById<ListView>(R.id.list)?.let { list ->
                                    list.adapter =
                                            ArrayAdapter(dialog.context, android.R.layout.simple_list_item_1, nameList)
                                    // 下载文件
                                    list.setOnItemClickListener { _, _, position, _ ->
                                        Updater(engine).nonBlockingDownloadReleaseFile(Pair(versioningPosition, position), context = context)
                                    }
                                    list.setOnItemLongClickListener { _, _, position, _ ->
                                        Updater(engine).nonBlockingDownloadReleaseFile(Pair(versioningPosition, position), externalDownloader = true, context = context)
                                        return@setOnItemLongClickListener true
                                    }
                                }
                            } else {
                                dialog.findViewById<TextView>(R.id.isEmptyTextView)?.visibility = View.VISIBLE
                            }
                            dialog.findViewById<LinearLayout>(R.id.placeholderLayout)?.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    private fun loadAllAppInfo() {
        AppDatabaseManager.getDatabase(appDatabaseId)?.let { appDatabase ->
            GlobalScope.launch {
                val installedVersioning = app.installedVersioning
                launch(Dispatchers.Main) {
                    if (this@AppInfoFragment.isVisible) {
                        appIconImageView.let {
                            IconPalette.loadAppIconView(it, appDatabaseId = appDatabaseId)
                        }
                        activity?.run {
                            this.findViewById<ImageView>(R.id.app_logo_image_view)?.let {
                                IconPalette.loadAppIconView(it, appDatabaseId = appDatabaseId)
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
        AppDatabaseManager.getDatabase(appDatabaseId)?.let {
            GlobalScope.launch {
                val engine = app.engine
                if (!Updater(app.engine).isSuccessRenew()) return@launch
                val latestVersionNumber = engine.getReleaseInfo(versioningPosition)?.version_number
                val latestChangeLog = engine.getReleaseInfo(versioningPosition)?.change_log
                launch(Dispatchers.Main) {
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
                        appChangelogTextView.text = latestChangeLog
                                ?: getString(R.string.null_english)
                    }
                }
            }
        }
    }

    private fun toastPromptMarkedVersionNumber() {
        GlobalScope.launch {
            if (AppManager.getApp(appDatabaseId).isLatest()) null
            else {
                if (AppDatabaseManager.getDatabase(appDatabaseId)?.extraData?.markProcessedVersionNumber != null)
                    R.string.marked_version_number_is_behind_latest
                else R.string.long_click_version_number_to_mark_as_processed
            }?.let {
                launch(Dispatchers.Main) {
                    context?.run { Toast.makeText(this, it, Toast.LENGTH_LONG).show() }
                }
            }
        }
    }

    private fun markVersionNumber(versionNumber: String?) {
        AppDatabaseManager.getDatabase(appDatabaseId)?.also {
            (it.extraData ?: AppDatabaseExtraData(null, null))
                    .apply {
                        this.markProcessedVersionNumber =
                                if (this.markProcessedVersionNumber != versionNumber) versionNumber
                                else null
                    }.run { it.extraData = this }
        }?.save()
    }

    private fun renewVersionRelatedItems() {
        versionMarkImageView.visibility =
                if (AppDatabaseManager.getDatabase(appDatabaseId)?.extraData?.markProcessedVersionNumber == cloudVersioningTextView.text)
                    View.VISIBLE
                else View.GONE
        loadVersioningPopupMenu()
    }

    companion object {
        internal const val APP_DATABASE_ID = "app_database_id"
    }
}