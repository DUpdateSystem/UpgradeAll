package net.xzos.upgradeall.core.database.table

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.xzos.upgradeall.core.data.json.AppConfigGson
import net.xzos.upgradeall.core.database.metaDatabase
import net.xzos.upgradeall.core.manager.HubManager
import net.xzos.upgradeall.core.module.Hub
import net.xzos.upgradeall.core.utils.cleanBlankValue


@Entity(tableName = "app")
class AppEntity(
        @PrimaryKey(autoGenerate = true)
        val id: Long,
        var name: String,
        @ColumnInfo(name = "app_id") var appId: Map<String, String?>,
        @ColumnInfo(name = "invalid_version_number_field_regex") var invalidVersionNumberFieldRegexString: String? = null,
        @ColumnInfo(name = "ignore_version_number") var ignoreVersionNumber: String? = null,
        @ColumnInfo(name = "cloud_config") var cloudConfig: AppConfigGson? = null,
        @ColumnInfo(name = "enable_hub_list") var _enableHubUuidListString: String? = null,
) {
    /** @return 软件源的排序列表 与 其是否被使用 */
    fun getSortHubUuidList(): List<String> {
        return sortHubUuidListStringToList(_enableHubUuidListString)
    }

    suspend fun setSortHubUuidList(sortHubUuidList: List<String>) {
        _enableHubUuidListString = sortHubUuidListToString(sortHubUuidList)
        withContext(Dispatchers.Default) {
            metaDatabase.appDao().update(this@AppEntity)
        }
    }

    private fun sortHubUuidListToString(sortHubUuidList: List<String>): String? {
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
    invalidVersionNumberFieldRegexString = if (invalidVersionNumberFieldRegexString.isNullOrBlank()) null
    else invalidVersionNumberFieldRegexString
}

fun AppEntity.getEnableSortHubList(): List<Hub> {
    val sortHubUuidList = this.getSortHubUuidList()
    return if (sortHubUuidList.isEmpty()) {
        val allHubList = HubManager.getHubList()
        if (isInit()) allHubList
        else allHubList.filter { it.isEnableApplicationsMode() }
    } else sortHubUuidList.mapNotNull { HubManager.getHub(it) }
}

suspend fun AppEntity.setEnableSortHubList(hubList: List<Hub>) {
    this.setSortHubUuidList(hubList.map { it.uuid })
}