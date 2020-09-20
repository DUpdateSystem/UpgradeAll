package net.xzos.upgradeall.core.data.config

import net.xzos.upgradeall.core.log.Log.VERBOSE
import net.xzos.upgradeall.core.network_api.GrpcApi
import net.xzos.upgradeall.core.network_api.GrpcApi.Companion.grpcApi
import java.util.*

object AppConfig {
    const val log_level = VERBOSE // TODO: 详细日志模式
    var app_cloud_rules_hub_url: String? = null
    var update_server_url = "update-server.xzos.net:5255"
        set(value) {
            if (value != field)
                if (grpcApi.setUpdateServerUrl(value))
                    field = value
        }
}

object AppValue {
    val locale: Locale = Locale.CHINA
    const val app_config_version = 1
    const val hub_config_version = 3
    const val data_expiration_time = 30 // 默认本地数据缓存时间
    const val default_cloud_rules_hub_url = "FromUpdateServer"
    val git_url_arg_regex = "(%.*?)\\w*".toRegex()
    val version_number_match_regex = "(\\d+(\\.\\d+)*)(([.|\\-|+|_| ]|[0-9A-Za-z])*)".toRegex()
}
