package net.xzos.upgradeall.core.data.config

import net.xzos.upgradeall.core.log.Log.VERBOSE
import net.xzos.upgradeall.core.network_api.GrpcApi
import java.util.*

object AppConfig {
    const val log_level = VERBOSE // TODO: 详细日志模式
    var update_server_url = "update-server.xzos.net:5255"
        set(value) {
            if (GrpcApi.setUpdateServerUrl(value)) {
                field = value
            }
        }
}

object AppValue {
    val locale: Locale = Locale.CHINA
    const val app_config_version = 1
    const val hub_config_version = 3
    const val data_expiration_time = 30 // 默认本地数据缓存时间
    const val default_cloud_rules_hub_url = "https://github.com/DUpdateSystem/UpgradeAll-rules/tree/master"
    const val update_server_url_template = "http://%host:%port"
    const val github_url = "https://github.com/%owner/%repo/tree/%branch"
    const val github_raw_url = "https://raw.githubusercontent.com/%owner/%repo/%branch/%path"
    const val cdn_github_url = "https://cdn.jsdelivr.net/gh/%owner/%repo@%branch"
    const val cdn_github_raw_url = "https://cdn.jsdelivr.net/gh/%owner/%repo@%branch/%path"
    const val coding_url = "https://%owner.coding.net/p/%project/d/%repo/git/tree/%branch"
    const val coding_raw_url = "https://%owner.coding.net/p/%project/d/%repo/git/raw/%branch/%path"
    val git_url_arg_regex = "(%.*?)\\w*".toRegex()
    val version_number_match_regex = "(\\d+(\\.\\d+)*)(([.|\\-|+|_| ]|[0-9A-Za-z])*)".toRegex()
}
