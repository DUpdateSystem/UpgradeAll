package net.xzos.upgradeall.core.database.table.extra_hub.utils

import net.xzos.upgradeall.core.database.table.extra_hub.ExtraHubEntity
import net.xzos.upgradeall.core.utils.URLReplaceData

fun ExtraHubEntity?.toURLReplace() =
    this?.let { if (it.global) null else URLReplaceData(it.urlReplaceSearch, it.urlReplaceString) }
        ?: URLReplaceData(null, null)

fun setExtraHubEntity(
    extraHubEntity: ExtraHubEntity,
    enableGlobal: Boolean,
    urlReplaceData: URLReplaceData
) =
    extraHubEntity.apply {
        global = enableGlobal
        urlReplaceSearch = urlReplaceData.search?.clean()
        urlReplaceString = urlReplaceData.replace?.clean()
    }

fun String.clean(): String? = if (this.isBlank())
    null
else this
