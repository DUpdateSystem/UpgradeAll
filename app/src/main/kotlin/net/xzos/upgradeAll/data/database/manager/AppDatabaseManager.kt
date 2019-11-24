package net.xzos.upgradeAll.data.database.manager

import com.google.gson.Gson
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.application.MyApplication
import net.xzos.upgradeAll.data.database.litepal.RepoDatabase
import net.xzos.upgradeAll.data.json.gson.AppConfig
import net.xzos.upgradeAll.data.json.gson.AppDatabaseExtraData
import net.xzos.upgradeAll.server.ServerContainer
import org.litepal.LitePal
import org.litepal.extension.findAll

object AppDatabaseManager {

    private val Log = ServerContainer.Log

    private const val TAG = "AppDatabaseManager"
    private val LogObjectTag = arrayOf("Core", TAG)

    // 读取 apps 数据库
    val appDatabases: List<RepoDatabase>
        get() = LitePal.findAll()

    /**
     * appConfigGson: 软件 json 数据输入
     * id: 指定预期的数据库 ID，若 ID 为 0，表示新建数据库
     */
    fun setDatabase(id: Long, appConfigGson: AppConfig): Boolean {
        val name = appConfigGson.info?.appName
        val url = appConfigGson.info?.url
        val uuid = appConfigGson.uuid
        val apiUuid = appConfigGson.appConfig?.hubInfo?.hubUuid
        // 如果设置了名字与 UUID，则存入数据库
        if (name != null && url != null && apiUuid != null) {
            val appDatabaseExtraData = AppDatabaseExtraData(Gson().toJson(appConfigGson))
            val targetChecker = appConfigGson.appConfig?.targetChecker
            // 修改数据库
            getDatabase(id = id, uuid = uuid)?.also {
                it.name = name
                it.url = url
                it.api_uuid = apiUuid
                // 存储 js 代码
                it.extraData = appDatabaseExtraData
                it.targetChecker = targetChecker
                it.save() // 将数据存入 RepoDatabase数据库
                ServerContainer.AppManager.setApp(it.id)  // 更新相关跟踪项
                return true
            } ?: if (id == 0L) {
                RepoDatabase(
                        name = name,
                        url = url,
                        api_uuid = apiUuid
                ).apply {
                    this.extraData = appDatabaseExtraData
                    this.targetChecker = targetChecker
                }.also {
                    it.save()
                }
                return true
            }
        }
        return false
    }

    fun del(id: Long) {
        LitePal.delete(RepoDatabase::class.java, id)
    }

    fun exists(uuid: String?): Boolean {
        return getDatabase(uuid = uuid) != null
    }

    fun getDatabase(id: Long = 0, uuid: String? = null): RepoDatabase? {
        return when {
            id != 0L -> {
                LitePal.find(RepoDatabase::class.java, id)
            }
            uuid != null -> {
                for (appDatabase in appDatabases) {
                    val itemUuid = appDatabase.extraData?.getCloudAppConfig()?.uuid
                    if (itemUuid == uuid)
                        return appDatabase
                }
                return null
            }
            else -> null
        }
    }

    fun getAppConfig(id: Long): AppConfig? {
        getDatabase(id = id)?.let {
            val appBaseVersion = MyApplication.context.resources.getInteger(R.integer.app_config_version)
            return AppConfig(
                    baseVersion = appBaseVersion,
                    uuid = null,
                    info = AppConfig.InfoBean(
                            appName = it.name,
                            configVersion = 1,
                            url = it.url
                    ),
                    appConfig = AppConfig.AppConfigBean(
                            hubInfo = AppConfig.AppConfigBean.HubInfoBean(
                                    hubUuid = it.api_uuid
                            ),
                            targetChecker = AppConfig.AppConfigBean.TargetCheckerBean(
                                    api = it.targetChecker?.api,
                                    extraString = it.targetChecker?.extraString
                            )
                    ))
        } ?: return null
    }
}