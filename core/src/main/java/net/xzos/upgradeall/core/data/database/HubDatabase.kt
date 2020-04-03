package net.xzos.upgradeall.core.data.database

import net.xzos.upgradeall.core.data.json.gson.HubConfig
import net.xzos.upgradeall.core.data_manager.HubDatabaseManager


data class HubDatabase(
    var uuid: String,
    var hubConfig: HubConfig
) {

    fun save(): Boolean = HubDatabaseManager.saveDatabase(this)

    fun delete(): Boolean = HubDatabaseManager.deleteDatabase(this)

    companion object {
        fun newInstance() = HubDatabase("", HubConfig())
    }
}
