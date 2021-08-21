package net.xzos.upgradeall.core

import android.annotation.SuppressLint
import net.xzos.upgradeall.core.data.CoreConfig
import net.xzos.upgradeall.core.manager.AppManager
import net.xzos.upgradeall.core.manager.HubManager
import net.xzos.upgradeall.core.module.network.renewChannel
import net.xzos.upgradeall.core.websdk.ServerApi


@SuppressLint("StaticFieldLeak")
lateinit var coreConfig: CoreConfig

lateinit var serverApi: ServerApi

/**
 * 初始化 Core 的配置，也用作用户修改设置后传入新的设置
 * @param _coreConfig Core 运行的核心设置
 */
fun initCore(
    _coreConfig: CoreConfig,
) {
    coreConfig = _coreConfig
    if (::serverApi.isInitialized) serverApi.shutdown()
    serverApi = ServerApi(_coreConfig.update_server_url, _coreConfig.data_expiration_time)
    initObject()
    renewChannel(false)
}

// 提前初始化 Object，避免多线程时属性初始化错误
private fun initObject() {
    HubManager
    AppManager
}