package net.xzos.upgradeall.core.data

import java.io.File

class CoreConfig(
    // 缓存过期时间
    internal val data_expiration_time: Int,
    // SDK 本地文件缓存地址
    internal val cache_dir: File,
    // 更新服务器地址
    internal val update_server_url: String,
    // 特别的云配置地址
    internal val cloud_rules_hub_url: String?,
    // 应用市场模式下是否忽略系统应用
    internal val applications_ignore_system_app: Boolean,
)