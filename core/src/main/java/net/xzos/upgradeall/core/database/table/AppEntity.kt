package net.xzos.upgradeall.core.database.table

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.xzos.upgradeall.core.database.metaDatabase
import net.xzos.upgradeall.core.manager.HubManager
import net.xzos.upgradeall.core.module.Hub
import net.xzos.upgradeall.core.module.app.version.VersionInfo
import net.xzos.upgradeall.core.utils.cleanBlankValue
import net.xzos.upgradeall.core.websdk.json.AppConfigGson


// 使用 data 保持 hashcode 相同
// 并在上层调用中使用 data 数据类标识，以保持 hashcode 为准的相等判断
@Entity(tableName = "app")
data class AppEntity(
    @ColumnInfo(name = "name") var name: String,
    @ColumnInfo(name = "app_id") var appId: Map<String, String?>,
    @ColumnInfo(name = "invalid_version_number_field_regex") var invalidVersionNumberFieldRegexString: String? = null,
    @ColumnInfo(name = "ignore_version_number") var ignoreVersionNumber: String? = null,
    @ColumnInfo(name = "cloud_config") var cloudConfig: AppConfigGson? = null,
    @ColumnInfo(name = "enable_hub_list") var _enableHubUuidListString: String? = null,
    @ColumnInfo(name = "star") var startRaw: Boolean? = null,
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
) {

    var star: Boolean
        get() = startRaw == true
        set(value) = if (value) startRaw = value else startRaw = null

    fun getIgnoreVersion() = ignoreVersionNumber?.let { VersionInfo.new(it) }

    /** @return 软件源的排序列表 与 其是否被使用 */
    fun getSortHubUuidList(): List<String> {
        return sortHubUuidListStringToList(_enableHubUuidListString)
    }

    internal suspend fun rowSetSortHubUuidList(sortHubUuidList: Collection<String>) {
        _enableHubUuidListString = sortHubUuidListToString(sortHubUuidList)
        withContext(Dispatchers.Default) {
            metaDatabase.appDao().update(this@AppEntity)
        }
    }

    private fun sortHubUuidListToString(sortHubUuidList: Collection<String>): String? {
        val s = sortHubUuidList.joinToString(separator = " ")
        return if (s.isNotBlank()) s
        else null
    }

    private fun sortHubUuidListStringToList(sortHubUuidListString: String?): List<String> {
        return sortHubUuidListString?.split(" ") ?: listOf()
    }
}

fun AppEntity.isInit(): Boolean {
    return 0L != id
}

fun AppEntity.recheck() {
    appId = appId.cleanBlankValue()
    invalidVersionNumberFieldRegexString =
        if (invalidVersionNumberFieldRegexString.isNullOrBlank()) null
        else invalidVersionNumberFieldRegexString
}

fun AppEntity.getEnableSortHubList(): List<Hub> {
    val sortHubUuidList = this.getSortHubUuidList().toMutableList().also {
        if (cloudConfig != null) {
            val hubUuid = cloudConfig!!.baseHubUuid
            it.remove(hubUuid)
            it.add(0, hubUuid)
        }
    }
    return if (sortHubUuidList.isEmpty()) {
        val allHubList = HubManager.getHubList()
        if (isInit()) allHubList
        else allHubList.filter { it.isEnableApplicationsMode() }
    } else sortHubUuidList.mapNotNull { HubManager.getHub(it) }
}

suspend fun AppEntity.setSortHubUuidList(sortHubUuidList: Collection<String>) {
    if (this.isInit())
        this.rowSetSortHubUuidList(sortHubUuidList)
    else {
        HubManager.getHubList().forEach {
            if (sortHubUuidList.contains(it.uuid))
                it.unignoreApp(appId)
            else
                it.ignoreApp(appId)
        }
    }
}