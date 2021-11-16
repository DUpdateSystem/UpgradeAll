package net.xzos.upgradeall.core.utils.data_cache

fun <E> DataCache.getValueIfNocache(key: String, backupFunction: () -> E?): E? {
    return this.get(key) ?: backupFunction()?.also { this.cache(key, it) }
}