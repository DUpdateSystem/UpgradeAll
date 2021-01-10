package net.xzos.upgradeall.ui.detail.setting

import android.content.Context
import android.content.Intent
import kotlinx.android.synthetic.main.activity_app_setting.*
import kotlinx.coroutines.runBlocking

class ApplicationsSettingActivity : BaseAppSettingActivity() {

    private val applicationsDatabase: ApplicationsDatabase = bundleDatabase
            ?: ApplicationsDatabase(0, "", "")

    override fun saveDatabase(): Boolean {
        // 数据处理
        val name = editName.text.toString()
        with(applicationsDatabase) {
            this.name = name
            this.hubUuid = this@ApplicationsSettingActivity.hubUuid ?: return false
        }
        return if (applicationsDatabase.id == 0L)
            runBlocking { AppDatabaseManager.insertApplicationsDatabase(applicationsDatabase) != 0L }
        else
            runBlocking { AppDatabaseManager.updateApplicationsDatabase(applicationsDatabase) }
    }

    override fun getHubJsonObject(): Pair<List<String>, List<String>> {
        // api接口名称列表
        // 清空 apiSpinnerList
        val hubNameStringList = mutableListOf<String>()
        val hubUuidStringList = mutableListOf<String>()
        // 获取自定义源
        HubDatabaseManager.hubDatabases.filter {
            it.hubConfig.apiKeywords.contains(AppType.androidApp)
        }.forEach {  // 读取 hub 数据库
            val name: String = it.hubConfig.info.hubName
            val apiUuid: String = it.uuid
            hubNameStringList.add(name)
            // 记录可用的api UUID
            hubUuidStringList.add(apiUuid)
        }
        return Pair(hubNameStringList, hubUuidStringList)
    }


    override fun setSettingItem() {}
    override fun initUi() {}

    companion object {
        private var bundleDatabase: ApplicationsDatabase? = null
            set(value) {
                BaseAppSettingActivity.bundleDatabase = value
                field = value
            }
            get() {
                val app = field
                field = null
                return app
            }

        fun getInstance(context: Context, database: ApplicationsDatabase?) {
            bundleDatabase = database ?: ApplicationsDatabase(0, "", "")
            context.startActivity(Intent(context, ApplicationsSettingActivity::class.java))
        }
    }
}
