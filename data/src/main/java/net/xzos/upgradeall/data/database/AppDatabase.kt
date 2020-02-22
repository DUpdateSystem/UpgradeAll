package net.xzos.upgradeall.data.database

import net.xzos.upgradeall.data.json.gson.AppConfigGson
import net.xzos.upgradeall.data.json.gson.AppDatabaseExtraData


open class AppDatabase(
        var id: Long,
        var name: String,
        var url: String,
        var api_uuid: String,
        var type: String,
        var targetChecker: AppConfigGson.AppConfigBean.TargetCheckerBean? = null,
        var extraData: AppDatabaseExtraData? = null

) {
    companion object {
        @Transient
        const val APP_TYPE_TAG = "app"
        @Transient
        const val APPLICATIONS_TYPE_TAG = "applications"
    }
}