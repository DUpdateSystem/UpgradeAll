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
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.data.json.gson.AppDatabaseExtraData
import net.xzos.upgradeAll.data.json.gson.JSReturnData
import net.xzos.upgradeAll.server.app.engine.js.JavaScriptEngine
import net.xzos.upgradeAll.server.app.manager.module.App
import net.xzos.upgradeAll.server.app.manager.module.Updater
import net.xzos.upgradeAll.server.update.UpdateManager
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
    private lateinit var app: App
    private lateinit var engine: JavaScriptEngine

    private var versioningPosition: Int = 0
    private var jsReturnData: JSReturnData? = null
        get() {
            return field ?: runBlocking {
                engine.getJsReturnData().also {
                    field = it
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MainActivity.bundleApp?.also {
            app = it
        } ?: activity?.onBackPressed()
        engine = app.engine
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
            MainActivity.navigationItemId.value = R.id.appSettingFragment
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
            val versionNumberList = jsReturnData!!.releaseInfoList.map {
                it.version_number
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
                    val nameList = jsReturnData!!.releaseInfoList[versioningPosition].assets.map { asset ->
                        asset.name
                    }
                    launch(Dispatchers.Main) {
                        if (this@AppInfoFragment.isVisible) {
                            if (nameList.isNotEmpty()) {
                                dialog.findViewById<ListView>(R.id.list)?.let { list ->
                                    list.adapter =
                                            ArrayAdapter(dialog.context, android.R.layout.simple_list_item_1, nameList)
                                    // 下载文件
                                    list.setOnItemClickListener { _, _, position, _ ->
                                        Updater(app).nonBlockingDownloadReleaseFile(Pair(versioningPosition, position), context = context)
                                    }
                                    list.setOnItemLongClickListener { _, _, position, _ ->
                                        Updater(app).nonBlockingDownloadReleaseFile(Pair(versioningPosition, position), externalDownloader = true, context = context)
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
        app.appDatabase.let { appDatabase ->
            GlobalScope.launch {
                val installedVersioning = app.installedVersioning
                launch(Dispatchers.Main) {
                    if (this@AppInfoFragment.isVisible) {
                        appIconImageView.let {
                            IconPalette.loadAppIconView(it, app = app)
                        }
                        activity?.run {
                            this.findViewById<ImageView>(R.id.app_logo_image_view)?.let {
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
        app.appDatabase.let {
            GlobalScope.launch {
                if (!Updater(app).isSuccessRenew()) return@launch
                val releaseInfoBean = jsReturnData!!.releaseInfoList[versioningPosition]
                val latestVersionNumber = releaseInfoBean.version_number
                val latestChangeLog = releaseInfoBean.change_log
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
            if (Updater(app).getUpdateStatus() == Updater.APP_LATEST) null
            else {
                if (app.appDatabase.extraData?.markProcessedVersionNumber != null)
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
        app.appDatabase.also {
            (it.extraData ?: AppDatabaseExtraData(null, null))
                    .apply {
                        this.markProcessedVersionNumber =
                                if (this.markProcessedVersionNumber != versionNumber) versionNumber
                                else null
                    }.run { it.extraData = this }
        }.save()
    }

    private fun renewVersionRelatedItems() {
        versionMarkImageView.visibility =
                if (app.appDatabase.extraData?.markProcessedVersionNumber == cloudVersioningTextView.text)
                    View.VISIBLE
                else View.GONE
        loadVersioningPopupMenu()
    }
}
