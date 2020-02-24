package net.xzos.upgradeall.data.database

import net.xzos.upgradeall.data.json.gson.HubConfig
import net.xzos.upgradeall.data.json.gson.HubDatabaseExtraData


class HubDatabase(
        var name: String,
        var uuid: String,
        var cloudHubConfig: HubConfig?,
        var extraData: HubDatabaseExtraData?) {

    companion object {
        fun newInstance() = HubDatabase("", "", null, null)
    }
}
