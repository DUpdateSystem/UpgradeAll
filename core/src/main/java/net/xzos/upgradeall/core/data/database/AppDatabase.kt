package net.xzos.upgradeall.core.data.database

import net.xzos.upgradeall.core.data.json.gson.AppConfigGson
import net.xzos.upgradeall.core.data.json.gson.PackageIdGson


class AppDatabase(
        id: Long,
        name: String,
        hubUuid: String,
        var url: String,
        var packageId: PackageIdGson? = null,
        var cloudConfig: AppConfigGson? = null,
        auth: Map<String, String?> = mapOf(),
        extraId: Map<String, String?> = mapOf(),
        var ignoreVersionNumber: String? = null
) : BaseAppDatabase(id, name, hubUuid, auth, extraId)
