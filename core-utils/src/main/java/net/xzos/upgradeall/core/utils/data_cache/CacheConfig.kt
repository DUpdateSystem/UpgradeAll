package net.xzos.upgradeall.core.utils.data_cache

import java.io.File

class CacheConfig(
    val defExpires: Int,
    val dir: File?,
    val autoRemove: Boolean = true,
)