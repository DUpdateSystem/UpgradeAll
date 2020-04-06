package net.xzos.upgradeall.core.data.config

import net.xzos.upgradeall.core.network_api.GrpcApi
import java.util.*

object AppConfig {
    val locale = Locale.CHINA
    const val log_level = 1
    const val app_config_version = 1
    const val hub_config_version = 3
    val hub_web_crawler_tool_list = listOf("JavaScript")
    var data_expiration_time = 10
    var update_server_url = "update-server.xzos.net:5255"
        private set
    const val default_background_sync_data_time = 18

    const val default_cloud_rules_hub_url = "https://github.com/DUpdateSystem/UpgradeAll-rules/"

    var update_server_url_template = "http://%host:%port"
    const val github_url = "https://github.com/%owner/%repo/tree/%branch"
    const val github_raw_url = "https://raw.githubusercontent.com/%owner/%repo/%branch/%path"
    const val cdn_github_url = "https://cdn.jsdelivr.net/gh/%owner/%repo@%branch"
    const val cdn_github_raw_url = "https://cdn.jsdelivr.net/gh/%owner/%repo@%branch/%path"
    const val coding_url = "https://%owner.coding.net/p/%project/d/%repo/git/tree/%branch"
    const val coding_raw_url = "https://%owner.coding.net/p/%project/d/%repo/git/raw/%branch/%path"

    val git_url_arg_regex = "(%.*?)\\w*".toRegex()
    val version_number_match_regex = "(\\d+(\\.\\d+)*)(([.|\\-|+|_| ]|[0-9A-Za-z])*)".toRegex()

    fun setUpdateServerUrl(url: String?) {
        if (url.isNullOrBlank() || url == update_server_url) return
        GrpcApi
        val old = update_server_url
        try {
            update_server_url = url
            GrpcApi.renew()
        } catch (e: IllegalArgumentException) {
            update_server_url = old
        }
    }
}