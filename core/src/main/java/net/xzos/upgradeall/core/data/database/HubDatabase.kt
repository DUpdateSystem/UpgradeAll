package net.xzos.upgradeall.core.data.database

import net.xzos.upgradeall.core.data.json.gson.HubConfigGson
import net.xzos.upgradeall.core.data_manager.HubDatabaseManager


data class HubDatabase(
    var uuid: String,
    var hubConfig: HubConfigGson
) {

    fun save(): Boolean = HubDatabaseManager.saveDatabase(this)

    fun delete(): Boolean = HubDatabaseManager.deleteDatabase(this)

    companion object {
        fun newInstance() = HubDatabase("", HubConfigGson())
    }
}
