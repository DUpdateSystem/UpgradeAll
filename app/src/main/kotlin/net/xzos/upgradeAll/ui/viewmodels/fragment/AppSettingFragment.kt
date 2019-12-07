package net.xzos.upgradeAll.ui.viewmodels.fragment

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputLayout
import kotlinx.android.synthetic.main.fragment_apps_setting.*
import kotlinx.android.synthetic.main.layout_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.data.database.litepal.RepoDatabase
import net.xzos.upgradeAll.data.database.manager.AppDatabaseManager
import net.xzos.upgradeAll.data.database.manager.HubDatabaseManager
import net.xzos.upgradeAll.data.json.gson.AppConfig
import net.xzos.upgradeAll.server.ServerContainer
import net.xzos.upgradeAll.server.app.engine.js.JavaScriptEngine
import net.xzos.upgradeAll.ui.activity.MainActivity
import net.xzos.upgradeAll.ui.viewmodels.adapters.SearchResultItemAdapter
import net.xzos.upgradeAll.utils.IconPalette
import net.xzos.upgradeAll.utils.SearchUtils
import net.xzos.upgradeAll.utils.VersioningUtils
import java.util.*


class AppSettingFragment : Fragment() {

    private var databaseId: Long = 0  // 设置页面代表的数据库项目

    private val targetCheckerApi: String?
        get() = when (versionCheckSpinner.selectedItem.toString()) {
            "APP 版本" -> "app_package"
            "Magisk 模块" -> "magisk_module"
            "自定义 Shell 命令" -> "Shell"
            "自定义 Shell 命令（ROOT）" -> "Shell_ROOT"
            else -> null
        }

    // 获取versionChecker
    private val targetChecker: AppConfig.AppConfigBean.TargetCheckerBean
        get() = AppConfig.AppConfigBean.TargetCheckerBean(
                targetCheckerApi,
                editTarget.text.toString()
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 获取可能来自修改设置项的请求
        arguments?.let {
            databaseId = it.getLong(AppInfoFragment.APP_DATABASE_ID, 0)
        }
    }

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
            val adapter = ArrayAdapter(context!!,
                    android.R.layout.simple_spinner_item, apiSpinnerStringArray)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            apiSpinner.adapter = adapter
        } else {
            Toast.makeText(context, "请先添加软件源", Toast.LENGTH_LONG).show()
            activity?.onBackPressed()
        }
        setSettingItem() // 设置预置设置项
        // 以下是按键事件
        versionCheckButton.setOnClickListener {
            // 版本检查设置
            val versionChecker = VersioningUtils.VersionChecker(targetChecker)
            val appVersion = versionChecker.version
            if (appVersion != null) {
                Toast.makeText(context, "version: $appVersion", Toast.LENGTH_SHORT).show()
            }
        }
        editTarget.threshold = 1
        editTarget.addTextChangedListener {
            if (targetCheckerApi != "Shell" && targetCheckerApi != "Shell_ROOT")
                GlobalScope.launch {
                    val searchInfoList = SearchUtils.searchTargetByAllApi(editTarget.text.toString())
                    if (this@AppSettingFragment.isVisible) {
                        launch(Dispatchers.Main) {
                            if (searchInfoList.isNotEmpty()) {
                                editTarget.setAdapter(SearchResultItemAdapter(context!!, searchInfoList))
                                editTarget.showDropDown()
                            } else Toast.makeText(context, R.string.no_completion_results, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
        }
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

    override fun onPause() {
        super.onPause()
        SearchUtils.clearResultCache()
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
                    dialog.findViewById<TextView>(R.id.text)?.text = text
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
                val addRepoSuccess = addRepoDatabase(databaseId, name, apiNum, url, versionChecker)  // 添加数据库
                launch(Dispatchers.Main) {
                    if (addRepoSuccess) {
                        ServerContainer.AppManager.setApp(databaseId)
                        activity?.onBackPressed()  // 跳转主页面
                    } else
                        Toast.makeText(context, "什么？添加失败！", Toast.LENGTH_LONG).show()
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
        if (databaseId != 0L) {
            AppDatabaseManager.getDatabase(databaseId)?.run {
                editName.setText(this.name)
                editUrl.setText(this.url)
                val apiUuid = this.api_uuid
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
    }

    private fun addRepoDatabase(databaseId: Long, name: String, apiNum: Int, URL: String, targetChecker: AppConfig.AppConfigBean.TargetCheckerBean): Boolean {
        // 数据处理
        @Suppress("NAME_SHADOWING") var name: String = name
        val apiUuid = apiSpinnerList[apiNum]
        if (URL.isNotBlank()) {
            // Name 为空，获取默认名称
            if (name.isBlank()) {
                val jsCode = HubDatabaseManager.getJsCode(apiUuid)
                if (jsCode.isNullOrBlank()) {
                    Log.e(logObjectTag, TAG, "未找到 js 脚本")
                } else {
                    val logObjectTag = Pair("TEMP", "0")
                    val defaultName = runBlocking(Dispatchers.Default) {
                        JavaScriptEngine(logObjectTag, URL, jsCode).getDefaultName()
                    }
                    if (!defaultName.isNullOrBlank())
                        name = defaultName
                }
            }
            val api = apiSpinner.selectedItem.toString()
            // 修改数据库
            var repoDatabase = AppDatabaseManager.getDatabase(databaseId)
            if (name.isNotBlank() && api.isNotBlank() && apiUuid.isNotBlank() && URL.isNotBlank()) {
                if (repoDatabase != null) {
                    // 开启数据库
                    // 将数据存入 RepoDatabase 数据库
                    repoDatabase.name = name
                    repoDatabase.api_uuid = apiUuid
                    repoDatabase.url = URL
                    repoDatabase.targetChecker = targetChecker
                } else {
                    repoDatabase = RepoDatabase(
                            name = name,
                            url = URL,
                            api_uuid = apiUuid
                    ).apply {
                        this.targetChecker = targetChecker
                    }
                }
                repoDatabase.save()
                // 为 databaseId 赋值
                this.databaseId = repoDatabase.id
                return true
            }
        }
        return false
    }

    private fun renewApiJsonObject(): Array<String> {
        // api接口名称列表
        // 清空 apiSpinnerList
        val nameStringList = ArrayList<String>()
        // 获取自定义源
        val hubList = HubDatabaseManager.databases  // 读取 hub 数据库
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

        private val Log = ServerContainer.Log
        private const val TAG = "UpdateItemSetting"
        private val logObjectTag = Pair("Core", TAG)

        private val apiSpinnerList = ArrayList<String>()
    }
}