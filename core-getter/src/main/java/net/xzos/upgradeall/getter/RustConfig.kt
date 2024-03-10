package net.xzos.upgradeall.getter

import java.io.File

class RustConfig(
    internal val cacheDir: File,
    internal val dataDir: File,
    internal val globalExpireTime: Long,
)