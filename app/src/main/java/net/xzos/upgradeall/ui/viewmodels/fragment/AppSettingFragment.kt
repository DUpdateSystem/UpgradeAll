package net.xzos.upgradeall.ui.viewmodels.fragment

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputLayout
import kotlinx.android.synthetic.main.fragment_apps_setting.*
import kotlinx.android.synthetic.main.layout_main.*
import kotlinx.android.synthetic.main.simple_textview.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.xzos.dupdatesystem.core.data.database.AppDatabase
import net.xzos.dupdatesystem.core.data.json.gson.AppConfigGson
import net.xzos.dupdatesystem.core.data.json.gson.AppConfigGson.AppConfigBean.TargetCheckerBean.Companion.API_TYPE_APP_PACKAGE
import net.xzos.dupdatesystem.core.data.json.gson.AppConfigGson.AppConfigBean.TargetCheckerBean.Companion.API_TYPE_SHELL
import net.xzos.dupdatesystem.core.data.json.gson.AppConfigGson.AppConfigBean.TargetCheckerBean.Companion.API_TYPE_SHELL_ROOT
import net.xzos.dupdatesystem.core.data.json.nongson.ObjectTag
import net.xzos.dupdatesystem.core.data_manager.HubDatabaseManager
import net.xzos.dupdatesystem.core.server_manager.module.app.App
import net.xzos.upgradeall.R
import net.xzos.upgradeall.ui.activity.MainActivity
import net.xzos.upgradeall.ui.viewmodels.adapters.SearchResultItemAdapter
import net.xzos.upgradeall.utils.IconPalette
import net.xzos.upgradeall.utils.SearchUtils
import net.xzos.upgradeall.utils.VersioningUtils
import java.util.*


class AppSettingFragment : Fragment() {

    // 获取可能来自修改设置项的请求
    private val app = bundleApp
    private var appInfo = app?.appInfo
            ?: AppDatabase.newInstance()

    private val targetCheckerApi: String?
        get() = when (versionCheckSpinner.selectedItem.toString()) {
            "APP 版本" -> API_TYPE_APP_PACKAGE
            "Magisk 模块" -> API_TYPE_APP_PACKAGE
            "自定义 Shell 命令" -> API_TYPE_SHELL
            "自定义 Shell 命令（ROOT）" -> API_TYPE_SHELL_ROOT
            else -> null
        }

    // 获取versionChecker
    private val targetChecker: AppConfigGson.AppConfigBean.TargetCheckerBean
        get() = AppConfigGson.AppConfigBean.TargetCheckerBean(
                targetCheckerApi,
                editTarget.text.toString()
        )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_apps_setting, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        MainActivity.actionBarDrawerToggle.isDrawerIndicatorEnabled = false  // 禁止开启侧滑栏，启用返回按钮响应事件
        // 刷新第三方源列表，获取支持的第三方源列表
        val apiSpinnerStringArray = renewApiJsonObject()
        // 修改 apiSpinner
        if (apiSpinnerStringArray.isNotEmpty()) {
            val adapter = ArrayAdapter(requireContext(),
                    android.R.layout.simple_spinner_item, apiSpinnerStringArray)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            apiSpinner.adapter = adapter
        } else {
            Toast.makeText(context, "请先添加软件源或下载软件配置", Toast.LENGTH_LONG).show()
            activity?.onBackPressed()
            MainActivity.navigationItemId.value = R.id.hubCloudFragment
        }
        setSettingItem() // 设置预置设置项
        // 以下是按键事件
        versionCheckButton.setOnClickListener {
            // 版本检查设置
            val appVersion = VersioningUtils.getAppVersionNumber(targetChecker)
            if (appVersion != null) {
                Toast.makeText(context, "version: $appVersion", Toast.LENGTH_SHORT).show()
            }
        }
        editTarget.threshold = 1
        editTarget.addTextChangedListener {
            if (targetCheckerApi != API_TYPE_SHELL && targetCheckerApi != API_TYPE_SHELL_ROOT)
                GlobalScope.launch {
                    val searchInfoList = SearchUtils().search(editTarget.text.toString())
                    if (this@AppSettingFragment.isVisible) {
                        launch(Dispatchers.Main) {
                            if (searchInfoList.isNotEmpty()) {
                                editTarget.setAdapter(SearchResultItemAdapter(requireContext(), searchInfoList))
                                editTarget.showDropDown()
                            } else Toast.makeText(context, R.string.no_completion_results, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.let {
            it as AppCompatActivity
            it.toolbar_backdrop_image.setBackgroundColor(IconPalette.getColorInt(R.color.taupe))
            it.collapsingToolbarLayout.contentScrim = it.getDrawable(R.color.taupe)
            it.addFloatingActionButton.visibility = View.GONE
            it.floatingActionButton.let { fab ->
                fab.setOnClickListener {
                    addApp()
                }
                fab.setImageDrawable(it.getDrawable(R.drawable.ic_check_mark))
                fab.backgroundTintList = ColorStateList.valueOf((IconPalette.getColorInt(R.color.taupe)))
                fab.setColorFilter(IconPalette.getColorInt(R.color.white))
                fab.visibility = View.VISIBLE
            }
        }
        setEndHelpIcon()
    }

    override fun onDestroy() {
        super.onDestroy()
        activity?.floatingActionButton?.run {
            this.setOnClickListener(null)
            this.visibility = View.GONE
        }
    }

    private fun setEndHelpIcon() {
        setEndIconOnClickListener(name_input_layout)
        setEndIconOnClickListener(url_input_layout)
        setEndIconOnClickListener(versioning_input_layout)
    }

    private fun setEndIconOnClickListener(textInputLayout: TextInputLayout) {
        (getString(when (textInputLayout.id) {
            R.id.name_input_layout -> R.string.setting_app_name_explain
            R.id.url_input_layout -> R.string.setting_app_url_explain
            R.id.versioning_input_layout -> R.string.setting_app_versioning_explain
            else -> R.string.null_english
        }) + getString(R.string.setting_help_web_page)).let { text ->
            textInputLayout.setEndIconOnClickListener {
                AlertDialog.Builder(it.context).setView(R.layout.simple_textview).create().let { dialog ->
                    dialog.show()
                    dialog.text.text = text
                }
            }
        }
    }

    private fun addApp() {
        activity?.window?.let {
            GlobalScope.launch {
                val name = editName.text.toString()
                val url = editUrl.text.toString()
                val apiNum = apiSpinner.selectedItemPosition
                val versionChecker = targetChecker
                launch(Dispatchers.Main) {
                    // 弹出等待框
                    activity?.run {
                        this.floatingActionButton?.visibility = View.GONE
                        this.loadingBar?.visibility = View.VISIBLE
                    }
                }
                val addRepoSuccess = addRepoDatabase(name, apiNum, url, versionChecker)  // 添加数据库
                launch(Dispatchers.Main) {
                    if (addRepoSuccess) {
                        // 提醒跟踪项详情页数据已刷新
                        if (app != null)
                            AppInfoFragment.bundleApp = app
                        activity?.onBackPressed()  // 跳转主页面
                    } else
                        Toast.makeText(context, "添加失败", Toast.LENGTH_LONG).show()
                    // 取消等待框
                    activity?.run {
                        this.floatingActionButton?.visibility = View.VISIBLE
                        this.loadingBar?.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun setSettingItem() {
        // 如果是设置修改请求，设置预置设置项
        appInfo.run {
            editName.setText(this.name)
            editUrl.setText(this.url)
            val apiUuid = this.apiUuid
            val spinnerIndex = apiSpinnerList.indexOf(apiUuid)
            if (spinnerIndex != -1) apiSpinner.setSelection(spinnerIndex)
            val versionCheckerGson = this.targetChecker
            val versionCheckerApi = versionCheckerGson?.api
            val versionCheckerText = versionCheckerGson?.extraString
            if (versionCheckerApi != null)
                @SuppressLint("DefaultLocale")
                when (versionCheckerApi.toLowerCase()) {
                    "app" -> versionCheckSpinner.setSelection(0)
                    "magisk" -> versionCheckSpinner.setSelection(1)
                }
            editTarget.setText(versionCheckerText)
        }
    }

    private fun addRepoDatabase(
            name: String, apiNum: Int, URL: String,
            targetChecker: AppConfigGson.AppConfigBean.TargetCheckerBean
    ): Boolean {
        // 获取数据库类
        val apiUuid = apiSpinnerList[apiNum]
        appInfo.run {
            this.apiUuid = apiUuid
            this.url = URL
            this.targetChecker = targetChecker
            // 数据处理
            this.name = if (name.isNotBlank()) name
            else runBlocking(Dispatchers.IO) { App(this@run).engine.getDefaultName() }
                    ?: return false
        }
        return appInfo.save()
    }

    private fun renewApiJsonObject(): Array<String> {
        // api接口名称列表
        // 清空 apiSpinnerList
        val nameStringList = ArrayList<String>()
        // 获取自定义源
        val hubList = HubDatabaseManager.hubDatabases  // 读取 hub 数据库
        for (hubDatabase in hubList) {
            val name: String = hubDatabase.name
            val apiUuid: String = hubDatabase.uuid
            nameStringList.add(name)
            // 记录可用的api UUID
            apiSpinnerList.add(apiUuid)
        }
        return nameStringList.toTypedArray()
    }

    companion object {

        private const val TAG = "UpdateItemSetting"
        private val logObjectTag = ObjectTag("UI", TAG)

        private val apiSpinnerList = ArrayList<String>()

        internal var bundleApp: App? = null
            get() {
                val app = field
                field = null
                return app
            }
    }
}
