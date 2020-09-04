package net.xzos.upgradeall.ui.activity.detail.setting

import kotlinx.android.synthetic.main.activity_app_setting.*
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.data.config.AppValue
import net.xzos.upgradeall.core.data.database.AppDatabase
import net.xzos.upgradeall.core.data.json.gson.PackageIdGson
import net.xzos.upgradeall.core.data.json.gson.PackageIdGson.Companion.API_TYPE_APP_PACKAGE
import net.xzos.upgradeall.core.data.json.gson.PackageIdGson.Companion.API_TYPE_MAGISK_MODULE
import net.xzos.upgradeall.core.data.json.gson.PackageIdGson.Companion.API_TYPE_SHELL
import net.xzos.upgradeall.core.data.json.gson.PackageIdGson.Companion.API_TYPE_SHELL_ROOT
import net.xzos.upgradeall.core.data_manager.AppDatabaseManager

class AppSettingActivity : BaseAppSettingActivity() {

    private val appDatabase: AppDatabase = bundleDatabase
            ?: AppDatabase(0, "", "", "")

    private val targetCheckerApi: String?
        get() = when (versionCheckSpinner.selectedItem.toString()) {
            "APP 版本" -> API_TYPE_APP_PACKAGE
            "Magisk 模块" -> API_TYPE_MAGISK_MODULE
            "自定义 Shell 命令" -> API_TYPE_SHELL
            "自定义 Shell 命令（ROOT）" -> API_TYPE_SHELL_ROOT
            else -> null
        }

    // 获取versionChecker
    private val packageId: PackageIdGson
        get() = PackageIdGson(
                targetCheckerApi,
                editTarget.text.toString()
        )

    override fun saveDatabase(): Boolean {
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
        val packageIdGson = appDatabase.packageId
        val versionCheckerText = packageIdGson?.extraString
        versionCheckSpinner.setSelection(
                when (packageIdGson?.api?.toLowerCase(AppValue.locale)) {
                    API_TYPE_APP_PACKAGE -> 0
                    API_TYPE_MAGISK_MODULE -> 1
                    API_TYPE_SHELL -> 2
                    API_TYPE_SHELL_ROOT -> 3
                    else -> 0
                }
        )
        editTarget.setText(versionCheckerText)
    }

    companion object {
        internal var bundleDatabase: AppDatabase? = null
            set(value) {
                BaseAppSettingActivity.bundleDatabase = value
                field = value
            }
            get() {
                val app = field
                field = null
                return app
            }
    }
}
