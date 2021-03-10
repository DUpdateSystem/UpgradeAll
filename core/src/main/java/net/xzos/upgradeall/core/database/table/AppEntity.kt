package net.xzos.upgradeall.core.database.table

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import net.xzos.upgradeall.core.data.json.AppConfigGson
import net.xzos.upgradeall.core.utils.cleanBlankValue


@Entity(tableName = "app")
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

fun AppEntity.isInit(): Boolean {
    return 0L != id
}

fun AppEntity.renewData() {
    appId = appId.cleanBlankValue()
}