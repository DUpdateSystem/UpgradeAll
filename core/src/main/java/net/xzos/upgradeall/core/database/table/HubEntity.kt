package net.xzos.upgradeall.core.database.table

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import net.xzos.upgradeall.core.data.json.HubConfigGson
import net.xzos.upgradeall.core.utils.coroutines.CoroutinesMutableList
import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableListOf


@Entity(tableName = "hub")
data class HubEntity(
        @PrimaryKey val uuid: String,
        @ColumnInfo(name = "hub_config") var hubConfig: HubConfigGson,
        @ColumnInfo(name = "auth") var auth: MutableMap<String, String?>,
        @ColumnInfo(name = "ignore_app_id_list") var ignoreAppIdList: CoroutinesMutableList<Map<String, String?>> = coroutinesMutableListOf(true),
        @ColumnInfo(name = "applications_mode") var _applicationsMode: Int = 0,
        @ColumnInfo(name = "user_ignore_app_id_list") var userIgnoreAppIdList: CoroutinesMutableList<Map<String, String?>> = coroutinesMutableListOf(true),
) {
    fun getApplicationsMode(): Boolean = _applicationsMode == 1
    fun setApplicationsMode(enable: Boolean) {
        _applicationsMode = if (enable) 1 else 0
    }
}