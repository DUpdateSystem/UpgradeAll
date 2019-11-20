package net.xzos.upgradeAll.data.database.litepal

import com.google.gson.Gson
import net.xzos.upgradeAll.data.json.gson.AppConfig
import net.xzos.upgradeAll.data.json.gson.AppDatabaseExtraData
import net.xzos.upgradeAll.utils.VersionChecker
import org.litepal.crud.LitePalSupport

class RepoDatabase(
        var name: String,
        var url: String,
        var api_uuid: String
) : LitePalSupport() {
    val id: Long = 0

    private var extra_data: String? = null
    private var versionChecker: String? = null

    var extraData: AppDatabaseExtraData?
        set(value) {
            if (value != null)
                extra_data = Gson().toJson(value)
        }
        get() {
            return if (extra_data != null)
                Gson().fromJson(extra_data, AppDatabaseExtraData::class.java)
            else null
        }
    var targetChecker: AppConfig.AppConfigBean.TargetCheckerBean?
        get() {
            return if (versionChecker != null) Gson().fromJson(
                    versionChecker, AppConfig.AppConfigBean.TargetCheckerBean::class.java
            ).also {
                // 修补老标准格式
                // TODO: 修改版本: 0.1.0-alpha.beta
                if (it.extraString == null) {
                    val fixChecker = VersionChecker.fixJson(versionChecker!!)
                    it.api = fixChecker.api
                    it.extraString = fixChecker.extraString
                    targetChecker = it
                    this.save()
                }
            }
            else null
        }
        set(value) {
            if (value != null)
                versionChecker = Gson().toJson(value)
        }
}