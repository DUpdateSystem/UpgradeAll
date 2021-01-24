package net.xzos.upgradeall.core.database.table

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import net.xzos.upgradeall.core.data.json.AppConfigGson


@Entity(
        tableName = "app",
        indices = [Index(
                value = ["app_id"],
                unique = true,
                name = "app_key_value"
        )]
)
class AppEntity(
        @PrimaryKey(autoGenerate = true)
        val id: Long,
        var name: String,
        @ColumnInfo(name = "app_id")
        var appId: Map<String, String?>,
        @ColumnInfo(name = "ignore_version_number")
        var ignoreVersionNumber: String? = null,
        @ColumnInfo(name = "cloud_config")
        var cloudConfig: AppConfigGson? = null,
)