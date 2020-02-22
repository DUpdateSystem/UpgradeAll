package net.xzos.upgradeall.data_manager.database

import net.xzos.upgradeall.data.database.HubDatabase
import net.xzos.upgradeall.data.json.gson.HubConfig
import net.xzos.upgradeall.data.json.gson.HubDatabaseExtraData
import net.xzos.upgradeall.system_api.api.DatabaseApi


class HubDatabase(
        name: String,
        uuid: String,
        cloudHubConfig: HubConfig?,
        extraData: HubDatabaseExtraData?
) : HubDatabase(name, uuid, cloudHubConfig, extraData) {
    fun save() = DatabaseApi.saveHubDatabase(this)

    fun delete() = DatabaseApi.deleteHubDatabase(this)
}
