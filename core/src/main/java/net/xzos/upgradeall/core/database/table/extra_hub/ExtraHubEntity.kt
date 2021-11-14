package net.xzos.upgradeall.core.database.table.extra_hub

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "extra_hub")
data class ExtraHubEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "enable_global") var global: Boolean = false,
    @ColumnInfo(name = "url_replace_search") var urlReplaceSearch: String? = null,
    @ColumnInfo(name = "url_replace_string") var urlReplaceString: String? = null,
)

const val GLOBAL = "GLOBAL"
