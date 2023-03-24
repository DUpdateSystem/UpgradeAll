package net.xzos.upgradeall.core.module.app.data

import net.xzos.upgradeall.core.database.table.AppEntity
import net.xzos.upgradeall.core.database.table.getEnableSortHubList
import net.xzos.upgradeall.core.database.table.isInit
import net.xzos.upgradeall.core.database.table.setSortHubUuidList
import net.xzos.upgradeall.core.manager.HubManager
import net.xzos.upgradeall.core.module.Hub

abstract class AppDbWrapper {
    abstract val db: AppEntity

    /* App 对象的属性字典 */
    val appId: Map<String, String?> get() = db.appId

    /* App 名称 */
    val name get() = db.name

    /* 这个 App 已启用的软件源 */
    val hubEnableList: List<Hub>
        get() = db.getEnableSortHubList().filter { it.isValidApp(db.appId) }

    /* 是否星标 */
    val star get() = db.star

    suspend fun setHubList(hubUuidList: List<String>) {
        db.setSortHubUuidList(hubUuidList)
    }

    val isActive: Boolean
        get() {
            hubEnableList.forEach {
                if (it.isActiveApp(appId))
                    return true
            }
            return false
        }

    val isVirtual: Boolean
        get() = !db.isInit()

    /* 获取相应软件源的网址 */
    fun getUrl(hubUuid: String): String? = HubManager.getHub(hubUuid)?.getUrl(this)
}