package net.xzos.upgradeall.core.data.database

import net.xzos.upgradeall.core.data.json.gson.AppConfigGson
import net.xzos.upgradeall.core.data.json.gson.AppDatabaseExtraData
import net.xzos.upgradeall.core.data_manager.AppDatabaseManager


data class AppDatabase(
        var id: Long,  // 后期考虑替换为其他标识符
        var name: String,
        var url: String,
        var hubUuid: String,
        var type: String,
        var targetChecker: AppConfigGson.AppConfigBean.TargetCheckerBean? = null,
        var extraData: AppDatabaseExtraData? = null
) {
    // 是否需要刷新数据
    var needRefreshable = false
        get() {
            val i = field
            field = false
            return i
        }

    fun save(refresh: Boolean): Boolean {
        needRefreshable = refresh  // 是否立即刷新
        return AppDatabaseManager.saveDatabase(this)
    }

    fun delete(): Boolean {
        needRefreshable = true
        return AppDatabaseManager.deleteDatabase(this)
    }

    companion object {
        @Transient
        const val APP_TYPE_TAG = "app"

        @Transient
        const val APPLICATIONS_TYPE_TAG = "applications"

        fun newInstance() = AppDatabase(0L, "", "", "", "")
    }
}
