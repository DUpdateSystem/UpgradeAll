package net.xzos.upgradeAll.database

import org.litepal.crud.LitePalSupport

class RepoDatabase(
        var name: String,
        var url: String,
        var api: String,
        var api_uuid: String,
        var extra_data: String = "",
        var versionChecker: String
) : LitePalSupport() {
    val id: Long = 0
}