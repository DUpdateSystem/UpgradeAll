package net.xzos.upgradeall.core.module.app.version

import net.xzos.upgradeall.core.database.metaDatabase
import net.xzos.upgradeall.core.database.table.AppEntity
import net.xzos.upgradeall.core.database.table.extra_app.ExtraAppEntityManager
import net.xzos.upgradeall.core.database.table.isInit
import net.xzos.upgradeall.core.module.app.version_item.Asset
import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableListOf
import net.xzos.upgradeall.core.utils.coroutines.toCoroutinesMutableList
import net.xzos.upgradeall.core.utils.log.Log
import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.core.utils.log.ObjectTag.Companion.core
import net.xzos.upgradeall.core.utils.versioning.VersioningUtils

/**
 * 版本号数据
 */
class VersionUtils internal constructor(private val appEntity: AppEntity) {

    suspend fun isIgnored(versionNumber: String): Boolean {
        val markedVersionNumber = if (appEntity.isInit())
            appEntity.ignoreVersionNumber
        else ExtraAppEntityManager.getMarkVersionNumber(appEntity.appId)
        return VersioningUtils.compareVersionNumber(versionNumber, markedVersionNumber) == 0
    }

    suspend fun switchIgnoreStatus(versionNumber: String) {
        if (isIgnored(versionNumber)) unignore()
        else ignore(versionNumber)
    }

    /* 忽略这个版本 */
    private suspend fun ignore(versionNumber: String) {
        appEntity.ignoreVersionNumber = versionNumber
        if (appEntity.isInit())
            metaDatabase.appDao().update(appEntity)
        else
            ExtraAppEntityManager.addMarkVersionNumber(appEntity.appId, versionNumber)
    }

    /* 取消忽略这个版本 */
    private suspend fun unignore() {
        if (appEntity.isInit()) {
            appEntity.ignoreVersionNumber = null
            metaDatabase.appDao().update(appEntity)
        } else
            ExtraAppEntityManager.removeMarkVersionNumber(appEntity.appId)
    }

    internal fun doAddAsset(
        assetList: List<Asset>, versionMap: MutableMap<String, Version>
    ): List<Version> {
        assetList.forEach { asset ->
            val key = getKeyVersionNumber(
                asset.versionNumber, appEntity.invalidVersionNumberFieldRegexString
            )
            val mapKey = getKey(key)
            if (mapKey.isBlank()) return@forEach
            val list = versionMap[mapKey]
                ?: Version(key, coroutinesMutableListOf(true), this).apply {
                    versionMap[mapKey] = this
                }
            list.assetList.add(asset)
        }
        return versionMap.values.toList()
    }

    companion object {
        private const val TAG = "VersionUtils"
        private val logObjectTag = ObjectTag(core, TAG)

        fun getKey(raw: List<Pair<Char, Boolean>>): String =
            raw.filter { it.second }.map { it.first }.joinToString(separator = "")

        internal fun cleanAsset(
            hubUuid: String,
            _versionList: List<Version>
        ): MutableList<Version> {
            val versionList = _versionList.toCoroutinesMutableList(true)
            versionList.forEach { version ->
                version.assetList.forEach {
                    if (it.hub.uuid == hubUuid)
                        version.assetList.remove(it)
                }
                if (version.assetList.isEmpty())
                    versionList.remove(version)
            }
            return versionList.toMutableList()
        }

        fun getKeyVersionNumber(
            rawVersionNumber: String, invalidStringFieldRegexString: String?
        ): List<Pair<Char, Boolean>> {
            val preVersionNumberInfoList =
                rawVersionNumber.map { Pair(it, true) }.toMutableList()
            invalidStringFieldRegexString?.run {
                try {
                    getInvalidVersionNumberIndex(rawVersionNumber, this).let {
                        setVersionNumberInfoList(preVersionNumberInfoList, it)
                    }
                } catch (e: Throwable) {
                    Log.e(logObjectTag, TAG, " getKeyVersionNumber: ${e.stackTraceToString()}")
                }
            }

            val indexMap = mutableMapOf<Int, Int>()
            val preVersionNumber = StringBuilder()
            preVersionNumberInfoList.forEachIndexed { index, pair ->
                if (pair.second) {
                    preVersionNumber.append(pair.first)
                    indexMap[preVersionNumber.length - 1] = index
                }
            }

            val finishVersionNumberInfoList =
                rawVersionNumber.map { Pair(it, false) }.toMutableList()
            VersioningUtils.matchVersioningString(preVersionNumber)?.range?.run {
                val versionNumberIndexList = mutableListOf<Int>()
                for (index in this.first..this.last) {
                    versionNumberIndexList.add(indexMap[index]!!)
                }
                setVersionNumberInfoList(
                    finishVersionNumberInfoList,
                    versionNumberIndexList,
                    true
                )
            }
            return finishVersionNumberInfoList
        }

        private fun setVersionNumberInfoList(
            versionNumberInfoList: MutableList<Pair<Char, Boolean>>,
            versionNumberIndexList: List<Int>, enable: Boolean = false
        ) {
            versionNumberIndexList.forEach {
                val newValue = versionNumberInfoList[it].copy(second = enable)
                versionNumberInfoList.removeAt(it)
                versionNumberInfoList.add(it, newValue)
            }
        }

        fun getInvalidVersionNumberIndex(
            rawVersionNumber: String,
            invalidVersionNumberFieldRegexString: String
        ): List<Int> {
            val invalidVersionNumberFieldRegex = invalidVersionNumberFieldRegexString.toRegex()
            val list = invalidVersionNumberFieldRegex.findAll(rawVersionNumber)
            val indexList = mutableListOf<Int>()
            list.forEach {
                indexList.addAll(it.range.first..it.range.last)
            }
            return indexList
        }
    }
}