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
) : Database, HubDatabase(name, uuid, cloudHubConfig, extraData) {

    override fun save() = DatabaseApi.saveHubDatabase(this) != 0L

    override fun delete() = DatabaseApi.deleteHubDatabase(this)

    companion object {
        fun newInstance() =
                net.xzos.upgradeall.data_manager.database.HubDatabase("", "", null, null)
    }
}
