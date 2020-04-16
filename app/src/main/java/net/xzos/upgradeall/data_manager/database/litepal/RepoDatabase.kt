package net.xzos.upgradeall.data_manager.database.litepal

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.core.data.json.gson.AppConfigGson
import net.xzos.upgradeall.core.data.json.gson.AppDatabaseExtraData
import net.xzos.upgradeall.data_manager.UIConfig
import org.json.JSONObject
import org.litepal.crud.LitePalSupport


internal class RepoDatabase(
        var name: String,
        var url: String,
        var api_uuid: String,
        var type: String
) : LitePalSupport() {
    val id: Long = 0

    private var extra_data: String? = null
    private var versionChecker: String? = null
    private val mutex = Mutex()

    var extraData: AppDatabaseExtraData?
        set(value) {
            runBlocking {
                mutex.withLock {
                    if (value != null)
                        extra_data = Gson().toJson(value)
                }
            }
        }
        get() {
            return if (extra_data != null)
                try {
                    Gson().fromJson(extra_data, AppDatabaseExtraData::class.java)
                } catch (e: JsonSyntaxException) {
                    val json = JSONObject(extra_data!!)
                    val cloudAppConfigStr = json.getString("cloud_app_config")
                    json.remove("cloud_app_config")
                    val extra_data = json.toString()
                    val cloudAppConfig = Gson().fromJson(cloudAppConfigStr, AppConfigGson::class.java)
                    Gson().fromJson(extra_data, AppDatabaseExtraData::class.java).apply {
                        this.cloudAppConfig = cloudAppConfig
                    }.also {
                        extraData = it
                        this.save()
                    }
                }
            else null
        }
    var targetChecker: AppConfigGson.AppConfigBean.TargetCheckerBean?
        get() {
            return if (versionChecker != null) Gson().fromJson(
                    versionChecker, AppConfigGson.AppConfigBean.TargetCheckerBean::class.java
            )
            else null
        }
        set(value) {
            if (value != null)
                versionChecker = Gson().toJson(value)
        }

    override fun save(): Boolean =
            if (name.isNotBlank() && url.isNotBlank() && api_uuid.isNotBlank() && type.isNotBlank())
                super.save()
            else false

    companion object {
        @Transient
        internal const val APP_TYPE_TAG = UIConfig.APP_TYPE_TAG

        @Transient
        internal const val APPLICATIONS_TYPE_TAG = UIConfig.APPLICATIONS_TYPE_TAG
    }
}
