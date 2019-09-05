package net.xzos.upgradeAll.server.app.engine.api

import net.xzos.upgradeAll.server.ServerContainer
import org.jetbrains.annotations.Contract
import org.json.JSONObject
import java.util.*

abstract class EngineApi : CoreApi {

    companion object {

        protected val Log = ServerContainer.Log

        internal val emptyEngine: EmptyEngine
            @Contract(" -> new")
            get() = EmptyEngine()
    }
}

/**
 * 生成空 engine 避免错误
 * TODO: 0.1.0 前核实去除方法
 */
internal class EmptyEngine : EngineApi() {

    override suspend fun getDefaultName(): String? = null

    override suspend fun getReleaseNum(): Int = 0

    override suspend fun getVersioning(releaseNum: Int): String? = null

    override suspend fun getChangelog(releaseNum: Int): String? = null

    override suspend fun getReleaseDownload(releaseNum: Int) = JSONObject()
}
