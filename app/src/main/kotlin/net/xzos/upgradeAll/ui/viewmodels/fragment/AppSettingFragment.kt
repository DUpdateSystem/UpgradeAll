package net.xzos.upgradeAll.ui.viewmodels.fragment

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.android.synthetic.main.fragment_apps_setting.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.database.RepoDatabase
import net.xzos.upgradeAll.json.gson.VersionCheckerGson
import net.xzos.upgradeAll.server.ServerContainer
import net.xzos.upgradeAll.server.app.engine.js.JavaScriptEngine
import net.xzos.upgradeAll.server.hub.HubManager
import net.xzos.upgradeAll.ui.activity.MainActivity
import net.xzos.upgradeAll.utils.IconPalette
import net.xzos.upgradeAll.utils.VersionChecker
import org.json.JSONException
import org.litepal.LitePal
import java.util.*

class AppSettingFragment : Fragment() {

    private var databaseId: Long = 0  // 设置页面代表的数据库项目

    // 获取versionChecker
    private val versionCheckerGson: VersionCheckerGson
        get() =
            VersionCheckerGson(
                    when (versionCheckSpinner.selectedItem.toString()) {
                        "APP 版本" -> "APP"
                        "Magisk 模块" -> "Magisk"
                        "自定义 Shell 命令" -> "Shell"
                        "自定义 Shell 命令（ROOT）" -> "Shell_ROOT"
                        else -> null
                    },
                    editVersioningCheckText.text.toString()
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
            // TODO: 返回处理
            Toast.makeText(context, "请先添加软件源", Toast.LENGTH_LONG).show()
            activity?.onBackPressed()
        }
        setSettingItem() // 设置预置设置项
        // 以下是按键事件
        versionCheckButton.setOnClickListener {
            // 版本检查设置
            val versionCheckerJsonObject = versionCheckerGson
            val versionChecker = VersionChecker(versionCheckerGson = versionCheckerJsonObject)
            val appVersion = versionChecker.version
            if (appVersion != null) {
                Toast.makeText(context, "version: $appVersion", Toast.LENGTH_SHORT).show()
            }
        }
        activity?.let {
            it as AppCompatActivity
            it.findViewById<ImageView>(R.id.toolbar_backdrop_image)?.setBackgroundColor(IconPalette.getColorInt(R.color.taupe))
            it.findViewById<CollapsingToolbarLayout>(R.id.collapsingToolbarLayout)?.contentScrim = it.getDrawable(R.color.taupe)
            it.findViewById<FloatingActionButton>(R.id.addFloatingActionButton)?.visibility = View.GONE
            it.findViewById<FloatingActionButton>(R.id.floatingActionButton)?.let { fab ->
                fab.setOnClickListener {
                    addApp()
                }
                fab.setImageDrawable(it.getDrawable(R.drawable.ic_check_mark))
                fab.backgroundTintList = ColorStateList.valueOf((IconPalette.getColorInt(R.color.taupe)))
                fab.setColorFilter(IconPalette.getColorInt(R.color.white))
                fab.visibility = View.VISIBLE
            }
        }
    }

    private fun addApp() {
        val name = editName.text.toString()
        val url = editUrl.text.toString()
        val apiNum = apiSpinner.selectedItemPosition
        val versionChecker = versionCheckerGson
        val progressBar = ProgressBar(context)
        // 弹出等待框
        progressBar.visibility = View.VISIBLE
        activity?.window?.let {
            it.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            // 添加数据库
            val addRepoSuccess = addRepoDatabase(databaseId, name, apiNum, url, versionChecker)
            // 取消等待框
            if (addRepoSuccess) {
                ServerContainer.AppManager.setApp(databaseId)
                activity?.onBackPressed()  // 跳转主页面
            } else
                Toast.makeText(context, "什么？添加失败！", Toast.LENGTH_LONG).show()
            it.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        }
        progressBar.visibility = View.GONE
    }

    private fun setSettingItem() {
        // 如果是设置修改请求，设置预置设置项
        if (databaseId != 0L) {
            val database = LitePal.find(RepoDatabase::class.java, databaseId)
            editName.setText(database.name)
            editUrl.setText(database.url)
            val apiUuid = database.api_uuid
            // 设置 apiSpinner 位置
            val spinnerIndex = apiSpinnerList.indexOf(apiUuid)
            if (spinnerIndex != -1) apiSpinner.setSelection(spinnerIndex)
            val versionCheckerGson = database.versionCheckerGson
            val versionChecker = if (versionCheckerGson is VersionCheckerGson)
                versionCheckerGson
            else VersionCheckerGson()
            var versionCheckerApi: String? = null
            var versionCheckerText: String? = null
            try {
                versionCheckerApi = versionChecker.api
                versionCheckerText = versionChecker.text
            } catch (e: JSONException) {
                Log.e(LogObjectTag, TAG, String.format("onCreate: 数据库损坏！  versionCheckerGson: %s", versionChecker))
            }

            if (versionCheckerApi != null)
                @SuppressLint("DefaultLocale")
                when (versionCheckerApi.toLowerCase()) {
                    "app" -> versionCheckSpinner.setSelection(0)
                    "magisk" -> versionCheckSpinner.setSelection(1)
                }
            editVersioningCheckText.setText(versionCheckerText)
        }
    }

    private fun addRepoDatabase(databaseId: Long, name: String, apiNum: Int, URL: String, versionChecker: VersionCheckerGson): Boolean {
        // 数据处理
        @Suppress("NAME_SHADOWING") var name: String = name
        val apiUuid = apiSpinnerList[apiNum]
        if (URL.isNotBlank()) {
            // Name 为空，获取默认名称
            if (name.isBlank()) {
                val jsCode = HubManager.getJsCode(apiUuid)
                if (jsCode.isNullOrBlank()) {
                    Log.e(LogObjectTag, TAG, "未找到 js 脚本")
                } else {
                    val logObjectTag = arrayOf("TEMP", "0")
                    val defaultName = runBlocking(Dispatchers.Default) {
                        JavaScriptEngine(logObjectTag, URL, jsCode).getDefaultName()
                    }
                    if (!defaultName.isNullOrBlank())
                        name = defaultName
                }
            }
            val api = apiSpinner.selectedItem.toString()
            // 修改数据库
            var repoDatabase: RepoDatabase? = LitePal.find(RepoDatabase::class.java, databaseId)
            if (name.isNotBlank() && api.isNotBlank() && apiUuid.isNotBlank() && URL.isNotBlank()) {
                if (repoDatabase != null) {
                    // 开启数据库
                    // 将数据存入 RepoDatabase 数据库
                    repoDatabase.name = name
                    repoDatabase.api = api
                    repoDatabase.api_uuid = apiUuid
                    repoDatabase.url = URL
                    repoDatabase.versionCheckerGson = versionChecker
                } else {
                    repoDatabase = RepoDatabase(name = name, api = api, url = URL, api_uuid = apiUuid).apply {
                        versionCheckerGson = versionChecker
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
        val hubList = HubManager.databases  // 读取 hub 数据库
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
        private val LogObjectTag = arrayOf("Core", TAG)

        private val apiSpinnerList = ArrayList<String>()
    }
}