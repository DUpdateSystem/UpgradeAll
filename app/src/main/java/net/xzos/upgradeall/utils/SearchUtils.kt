package net.xzos.upgradeall.utils

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.jaredrummler.android.shell.CommandResult
import com.jaredrummler.android.shell.Shell
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.data_manager.utils.SearchUtils
import net.xzos.upgradeall.data_manager.utils.StringMatchUtils

class SearchUtils {

    private var searchUtils: SearchUtils? = null

    suspend fun search(searchString: String): List<SearchUtils.SearchInfo> {
        if (searchUtils == null) {
            searchUtils = SearchUtils(initSearch(searchString))
        }
        return searchUtils?.search(searchString) ?: listOf()
    }

    private fun initSearch(searchString: String): List<SearchUtils.SearchInfo> {
        return listOf<SearchUtils.SearchInfo>()
                .plus(AppPackageMatchUtils.search(searchString)
                        .map { SearchUtils.SearchInfo("App_Package", it) }
                )
                .plus(MagiskModuleMatchUtils.search(searchString)
                        .map { SearchUtils.SearchInfo("Magisk_Module", it) }
                ).sortedByDescending { it.matchInfo.getMaxPoint() }
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
                        StringMatchUtils.MatchInfo(packageInfo.packageName, appName.toString(), matchList)
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
                        StringMatchUtils.MatchInfo(moduleInfo.moduleFolderName, moduleInfo.name, matchList)
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
