package net.xzos.upgradeAll.database

import android.util.Log
import com.google.gson.Gson
import net.xzos.upgradeAll.json.gson.VersionCheckerGson
import org.litepal.crud.LitePalSupport

class RepoDatabase(
        var name: String,
        var url: String,
        var api: String,
        var api_uuid: String
) : LitePalSupport() {
    val id: Long = 0

    private var extra_data: String? = null
    private var versionChecker: String? = null

    var versionCheckerGson: VersionCheckerGson?
        get() {
            return if (versionChecker != null) Gson().fromJson(versionChecker, VersionCheckerGson::class.java)
            else null
        }
        set(value) {
            if (value != null)
                versionChecker = Gson().toJson(value)
        }
}