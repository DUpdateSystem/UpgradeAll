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
        private val extraDataGson: AppDatabaseExtraData? = null
) {
    // 是否需要刷新数据
    @Transient
    var needRefreshable = false
        get() {
            val i = field
            field = false
            return i
        }

    var extraData: AppDatabaseExtraData? = extraDataGson
        get() = field ?: AppDatabaseExtraData()
                .also { field = it }

    fun save(refresh: Boolean): Boolean {
        return saveReturnId(refresh) != 0L
    }

    fun saveReturnId(refresh: Boolean): Long {
        if (id == 0L
                && type == APPLICATIONS_TYPE_TAG
                && AppDatabaseManager.exists(uuid = hubUuid))
            return 0L
        needRefreshable = refresh  // 是否立即刷新
        return AppDatabaseManager.saveDatabase(this).also {
            id = it
        }
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
