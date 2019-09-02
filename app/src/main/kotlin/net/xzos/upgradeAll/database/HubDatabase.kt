package net.xzos.upgradeAll.database

import org.litepal.crud.LitePalSupport

open class HubDatabase(
        var name: String,
        var uuid: String,
        var hub_config: String,
        var extra_data: String
) : LitePalSupport() {
    var id: Int = 0
}