package net.xzos.upgradeall.core.data

class CoreConfig(
    internal val data_expiration_time: Int,
    internal val update_server_url: String,
    internal val cloud_rules_hub_url: String?,
    internal val applications_ignore_system_app: Boolean,
)