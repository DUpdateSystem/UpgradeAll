package net.xzos.upgradeall.ui.activity.detail

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.android.synthetic.main.activity_app_setting.*
import kotlinx.android.synthetic.main.layout_appbar.*
import kotlinx.android.synthetic.main.list_content.*
import kotlinx.android.synthetic.main.simple_textview.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.data.config.AppValue
import net.xzos.upgradeall.core.data.database.AppDatabase
import net.xzos.upgradeall.core.data.json.gson.AppConfigGson
import net.xzos.upgradeall.core.data_manager.HubDatabaseManager
import net.xzos.upgradeall.core.server_manager.module.BaseApp
import net.xzos.upgradeall.core.server_manager.module.app.App
import net.xzos.upgradeall.core.server_manager.module.applications.Applications
import net.xzos.upgradeall.ui.activity.MainActivity
import net.xzos.upgradeall.ui.fragment.AppInfoFragment
import net.xzos.upgradeall.ui.viewmodels.adapters.SearchResultItemAdapter
import net.xzos.upgradeall.utils.IconPalette
import net.xzos.upgradeall.utils.SearchUtils
import net.xzos.upgradeall.utils.ToastUtil
import net.xzos.upgradeall.utils.VersioningUtils

class AppSettingActivity : AppCompatActivity() {

    private val searchUtils: SearchUtils = SearchUtils()
    private val editMode = bundleEditMode
    private val app = bundleApp as? App// 获取可能来自修改设置项的请求
    private val targetCheckerApi: String?
        get() = when (versionCheckSpinner.selectedItem.toString()) {
            "APP 版本" -> AppConfigGson.AppConfigBean.TargetCheckerBean.API_TYPE_APP_PACKAGE
            "Magisk 模块" -> AppConfigGson.AppConfigBean.TargetCheckerBean.API_TYPE_MAGISK_MODULE
            "自定义 Shell 命令" -> AppConfigGson.AppConfigBean.TargetCheckerBean.API_TYPE_SHELL
            "自定义 Shell 命令（ROOT）" -> AppConfigGson.AppConfigBean.TargetCheckerBean.API_TYPE_SHELL_ROOT
            else -> null
        }

    // 获取versionChecker
    private val targetChecker: AppConfigGson.AppConfigBean.TargetCheckerBean
        get() = AppConfigGson.AppConfigBean.TargetCheckerBean(
                targetCheckerApi,
                editTarget.text.toString()
        )

    private var hubUuid: String? = null
    private var appDatabase = app?.appDatabase
            ?: AppDatabase.newInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_setting)

        initView()
    }

    private fun setEndHelpIcon() {
        setEndIconOnClickListener(hub_input_layout)
        setEndIconOnClickListener(name_input_layout)
        setEndIconOnClickListener(url_input_layout)
        setEndIconOnClickListener(versioning_input_layout)
    }

    private fun setEndIconOnClickListener(textInputLayout: TextInputLayout) {
        val helpText = getString(when (textInputLayout.id) {
            R.id.hub_input_layout -> R.string.setting_app_hub_explain
            R.id.name_input_layout -> R.string.setting_app_name_explain
            R.id.url_input_layout -> R.string.setting_app_url_explain
            R.id.versioning_input_layout -> R.string.setting_app_versioning_explain
            else -> R.string.null_english
        }) + getString(R.string.detailed_help) + "：" + getString(R.string.readme_url)
        textInputLayout.setEndIconOnClickListener {
            AlertDialog.Builder(it.context).setView(R.layout.simple_textview).create().let { dialog ->
                dialog.show()
                dialog.text.text = helpText
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun showHubSelectDialog(vararg editViews: TextInputEditText) {
        BottomSheetDialog(this).apply {
            setContentView(layoutInflater.inflate(R.layout.list_content, null))
            val (hubNameStringList, hubUuidStringList) = renewApiJsonObject()
            list.adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, hubNameStringList)
            list.setOnItemClickListener { _, _, position, _ ->
                hubUuid = hubUuidStringList[position]
                for (editView in editViews)
                    editView.setText(hubNameStringList[position])
                cancel()
            }
        }.show()
    }

    @SuppressLint("InflateParams")
    private fun showHubUrlSelectDialog(editView: TextInputEditText) {
        hubUuid?.run {
            BottomSheetDialog(this@AppSettingActivity).apply {
                setContentView(layoutInflater.inflate(R.layout.list_content, null))
                val appUrlList = getAppUrlTemplate(this@run)
                list.adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, appUrlList)
                list.setOnItemClickListener { _, _, position, _ ->
                    editView.setText(appUrlList[position])
                    cancel()
                }
                if (appUrlList.isNotEmpty()) {
                    editView.setText(appUrlList[0])
                    if (appUrlList.size > 1)
                        show()
                }
            }
        }
    }

    private fun getAppUrlTemplate(hubUuid: String): List<String> {
        val appUrlList = mutableListOf<String>()
        HubDatabaseManager.getDatabase(hubUuid)?.hubConfig?.appUrlTemplates?.run {
            for (appUrlTemplate in this) {
                appUrlList.add((appUrlTemplate))
            }
        }
        return appUrlList
    }


    private fun addApp() {
        window?.let {
            floatingActionButton.visibility = View.GONE
            loadingBar.visibility = View.VISIBLE
            val name = editName.text.toString()
            val url = editUrl.text.toString()
            val versionChecker =
                    if (editMode != AppDatabase.APPLICATIONS_TYPE_TAG)
                        targetChecker
                    else null
            val addRepoSuccess = if (hubUuid != null && editMode != null)
                addRepoDatabase(name, hubUuid!!, url, editMode, versionChecker)  // 添加数据库
            else false
            if (addRepoSuccess) {
                // 提醒跟踪项详情页数据已刷新
                if (app != null)
                    AppInfoFragment.bundleApp = app
                finish()
            } else
                ToastUtil.makeText(R.string.failed_to_add, Toast.LENGTH_LONG)
            floatingActionButton?.visibility = View.VISIBLE
            loadingBar.visibility = View.GONE
        }
    }

    private fun setSettingItem() {
        // 如果是设置修改请求，设置预置设置项
        editName.setText(appDatabase.name)
        editUrl.setText(appDatabase.url)
        lifecycleScope.launch(Dispatchers.IO) {
            val text = HubDatabaseManager.getDatabase(appDatabase.hubUuid)?.hubConfig?.info?.hubName
            withContext(Dispatchers.Main) {
                editHub.setText(text)
            }
        }
        hubUuid = appDatabase.hubUuid
        val versionCheckerGson = appDatabase.targetChecker
        val versionCheckerText = versionCheckerGson?.extraString
        versionCheckerGson?.api?.let {
            versionCheckSpinner.setSelection(
                    when (it.toLowerCase(AppValue.locale)) {
                        AppConfigGson.AppConfigBean.TargetCheckerBean.API_TYPE_APP_PACKAGE -> 0
                        AppConfigGson.AppConfigBean.TargetCheckerBean.API_TYPE_MAGISK_MODULE -> 1
                        AppConfigGson.AppConfigBean.TargetCheckerBean.API_TYPE_SHELL -> 2
                        AppConfigGson.AppConfigBean.TargetCheckerBean.API_TYPE_SHELL_ROOT -> 3
                        else -> 0
                    }
            )
        }
        editTarget.setText(versionCheckerText)
    }

    private fun addRepoDatabase(
            name: String, hubUuid: String, URL: String, type: String,
            targetChecker: AppConfigGson.AppConfigBean.TargetCheckerBean?
    ): Boolean {
        // 获取数据库类
        appDatabase.run {
            this.hubUuid = hubUuid
            this.url = URL
            this.targetChecker = targetChecker
            this.type = type
            // 数据处理
            this.name = if (name.isNotBlank()) name
            else return false
        }
        return appDatabase.save(true)
    }

    private fun renewApiJsonObject(): Pair<List<String>, List<String>> {
        // api接口名称列表
        // 清空 apiSpinnerList
        val hubNameStringList = mutableListOf<String>()
        val hubUuidStringList = mutableListOf<String>()
        // 获取自定义源
        val hubList = HubDatabaseManager.hubDatabases  // 读取 hub 数据库
        for (hubDatabase in hubList) {
            val name: String = hubDatabase.hubConfig.info.hubName
            val apiUuid: String = hubDatabase.uuid
            hubNameStringList.add(name)
            // 记录可用的api UUID
            hubUuidStringList.add(apiUuid)
        }
        return Pair(hubNameStringList, hubUuidStringList)
    }

    companion object {

        internal var bundleApp: BaseApp? = null
            set(app) {
                field = app
                // 自动修改 EditMode
                bundleEditMode = when (app) {
                    is App -> {
                        AppDatabase.APP_TYPE_TAG
                    }
                    is Applications -> {
                        AppDatabase.APPLICATIONS_TYPE_TAG
                    }
                    else -> null
                }
            }
            get() {
                val app = field
                field = null
                return app
            }

        internal var bundleEditMode: String? = null
            get() {
                val editMode = field
                field = null
                return editMode
            }
    }

    private fun initView() {
        window.statusBarColor = ContextCompat.getColor(this, R.color.taupe)
        toolbar_backdrop_image.setBackgroundColor(IconPalette.getColorInt(R.color.taupe))
        collapsingToolbarLayout.contentScrim = getDrawable(R.color.taupe)
        floatingActionButton.visibility = View.GONE
        floatingActionButton.let { fab ->
            fab.setOnClickListener {
                addApp()
            }
            fab.setImageDrawable(getDrawable(R.drawable.ic_check_mark))
            fab.backgroundTintList = ColorStateList.valueOf((IconPalette.getColorInt(R.color.taupe)))
            fab.setColorFilter(IconPalette.getColorInt(R.color.white))
            fab.visibility = View.VISIBLE
        }
        setEndHelpIcon()
        app_logo_image_view.let {
            app?.let { innerApp ->
                IconPalette.loadAppIconView(it, app = innerApp)
            }
            it.visibility = View.VISIBLE
        }

        lifecycleScope.launch(Dispatchers.IO) {
            // 刷新第三方源列表，获取支持的第三方源列表
            val (hubNameStringList, _) = renewApiJsonObject()
            // 修改 apiSpinner
            withContext(Dispatchers.Main) {
                if (hubNameStringList.isEmpty()) {
                    ToastUtil.makeText(R.string.add_something, Toast.LENGTH_LONG)
                    MainActivity.setNavigationItemId(R.id.hubCloudFragment)
                    finish()
                }
            }
        }

        setSettingItem() // 设置预置设置项
        // 以下是按键事件
        // 判断编辑模式
        if (editMode == AppDatabase.APPLICATIONS_TYPE_TAG) {
            editUrl.isFocusable = false
            editUrl.setOnClickListener {
                showHubUrlSelectDialog(editUrl)
            }
            // 已隐藏无关选项
            imageView2.visibility = View.GONE
            textView2.visibility = View.GONE
            versionCheckSpinner.visibility = View.GONE
            versioning_input_layout.visibility = View.GONE
            versionCheckButton.visibility = View.GONE
        }
        val editViews = mutableListOf<TextInputEditText>(editHub).apply {
            if (editMode == AppDatabase.APPLICATIONS_TYPE_TAG)
                this.add(editName)
        }.toTypedArray()
        editHub.setOnClickListener {
            showHubSelectDialog(*editViews)
        }
        versionCheckButton.setOnClickListener {
            // 版本检查设置
            val rawVersion = VersioningUtils.getAppVersionNumber(targetChecker)
            val version = net.xzos.upgradeall.core.data_manager.utils.VersioningUtils.matchVersioningString(rawVersion)
            if (rawVersion != null) {
                ToastUtil.makeText("raw_version: $rawVersion\nversion: $version", Toast.LENGTH_SHORT)
            }
        }
        editTarget.threshold = 1
        val mutex = Mutex()
        editTarget.addTextChangedListener {
            if (targetCheckerApi != AppConfigGson.AppConfigBean.TargetCheckerBean.API_TYPE_SHELL && targetCheckerApi != AppConfigGson.AppConfigBean.TargetCheckerBean.API_TYPE_SHELL_ROOT) {
                val text = it.toString()
                GlobalScope.launch(Dispatchers.IO) {
                    mutex.withLock {
                        val searchInfoList = searchUtils.search(text)
                        if (text.isNotBlank()) {
                            withContext(Dispatchers.Main) {
                                if (searchInfoList.isNotEmpty()) {
                                    editTarget.setAdapter(SearchResultItemAdapter(this@AppSettingActivity, searchInfoList))
                                    editTarget.showDropDown()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}