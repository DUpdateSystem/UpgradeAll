package net.xzos.upgradeall.data.database.table

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import net.xzos.upgradeall.core.data.json.gson.AppConfigGson
import net.xzos.upgradeall.core.data.json.gson.PackageIdGson


@Entity(tableName = "app", indices = [Index(value = ["name", "hub_uuid", "url", "package_id"], unique = true, name = "app_key_value")])
class AppEntity(
        id: Long,
        name: String,
        hubUuid: String,
        auth: Map<String, String>,
        extraId: Map<String, String>,
        var url: String,
        @ColumnInfo(name = "package_id")
        var packageId: PackageIdGson? = null,
        @ColumnInfo(name = "ignore_version_number")
        var ignoreVersionNumber: String? = null,
        @ColumnInfo(name = "cloud_config")
        var cloudConfig: AppConfigGson? = null
) : BaseAppEntity(id, name, hubUuid, auth, extraId)
