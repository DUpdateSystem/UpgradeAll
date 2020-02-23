package net.xzos.upgradeall.data_manager.database

import net.xzos.upgradeall.data.database.AppDatabase
import net.xzos.upgradeall.data.json.gson.AppConfigGson
import net.xzos.upgradeall.data.json.gson.AppDatabaseExtraData
import net.xzos.upgradeall.system_api.api.DatabaseApi


class AppDatabase(
        name: String,
        url: String,
        api_uuid: String,
        type: String,
        targetChecker: AppConfigGson.AppConfigBean.TargetCheckerBean? = null,
        extraData: AppDatabaseExtraData? = null,
        id: Long = 0L

) : Database, AppDatabase(
        id, name, url, api_uuid, type, targetChecker, extraData
) {
    override fun save(): Boolean {
        val id = DatabaseApi.saveAppDatabase(this)
        return if (id != 0L) {
            this.id = id
            true
        } else false
    }

    override fun delete() = DatabaseApi.deleteAppDatabase(this)

    companion object {
        @Transient
        const val APP_TYPE_TAG = "app"
        @Transient
        const val APPLICATIONS_TYPE_TAG = "applications"

        fun newInstance() = AppDatabase("", "", "", "")
    }
}
