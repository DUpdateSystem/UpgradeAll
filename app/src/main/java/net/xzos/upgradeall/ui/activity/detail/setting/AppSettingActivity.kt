package net.xzos.upgradeall.ui.activity.detail.setting

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import kotlinx.android.synthetic.main.activity_app_setting.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.data.config.AppValue
import net.xzos.upgradeall.core.data.database.AppDatabase
import net.xzos.upgradeall.core.data.json.gson.PackageIdGson
import net.xzos.upgradeall.core.data.json.gson.PackageIdGson.Companion.API_TYPE_APP_PACKAGE
import net.xzos.upgradeall.core.data.json.gson.PackageIdGson.Companion.API_TYPE_MAGISK_MODULE
import net.xzos.upgradeall.core.data.json.gson.PackageIdGson.Companion.API_TYPE_SHELL
import net.xzos.upgradeall.core.data.json.gson.PackageIdGson.Companion.API_TYPE_SHELL_ROOT
import net.xzos.upgradeall.core.data_manager.AppDatabaseManager
import net.xzos.upgradeall.ui.viewmodels.adapters.SearchResultItemAdapter
import net.xzos.upgradeall.utils.SearchUtils
import net.xzos.upgradeall.utils.ToastUtil
import net.xzos.upgradeall.utils.VersioningUtils

class AppSettingActivity : BaseAppSettingActivity() {

    private val searchUtils by lazy { SearchUtils() }

    private val appDatabase: AppDatabase = bundleDatabase
            ?: AppDatabase(0, "", "", "")

    private val targetCheckerApi: String?
        get() = when (PackageIdApiSpinner.selectedItem.toString()) {
            getString(R.string.android_app) -> API_TYPE_APP_PACKAGE
            getString(R.string.magisk_module) -> API_TYPE_MAGISK_MODULE
            getString(R.string.shell) -> API_TYPE_SHELL
            getString(R.string.shell_root) -> API_TYPE_SHELL_ROOT
            else -> null
        }

    // 获取versionChecker
    private val packageId: PackageIdGson
        get() = PackageIdGson(
                targetCheckerApi,
                editPackageId.text.toString()
        )

    override fun saveDatabase(): Boolean {
        if (editUrl.text.isNullOrBlank()) {
            url_input_layout.error = getString(R.string.helper_text_cant_be_empty)
            return false
        }
        if (editUrl.text.isNullOrBlank()) {
            url_input_layout.error = getString(R.string.helper_text_cant_be_empty)
            return false
        }
        // 数据处理
        val name = editName.text.toString()
        val url = editUrl.text.toString()
        with(appDatabase) {
            this.hubUuid = this@AppSettingActivity.hubUuid ?: return false
            this.url = url
            this.packageId = this@AppSettingActivity.packageId
            this.name = name
        }
        return if (appDatabase.id == 0L)
            runBlocking { AppDatabaseManager.insertAppDatabase(appDatabase) != 0L }
        else
            runBlocking { AppDatabaseManager.updateAppDatabase(appDatabase) }
    }

    override fun setSettingItem() {
        // 如果是设置修改请求，设置预置设置项
        editUrl.setText(appDatabase.url)
        val packageIdGson = appDatabase.packageId
        val versionCheckerText = packageIdGson?.extraString
        PackageIdApiSpinner.setSelection(
                when (packageIdGson?.api?.toLowerCase(AppValue.locale)) {
                    API_TYPE_APP_PACKAGE -> 0
                    API_TYPE_MAGISK_MODULE -> 1
                    API_TYPE_SHELL -> 2
                    API_TYPE_SHELL_ROOT -> 3
                    else -> 0
                }
        )
        editPackageId.setText(versionCheckerText)
    }

    override fun initUi() {
        // 版本检查设置
        checkPackageIdButton.setOnClickListener {
            val rawVersion = VersioningUtils.getAppVersionNumber(packageId)
            val version = net.xzos.upgradeall.core.data_manager.utils.VersioningUtils.matchVersioningString(rawVersion)
            if (rawVersion != null) {
                ToastUtil.makeText("raw_version: $rawVersion\nversion: $version", Toast.LENGTH_SHORT)
            }
        }
        editPackageId.threshold = 1
        editPackageId.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) searchUtils.renewData()  // 清除搜索缓存
        }
        val mutex = Mutex()
        editPackageId.addTextChangedListener {
            if (targetCheckerApi != API_TYPE_SHELL && targetCheckerApi != API_TYPE_SHELL_ROOT) {
                val text = it.toString()
                GlobalScope.launch(Dispatchers.IO) {
                    mutex.withLock {
                        if (text.isNotBlank()) {
                            val searchInfoList = searchUtils.search(text)
                            if (searchInfoList.isNotEmpty()) {
                                withContext(Dispatchers.Main) {
                                    editPackageId.setAdapter(SearchResultItemAdapter(this@AppSettingActivity, searchInfoList))
                                    editPackageId.showDropDown()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        private var bundleDatabase: AppDatabase? = null
            set(value) {
                BaseAppSettingActivity.bundleDatabase = value
                field = value
            }
            get() {
                val app = field
                field = null
                return app
            }

        fun getInstance(context: Context, database: AppDatabase?) {
            bundleDatabase = database ?: AppDatabase(0, "", "", "")
            context.startActivity(Intent(context, AppSettingActivity::class.java))
        }
    }
}
