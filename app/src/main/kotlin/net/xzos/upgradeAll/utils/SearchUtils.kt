package net.xzos.upgradeAll.utils

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.jaredrummler.android.shell.CommandResult
import com.jaredrummler.android.shell.Shell
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeAll.application.MyApplication
import org.apache.commons.text.similarity.FuzzyScore
import java.util.*

object SearchUtils {
    private val mutex = Mutex()

    private const val searchResultCacheListSize = 8
    private val searchResultCacheList = mutableListOf<Pair<String?, List<SearchInfo>>>()

    suspend fun searchTargetByAllApi(searchString: String?): List<SearchInfo> {
        return mutex.withLock {
            if (searchString.isNullOrBlank())
                listOf()
            else
                getCacheResult(searchString)
                        ?: iterateSearch(searchString)
        }
    }

    private fun initSearch(searchString: String): List<SearchInfo> {
        return listOf<SearchInfo>()
                .plus(AppPackageMatchUtils.search(searchString)
                        .map { SearchInfo("App_Package", it) }
                )
                .plus(MagiskModuleMatchUtils.search(searchString)
                        .map { SearchInfo("Magisk_Module", it) }
                ).sortedByDescending { it.matchInfo.getMaxPoint() }
                .also { cacheResult(searchString, it) }
    }

    private fun iterateSearch(searchString: String): List<SearchInfo> {
        return getCacheResult(searchString.dropLast(1))?.toTypedArray()?.copyOf()?.toMutableList()
                ?.apply {
                    this.map { searchInfo ->
                        val matchList = searchInfo.matchInfo.matchList
                        searchInfo.matchInfo.matchList = StringMatchUtils.match(
                                searchString,
                                matchList.map { it.matchString }
                        )
                    }
                }?.filter { searchInfo ->
                    searchInfo.matchInfo.matchList.isNotEmpty()
                }?.also {
                    cacheResult(searchString, it)
                } ?: initSearch(searchString)
    }

    private fun getCacheResult(searchString: String?): List<SearchInfo>? {
        if (searchResultCacheList.map { it.first }.contains(searchString)) {
            for (searchResult in searchResultCacheList) {
                if (searchResult.first == searchString) {
                    searchResultCacheList.remove(searchResult)
                    searchResultCacheList.add(0, searchResult)
                    return searchResult.second
                }
            }
        }
        return null
    }

    private fun cacheResult(searchString: String?, searchInfoList: List<SearchInfo>) {
        if (!searchString.isNullOrBlank() && searchInfoList.isNotEmpty())
            searchResultCacheList.add(0, Pair(searchString, searchInfoList))
        if (searchResultCacheList.size > searchResultCacheListSize)
            searchResultCacheList.dropLast(searchResultCacheList.size - searchResultCacheListSize)
    }

    fun clearResultCache() {
        searchResultCacheList.clear()
    }

    data class SearchInfo(
            val targetApi: CharSequence,
            val matchInfo: StringMatchUtils.MatchInfo) {
        override fun toString(): String {
            return matchInfo.targetId
        }
    }
}

object StringMatchUtils {

    fun match(searchString: CharSequence?, matchStringList: List<CharSequence>): List<MatchString> {
        val matchList = mutableListOf<MatchString>()
        if (searchString != null) {
            val fuzzyScore = FuzzyScore(MiscellaneousUtils.getCurrentLocale(MyApplication.context)
                    ?: Locale.getDefault())
            for (matchString in matchStringList) {
                val matchPoints = fuzzyScore.fuzzyScore(matchString, searchString)
                if (matchPoints > 0) {
                    matchList.add(MatchString(matchString, matchPoints))
                }
            }
        }
        return matchList
    }

    data class MatchString(val matchString: CharSequence, val matchPoint: Int)

    data class MatchInfo(val groupName: CharSequence, val targetId: String, var matchList: List<MatchString>) {
        fun getMaxPoint(): Int {
            var maxPoint = 0
            for (matchString in matchList) {
                if (matchString.matchPoint > maxPoint) {
                    maxPoint = matchString.matchPoint
                }
            }
            return maxPoint
        }
    }
}

class ShellMatchUtils(private val useSU: Boolean = false) {

    fun getFileNameList(folderPath: String): List<String> {
        val folderPathString = folderPath.apply {
            val separator = '/'
            if (this.last() != separator)
                this.plus(separator)
        }
        val command = """ for entry in "${'$'}search_dir"${folderPathString}*
            do
              echo "${'$'}entry"
            done """.trimIndent()

        val result = runCommand(command)
        return result.getStdout()
                .split("\n".toRegex())
                .dropLastWhile { it.isEmpty() }
                .map { it.removePrefix(folderPathString) }
    }

    fun catFile(filePath: String): String {
        val command = "cat $filePath"
        return runCommand(command).getStdout()
    }

    private fun runCommand(commandString: String): CommandResult {
        return if (useSU)
            Shell.SU.run(commandString)
        else
            Shell.run(commandString)
    }
}

object AppPackageMatchUtils {
    private val packages: List<ApplicationInfo>
        get() = MyApplication.context.packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

    fun search(searchString: String): List<StringMatchUtils.MatchInfo> {
        val matchAppInfoList = mutableListOf<StringMatchUtils.MatchInfo>()
        for (packageInfo in packages) {
            val appName = MyApplication.context.packageManager.getApplicationLabel(packageInfo)
            val matchList = StringMatchUtils.match(
                    searchString,
                    listOf(appName, packageInfo.packageName)
            )
            if (matchList.isNotEmpty())
                matchAppInfoList.add(
                        StringMatchUtils.MatchInfo(appName, packageInfo.packageName, matchList)
                )
        }
        return matchAppInfoList
    }

}

object MagiskModuleMatchUtils {
    private const val defaultModuleFolderPath = "/data/adb/modules/"
    private val shellMatchUtils = ShellMatchUtils(useSU = true)
    private val moduleInfoList: List<ModuleInfo>
        get() {
            val list = mutableListOf<ModuleInfo>()
            for (moduleFolderName in shellMatchUtils.getFileNameList(defaultModuleFolderPath)) {
                list.add(getModuleInfo(moduleFolderName))
            }
            return list
        }

    fun search(searchString: String): List<StringMatchUtils.MatchInfo> {
        val matchAppInfoList = mutableListOf<StringMatchUtils.MatchInfo>()
        for (moduleInfo in moduleInfoList) {
            val matchList = StringMatchUtils.match(
                    searchString,
                    listOf(
                            moduleInfo.id,
                            moduleInfo.name,
                            moduleInfo.author,
                            moduleInfo.description
                    )
            )
            if (matchList.isNotEmpty())
                matchAppInfoList.add(
                        StringMatchUtils.MatchInfo(moduleInfo.name, moduleInfo.moduleFolderName, matchList)
                )
        }
        return matchAppInfoList
    }

    private fun getModuleInfo(moduleFolderName: String): ModuleInfo {
        val prop = MiscellaneousUtils.parsePropertiesString(
                shellMatchUtils.catFile(
                        "/data/adb/modules/$moduleFolderName/module.prop"
                ))
        return ModuleInfo(
                moduleFolderName,
                prop.getProperty("id", ""),
                prop.getProperty("name", ""),
                prop.getProperty("author", ""),
                prop.getProperty("description", "")
        )
    }

    data class ModuleInfo(
            val moduleFolderName: String,
            val id: String,
            val name: String,
            val author: String,
            val description: String)
}