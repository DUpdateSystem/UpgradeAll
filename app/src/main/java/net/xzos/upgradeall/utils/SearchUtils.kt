package net.xzos.upgradeall.utils

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import eu.darken.rxshell.cmd.Cmd
import net.xzos.upgradeall.application.MyApplication.Companion.context
import net.xzos.upgradeall.core.data.config.AppType
import net.xzos.upgradeall.core.data_manager.utils.MatchInfo
import net.xzos.upgradeall.core.data_manager.utils.MatchString
import net.xzos.upgradeall.core.data_manager.utils.SearchInfo
import net.xzos.upgradeall.core.data_manager.utils.SearchUtils

class SearchUtils {

    private var searchInfoList: List<SearchInfo> = listOf()
    private var searchUtils: SearchUtils = SearchUtils(listOf())

    fun renewData() {
        val list = initSearchList()
        if (searchInfoList != list) {
            searchInfoList = list
            searchUtils = SearchUtils(searchInfoList)
        }
    }

    suspend fun search(searchString: String): List<SearchInfo> {
        return searchUtils.search(searchString)
    }

    private fun initSearchList(): List<SearchInfo> {
        return listOf<SearchInfo>()
                .plus(AppPackageMatchUtils().matchInfoList)
                .plus(MagiskModuleMatchUtils().matchInfoList)
    }
}

private class AppPackageMatchUtils {
    private val packageManager = context.packageManager
    private val packages: List<ApplicationInfo> = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

    val matchInfoList = packages.map {
        val appName = packageManager.getApplicationLabel(it)
        SearchInfo(
                AppType.androidApp,
                MatchInfo(it.packageName, appName.toString(), listOf(
                        MatchString(appName),
                        MatchString(it.packageName)
                ))
        )
    }
}

private class MagiskModuleMatchUtils {
    private val shellMatchUtils = ShellMatchUtils(useSU = true)
    val matchInfoList = mutableListOf<SearchInfo>().apply {
        for (moduleFolderName in shellMatchUtils.getFileNameList(moduleFolderPath)) {
            getSearchInfo(moduleFolderName)?.let {
                add(it)
            }
        }
    }

    private fun getSearchInfo(moduleFolderName: String): SearchInfo? {
        val fileString =
                shellMatchUtils.catFile(
                        "/data/adb/modules/$moduleFolderName/module.prop"
                ) ?: return null
        val prop = MiscellaneousUtils.parsePropertiesString(fileString)
        return SearchInfo(
                AppType.androidMagiskModule,
                MatchInfo(moduleFolderName,
                        prop.getProperty("name", ""),
                        listOf(
                                MatchString(prop.getProperty("id", "")),
                                MatchString(prop.getProperty("name", "")),
                                MatchString(prop.getProperty("author", "")),
                                MatchString(prop.getProperty("description", "")
                                )
                        )
                )
        )
    }

    companion object {
        private const val moduleFolderPath = "/data/adb/modules/"
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

        val result = runCommand(command) ?: return emptyList()
        return result.getOutputString()
                .split("\n".toRegex())
                .dropLastWhile { it.isEmpty() }
                .map { it.removePrefix(folderPathString) }
    }

    fun catFile(filePath: String): String? {
        val command = "cat $filePath"
        return runCommand(command)?.getOutputString()
    }

    private fun runCommand(commandString: String): Cmd.Result? {
        return if (useSU)
            Shell.runSuShellCommand(commandString)
        else
            Shell.runShellCommand(commandString)
    }
}
