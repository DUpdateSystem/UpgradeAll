package net.xzos.upgradeall.utils

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import eu.darken.rxshell.cmd.Cmd
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.core.data.config.AppType
import net.xzos.upgradeall.core.data_manager.utils.SearchUtils
import net.xzos.upgradeall.core.data_manager.utils.StringMatchUtils

class SearchUtils {

    private var searchUtils: SearchUtils = SearchUtils(initSearch())

    suspend fun search(searchString: String): List<SearchUtils.SearchInfo> {
        return searchUtils.search(searchString)
    }

    private fun initSearch(): List<SearchUtils.SearchInfo> {
        return listOf<SearchUtils.SearchInfo>()
                .plus(AppPackageMatchUtils.matchInfoList)
                .plus(MagiskModuleMatchUtils.matchInfoList)
    }
}

private object AppPackageMatchUtils {
    private val packages: List<ApplicationInfo>
        get() = MyApplication.context.packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

    internal val matchInfoList: List<SearchUtils.SearchInfo>
        get() {
            return packages.map {
                val appName = MyApplication.context.packageManager.getApplicationLabel(it)

                SearchUtils.SearchInfo(
                        AppType.androidApp,
                        StringMatchUtils.MatchInfo(it.packageName, appName.toString(), listOf(
                                StringMatchUtils.MatchString(appName),
                                StringMatchUtils.MatchString(it.packageName)
                        ))
                )
            }
        }
}

private object MagiskModuleMatchUtils {
    private const val defaultModuleFolderPath = "/data/adb/modules/"
    private val shellMatchUtils = ShellMatchUtils(useSU = true)
    private val moduleInfoList: List<ModuleInfo>
        get() {
            val list = mutableListOf<ModuleInfo>()
            for (moduleFolderName in shellMatchUtils.getFileNameList(defaultModuleFolderPath)) {
                getModuleInfo(moduleFolderName)?.run {
                    list.add(this)
                }
            }
            return list
        }

    internal val matchInfoList: List<SearchUtils.SearchInfo>
        get() {
            return moduleInfoList.map {
                val searchInfo = SearchUtils.SearchInfo(
                        AppType.androidMagiskModule,
                        StringMatchUtils.MatchInfo(it.moduleFolderName, it.name, listOf(
                                StringMatchUtils.MatchString(it.id),
                                StringMatchUtils.MatchString(it.name),
                                StringMatchUtils.MatchString(it.author),
                                StringMatchUtils.MatchString(it.description)
                        ))
                )
                searchInfo
            }
        }

    private fun getModuleInfo(moduleFolderName: String): ModuleInfo? {
        val fileString =
                shellMatchUtils.catFile(
                        "/data/adb/modules/$moduleFolderName/module.prop"
                ) ?: return null
        val prop = MiscellaneousUtils.parsePropertiesString(fileString)
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

