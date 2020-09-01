package net.xzos.upgradeall.data.database.table

import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

abstract class BaseAppEntity(
        @PrimaryKey(autoGenerate = true)
        var id: Long,
        var name: String,

        @ColumnInfo(name = "hub_uuid")
        var hubUuid: String,
        var auth: Map<String, String>?,

        @ColumnInfo(name = "extra_id")
        var extraId: Map<String, String>?
)
