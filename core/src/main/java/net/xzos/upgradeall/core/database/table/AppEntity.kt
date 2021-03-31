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
        @ColumnInfo(name = "invalid_version_number_field_regex")
        var invalidVersionNumberFieldRegexString: String? = null,
        @ColumnInfo(name = "ignore_version_number")
        var ignoreVersionNumber: String? = null,
        @ColumnInfo(name = "cloud_config")
        var cloudConfig: AppConfigGson? = null,
)

fun AppEntity.getInvalidVersionNumberFieldRegex(): Regex? = invalidVersionNumberFieldRegexString?.toRegex()

fun AppEntity.isInit(): Boolean {
    return 0L != id
}

fun AppEntity.recheck() {
    appId = appId.cleanBlankValue()
    invalidVersionNumberFieldRegexString = if (invalidVersionNumberFieldRegexString.isNullOrBlank()) null
    else invalidVersionNumberFieldRegexString
}