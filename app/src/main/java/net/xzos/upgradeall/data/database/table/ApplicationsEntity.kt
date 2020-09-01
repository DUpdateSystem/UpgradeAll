package net.xzos.upgradeall.data.database.table

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import net.xzos.upgradeall.core.data.json.gson.IgnoreApp

@Entity(tableName = "applications", indices = [Index(value = ["hub_uuid"], unique = true, name = "applications_key_value")])
class ApplicationsEntity(
        id: Long,
        name: String,
        hubUuid: String,
        auth: Map<String, String>,
        extraId: Map<String, String>,
        @ColumnInfo(name = "invalid_package_list")
        var invalidPackageList: MutableList<Map<String, String>>?,
        @ColumnInfo(name = "ignore_app_list")
        var ignoreApps: MutableList<IgnoreApp>?
) : BaseAppEntity(id, name, hubUuid, auth, extraId)

