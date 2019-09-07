package net.xzos.upgradeAll.ui.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.database.RepoDatabase
import net.xzos.upgradeAll.server.ServerContainer
import net.xzos.upgradeAll.server.app.engine.js.JavaScriptEngine
import net.xzos.upgradeAll.server.hub.HubManager
import net.xzos.upgradeAll.utils.VersionChecker
import org.json.JSONException
import org.json.JSONObject
import org.litepal.LitePal
import java.util.*

class AppSettingActivity : AppCompatActivity() {

    private var databaseId: Int = 0  // 设置页面代表的数据库项目

    private// 获取versionChecker
    val versionChecker: JSONObject
        get() {
            val versionChecker = JSONObject()
            val versionCheckSpinner = findViewById<Spinner>(R.id.versionCheckSpinner)
            val editVersionCheckText = findViewById<EditText>(R.id.editVersionCheckText)
            val versionCheckerText = editVersionCheckText.text.toString()
            var versionCheckerApi = versionCheckSpinner.selectedItem.toString()
            when (versionCheckerApi) {
                "APP 版本" -> versionCheckerApi = "APP"
                "Magisk 模块" -> versionCheckerApi = "Magisk"
                "自定义 Shell 命令" -> versionCheckerApi = "Shell"
                "自定义 Shell 命令（ROOT）" -> versionCheckerApi = "Shell_ROOT"
            }
            try {
                versionChecker.put("api", versionCheckerApi)
                versionChecker.put("text", versionCheckerText)
            } catch (e: JSONException) {
                e.printStackTrace()
            }

            return versionChecker
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_setting)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setTitle(R.string.add)
            actionBar.setDisplayHomeAsUpEnabled(true)
        }
        // 获取可能来自修改设置项的请求
        databaseId = intent.getIntExtra("database_id", 0)
        // 刷新第三方源列表，获取支持的第三方源列表
        val apiSpinnerStringArray = renewApiJsonObject()
        // 修改 apiSpinner
        if (apiSpinnerStringArray.isNotEmpty()) {
            val apiSpinner = findViewById<Spinner>(R.id.apiSpinner)
            val adapter = ArrayAdapter(this,
                    android.R.layout.simple_spinner_item, apiSpinnerStringArray)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            apiSpinner.adapter = adapter
        } else {
            Toast.makeText(this@AppSettingActivity, "请先添加软件源", Toast.LENGTH_LONG).show()
            onBackPressed()
        }
        setSettingItem() // 设置预置设置项
        // 以下是按键事件
        val versionCheckButton = findViewById<Button>(R.id.statusCheckButton)
        versionCheckButton.setOnClickListener {
            // 版本检查设置
            val versionCheckerJsonObject = versionChecker
            val versionChecker = VersionChecker(inputVersionCheckerJsonObject = versionCheckerJsonObject)
            val appVersion = versionChecker.version
            if (appVersion != null) {
                Toast.makeText(this@AppSettingActivity, "version: $appVersion", Toast.LENGTH_SHORT).show()
            }
        }
        val addButton = findViewById<Button>(R.id.saveButton)
        addButton.setOnClickListener {
            addButton.isEnabled = false
            addApp()
            addButton.isEnabled = true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun addApp() {
        val editName = findViewById<EditText>(R.id.editName)
        val editUrl = findViewById<EditText>(R.id.editUrl)
        val name = editName.text.toString()
        val url = editUrl.text.toString()
        val apiSpinner = findViewById<Spinner>(R.id.apiSpinner)
        val apiNum = apiSpinner.selectedItemPosition
        val versionChecker = versionChecker
        val progressBar = ProgressBar(this)
        // 弹出等待框
        progressBar.visibility = View.VISIBLE
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        // 添加数据库
        val addRepoSuccess = addRepoDatabase(databaseId, name, apiNum, url, versionChecker)
        if (addRepoSuccess) {
            ServerContainer.AppManager.setApp(databaseId)
            if (!addRepoSuccess) {
                Toast.makeText(this@AppSettingActivity, "什么？数据库添加失败！", Toast.LENGTH_LONG).show()
            }
            // 取消等待框
            progressBar.visibility = View.GONE
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            onBackPressed()  // 跳转主页面
        }
    }

    private fun setSettingItem() {
        // 如果是设置修改请求，设置预置设置项
        if (databaseId != 0) {
            val database = LitePal.find(RepoDatabase::class.java, databaseId.toLong())
            val editName = findViewById<EditText>(R.id.editName)
            editName.setText(database.name)
            val editUrl = findViewById<EditText>(R.id.editUrl)
            editUrl.setText(database.url)
            val apiSpinner = findViewById<Spinner>(R.id.apiSpinner)
            val apiUuid = database.api_uuid
            // 设置 apiSpinner 位置
            val spinnerIndex = apiSpinnerList.indexOf(apiUuid)
            if (spinnerIndex != -1) apiSpinner.setSelection(spinnerIndex)
            val versionCheckSpinner = findViewById<Spinner>(R.id.versionCheckSpinner)
            val versionChecker = JSONObject(database.versionChecker)
            var versionCheckerApi = ""
            var versionCheckerText = ""
            try {
                versionCheckerApi = versionChecker.getString("api")
                versionCheckerText = versionChecker.getString("text")
            } catch (e: JSONException) {
                Log.e(LogObjectTag, TAG, String.format("onCreate: 数据库损坏！  versionChecker: %s", versionChecker))
            }

            @SuppressLint("DefaultLocale")
            when (versionCheckerApi.toLowerCase()) {
                "app" -> versionCheckSpinner.setSelection(0)
                "magisk" -> versionCheckSpinner.setSelection(1)
            }
            val editVersionCheckText = findViewById<EditText>(R.id.editVersionCheckText)
            editVersionCheckText.setText(versionCheckerText)
        }
    }

    private fun addRepoDatabase(databaseId: Int, name: String, apiNum: Int, URL: String, versionChecker: JSONObject): Boolean {
        // 数据处理
        @Suppress("NAME_SHADOWING") var name: String = name
        val apiUuid = apiSpinnerList[apiNum]
        if (URL.isNotEmpty()) {
            // Name 为空，获取默认名称
            if (name.isEmpty()) {
                val jsCode = HubManager.getJsCode(apiUuid)
                if (jsCode == "")
                    Log.e(LogObjectTag, TAG, "未找到 js 脚本")
                val logObjectTag = arrayOf("TEMP", "0")
                val defaultName = runBlocking(Dispatchers.Default) {
                    JavaScriptEngine(logObjectTag, URL, jsCode).getDefaultName()
                }
                if (defaultName != null)
                    name = defaultName
            }
            val apiSpinner = findViewById<Spinner>(R.id.apiSpinner)
            val api = apiSpinner.selectedItem.toString()
            // 修改数据库
            var repoDatabase: RepoDatabase? = LitePal.find(RepoDatabase::class.java, databaseId.toLong())
            if (repoDatabase != null) {
                // 开启数据库
                // 将数据存入 RepoDatabase 数据库
                repoDatabase.name = name
                repoDatabase.api = api
                repoDatabase.api_uuid = apiUuid
                repoDatabase.url = URL
                repoDatabase.versionChecker = versionChecker.toString()
            } else {
                repoDatabase = RepoDatabase(name = name, api = api, url = URL, api_uuid = apiUuid, versionChecker = versionChecker.toString())
            }
            repoDatabase.save()
            // 为 databaseId 赋值
            this.databaseId = repoDatabase.id
            return true
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