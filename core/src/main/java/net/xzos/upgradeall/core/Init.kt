package net.xzos.upgradeall.core

import android.annotation.SuppressLint
import android.content.Context
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeall.core.data.CoreConfig
import net.xzos.upgradeall.core.database.initDatabase
import net.xzos.upgradeall.core.manager.AppManager
import net.xzos.upgradeall.core.manager.HubManager
import net.xzos.upgradeall.core.utils.data_cache.CacheConfig
import net.xzos.upgradeall.core.websdk.initRustSdkApi
import net.xzos.upgradeall.core.websdk.initSdkCache
import net.xzos.upgradeall.core.websdk.renewSdkApi
import net.xzos.upgradeall.core.websdk.runGetterService


@SuppressLint("StaticFieldLeak")
lateinit var coreConfig: CoreConfig

/**
 * 初始化 Core 的配置，也用作用户修改设置后传入新的设置
 * @param context Core 初始化时用的 Context
 * @param _coreConfig Core 运行的核心设置
 */
fun initCore(
    context: Context,
    _coreConfig: CoreConfig,
) {
    coreConfig = _coreConfig
    initSdkCache(CacheConfig(_coreConfig.data_expiration_time, _coreConfig.cache_dir))
    initRustSdkApi(
        _coreConfig.rust_data_dir,
        _coreConfig.rust_cache_dir,
        _coreConfig.data_expiration_time.toLong(),
    )
    GlobalScope.launch {
        runGetterService(context)
    }
    renewSdkApi(_coreConfig.update_server_url)
    initObject(context)
}

/**
 *  提前初始化 Object，避免多线程时属性初始化错误
 */
private fun initObject(context: Context) {
    initDatabase(context)
    HubManager
    AppManager.initObject(context)
}