package net.xzos.upgradeAll.data.database.manager

import com.google.gson.Gson
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.application.MyApplication
import net.xzos.upgradeAll.data.database.litepal.RepoDatabase
import net.xzos.upgradeAll.data.json.gson.AppConfig
import net.xzos.upgradeAll.data.json.gson.AppDatabaseExtraData
import net.xzos.upgradeAll.data.json.nongson.ObjectTag
import net.xzos.upgradeAll.server.app.manager.AppManager
import net.xzos.upgradeAll.server.log.LogUtil
import org.litepal.LitePal
import org.litepal.extension.findAll

object AppDatabaseManager {

    private const val TAG = "AppDatabaseManager"
    private val objectTag = ObjectTag("Core", TAG)

    private val Log = LogUtil
    // 读取 apps 数据库
    internal val appDatabases: List<RepoDatabase>
        get() = LitePal.findAll()

    /**
     * appConfig: 软件数据库的 json 数据输入
     */
    fun setDatabase(appConfig: AppConfig): RepoDatabase? {
        val name = appConfig.info?.appName ?: ""
        val url = appConfig.info?.url ?: ""
        val uuid = appConfig.uuid ?: ""
        val apiUuid = appConfig.appConfig?.hubInfo?.hubUuid ?: ""
        // 如果设置了名字与 UUID，则存入数据库
        val appDatabaseExtraData = AppDatabaseExtraData(Gson().toJson(appConfig))
        val targetChecker = appConfig.appConfig?.targetChecker
        // 修改数据库
        val appDatabase = getDatabase(uuid = uuid).also {
            it.name = name
            it.url = url
            it.api_uuid = apiUuid
            // 存储 js 代码
            it.extraData = appDatabaseExtraData
            it.targetChecker = targetChecker
        }
        // 将数据存入 RepoDatabase数据库
        return if (appDatabase.save())
            appDatabase
        else null
    }

    fun del(appDatabase: RepoDatabase) = appDatabase.delete()

    fun getDatabase(databaseId: Int? = null, uuid: String? = null): RepoDatabase {
        var appDatabase: RepoDatabase? = null
        if (uuid != null)
            appDatabase = AppManager.getApp(databaseId, uuid)?.appDatabase
        if (appDatabase == null)
            appDatabase = RepoDatabase("", "", "")
        return appDatabase
    }

    fun translateAppConfig(appDatabase: RepoDatabase): AppConfig {
        val appBaseVersion = MyApplication.context.resources.getInteger(R.integer.app_config_version)
        return AppConfig(
                baseVersion = appBaseVersion,
                uuid = null,
                info = AppConfig.InfoBean(
                        appName = appDatabase.name,
                        configVersion = 1,
                        url = appDatabase.url
                ),
                appConfig = AppConfig.AppConfigBean(
                        hubInfo = AppConfig.AppConfigBean.HubInfoBean(
                                hubUuid = appDatabase.api_uuid
                        ),
                        targetChecker = AppConfig.AppConfigBean.TargetCheckerBean(
                                api = appDatabase.targetChecker?.api,
                                extraString = appDatabase.targetChecker?.extraString
                        )
                ))
    }
}
