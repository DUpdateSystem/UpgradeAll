package net.xzos.upgradeall.core.data.config

import net.xzos.upgradeall.core.data.config.AppValue.def_update_server_url
import net.xzos.upgradeall.core.log.Log.VERBOSE
import java.util.*

object AppConfig {
    const val log_level = VERBOSE // TODO: 详细日志模式
    var app_cloud_rules_hub_url: String? = null
    var update_server_url = def_update_server_url
}

object AppValue {
    val locale: Locale = Locale.CHINA
    const val app_config_version = 1
    const val hub_config_version = 3
    const val data_expiration_time = 10 // 默认本地数据缓存时间
    const val default_cloud_rules_hub_url = "FromUpdateServer"
    const val def_update_server_url = "update-server.xzos.net:5255"
    val git_url_arg_regex = "(%.*?)\\w*".toRegex()
    val version_number_strict_match_regex = "\\d+(\\.\\d+)+([.|\\-|+|_| ]*[A-Za-z0-9]+)*".toRegex()
    val version_number_match_regex = "\\d+(\\.\\d+)*([.|\\-|+|_| ]*[A-Za-z0-9]+)*".toRegex()
}
