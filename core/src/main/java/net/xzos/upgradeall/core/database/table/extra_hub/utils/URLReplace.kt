package net.xzos.upgradeall.core.database.table.extra_hub.utils

import net.xzos.upgradeall.core.database.table.extra_hub.ExtraHubEntity
import net.xzos.upgradeall.core.utils.URLReplace

fun ExtraHubEntity?.toURLReplace() =
    this?.let { URLReplace(it.urlReplaceSearch, it.urlReplaceString) }
        ?: URLReplace(null, null)

fun setExtraHubEntity(extraHubEntity: ExtraHubEntity, urlReplace: URLReplace) =
    extraHubEntity.apply {
        global = false
        urlReplaceSearch = urlReplace.search?.clean()
        urlReplaceString = urlReplace.replace?.clean()
    }

fun String.clean(): String? = if (this.isBlank())
    null
else this
