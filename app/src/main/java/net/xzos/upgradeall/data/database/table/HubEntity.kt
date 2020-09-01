package net.xzos.upgradeall.data.database.table

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import net.xzos.upgradeall.core.data.json.gson.HubConfigGson


@Entity(tableName = "hub")
data class HubEntity(
        @PrimaryKey var uuid: String,
        @ColumnInfo(name = "hub_config")
        var hubConfig: HubConfigGson
)
