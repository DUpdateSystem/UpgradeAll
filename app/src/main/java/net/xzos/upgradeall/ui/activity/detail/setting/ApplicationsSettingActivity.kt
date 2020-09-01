package net.xzos.upgradeall.ui.activity.detail.setting

import kotlinx.android.synthetic.main.activity_app_setting.*
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.data.database.ApplicationsDatabase
import net.xzos.upgradeall.core.data_manager.AppDatabaseManager

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

    override fun setSettingItem() {}

    companion object {
        internal var bundleDatabase: ApplicationsDatabase? = null
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
