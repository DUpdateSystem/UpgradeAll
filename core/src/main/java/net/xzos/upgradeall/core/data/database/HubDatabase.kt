package net.xzos.upgradeall.core.data.database

import net.xzos.upgradeall.core.data.json.gson.HubConfigGson


data class HubDatabase(
        var uuid: String,
        var hubConfig: HubConfigGson
)
