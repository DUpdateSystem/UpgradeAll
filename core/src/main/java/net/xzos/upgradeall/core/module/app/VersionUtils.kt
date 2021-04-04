package net.xzos.upgradeall.core.module.app

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.core.database.metaDatabase
import net.xzos.upgradeall.core.database.table.AppEntity
import net.xzos.upgradeall.core.utils.VersioningUtils
import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableListOf
import net.xzos.upgradeall.core.utils.coroutines.toCoroutinesMutableList

/**
 * 版本号数据
 */
class VersionUtils(
        private val appEntity: AppEntity
) {

    /* 版本号数据列表 */
    private var versionList: List<Version> = emptyList()
    private var versionListMutex = Mutex()

    fun getVersionList(): List<Version> {
        return versionList
    }

    suspend fun addAsset(assetList: List<Asset>, cleanHubUuid: String) {
        versionListMutex.withLock {
            val rowVersionList = cleanAsset(cleanHubUuid, versionList)
            val versionMap = rowVersionList.map { it.name to it }.toMap().toMutableMap()
            val versionList = doAddAsset(assetList, versionMap)
            val a = versionList.sortedWith(versionComparator)
            this.versionList = a
        }
    }

    private fun doAddAsset(assetList: List<Asset>, versionMap: MutableMap<String, Version>): List<Version> {
        assetList.forEach { asset ->
            val key = getKeyVersionNumber(asset)
            val mapKey = Version.getKey(key)
            val list = versionMap[mapKey]
                    ?: Version(key, coroutinesMutableListOf(true), this)
            list.assetList.add(asset)
            versionMap[mapKey] = list
        }
        return versionMap.values.toList()
    }

    private fun cleanAsset(hubUuid: String, _versionList: List<Version>): MutableList<Version> {
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

    private fun getKeyVersionNumber(asset: Asset): List<Pair<Char, Boolean>> {
        val rawVersionNumber = asset.versionNumber
        val preVersionNumberInfoList = rawVersionNumber.map { Pair(it, true) }.toMutableList()
        appEntity.invalidVersionNumberFieldRegexString?.run {
            val invalidVersionNumberIndexList = getInvalidVersionNumberIndex(rawVersionNumber, this)
            setVersionNumberInfoList(preVersionNumberInfoList, invalidVersionNumberIndexList)
        }

        val indexMap = mutableMapOf<Int, Int>()
        val preVersionNumber = StringBuilder()
        preVersionNumberInfoList.forEachIndexed { index, pair ->
            if (pair.second) {
                preVersionNumber.append(pair.first)
                indexMap[preVersionNumber.length - 1] = index
            }
        }

        val finishVersionNumberInfoList = rawVersionNumber.map { Pair(it, false) }.toMutableList()
        VersioningUtils.matchVersioningString(preVersionNumber)?.range?.run {
            val versionNumberIndexList = mutableListOf<Int>()
            for (index in this.first..this.last) {
                versionNumberIndexList.add(indexMap[index]!!)
            }
            setVersionNumberInfoList(finishVersionNumberInfoList, versionNumberIndexList, true)
        }
        return finishVersionNumberInfoList
    }

    private fun getInvalidVersionNumberIndex(rawVersionNumber: String, invalidVersionNumberFieldRegexString: String): List<Int> {
        val invalidVersionNumberFieldRegex = invalidVersionNumberFieldRegexString.toRegex()
        val list = invalidVersionNumberFieldRegex.findAll(rawVersionNumber)
        val indexList = mutableListOf<Int>()
        list.forEach {
            indexList.addAll(it.range.first..it.range.last)
        }
        return indexList
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


    fun isIgnored(versionNumber: String): Boolean = versionNumber == appEntity.ignoreVersionNumber


    fun switchIgnoreStatus(versionNumber: String) {
        if (isIgnored(versionNumber))
            unignore(versionNumber)
        else ignore(versionNumber)
    }

    /* 忽略这个版本 */
    private fun ignore(versionNumber: String) {
        appEntity.ignoreVersionNumber = versionNumber
        runBlocking { metaDatabase.appDao().update(appEntity) }
    }

    /* 取消忽略这个版本 */
    private fun unignore(versionNumber: String) {
        if (appEntity.ignoreVersionNumber != versionNumber) return
        appEntity.ignoreVersionNumber = null
        runBlocking { metaDatabase.appDao().update(appEntity) }
    }
}