package net.xzos.upgradeall.core.database.table

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import net.xzos.upgradeall.core.data.json.HubConfigGson


@Entity(tableName = "hub")
data class HubEntity(
        @PrimaryKey val uuid: String,
        @ColumnInfo(name = "hub_config") var hubConfig: HubConfigGson,
        var auth: Map<String, String?>,
        @ColumnInfo(name = "ignore_app_id_list") var ignoreAppIdList: HashSet<Map<String, String?>> = hashSetOf()
)