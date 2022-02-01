package net.xzos.upgradeall.core.database.table.extra_app

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "extra_app")
data class ExtraAppEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo(name = "app_id") var appId: Map<String, String?>,
    @ColumnInfo(name = "mark_version_number") var __mark_version_number: String? = null,
)