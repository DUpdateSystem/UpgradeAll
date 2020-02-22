package net.xzos.upgradeall.data.config

import java.util.*

object AppConfig {
    val locale = Locale.CHINA
    const val log_level = 1
    const val app_config_version = 1
    const val hub_config_version = 3
    const val default_data_expiration_time = 10
    const val default_background_sync_data_time = 18

    const val default_cloud_rules_hub_url = "https://github.com/DUpdateSystem/UpgradeAll-rules/"

    const val github_url = "https://github.com/%owner/%repo/tree/%branch"
    const val github_raw_url = "https://raw.githubusercontent.com/%owner/%repo/%branch/%path"
    const val cdn_github_url = "https://cdn.jsdelivr.net/gh/%owner/%repo@%branch"
    const val cdn_github_raw_url = "https://cdn.jsdelivr.net/gh/%owner/%repo@%branch/%path"
    const val coding_url = "https://%owner.coding.net/p/%project/d/%repo/git/tree/%branch"
    const val coding_raw_url = "https://%owner.coding.net/p/%project/d/%repo/git/raw/%branch/%path"

    val git_url_arg_regex = "(%.*?)\\w*".toRegex()
    val version_number_match_regex = "(\\d+(\\.\\d+)*)(([.|\\-|+|_| ]|[0-9A-Za-z])*)".toRegex()
}