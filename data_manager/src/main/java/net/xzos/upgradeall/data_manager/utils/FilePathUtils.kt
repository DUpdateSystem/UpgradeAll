package net.xzos.upgradeall.data_manager.utils

import net.xzos.upgradeall.data.json.nongson.ObjectTag
import net.xzos.upgradeall.log.Log
import java.io.File


object FilePathUtils {

    private const val TAG = "FilePathTranslation "
    private val logObjectTag = ObjectTag("Core", TAG)

    fun renameSameFile(targetFile: File, fileList: List<File>): File {
        val separator = "."
        val sameFileNameIndexList = mutableListOf<String>()
        for (file in fileList) {
            if (targetFile.parent == file.parent && file.name.contains("^\\d+.${targetFile.name}\$")) {
                sameFileNameIndexList.add(file.name.substringBefore(separator))
            }
        }
        var i = 0
        while (i.toString() in sameFileNameIndexList) {
            i++
        }
        val fileName =
                if (i == 0)
                    targetFile.name
                else
                    i.toString() + separator + targetFile.name
        return File(targetFile.parentFile, fileName)
    }

    /**
     * 使用一个绝对地址和一个相对地址合成一个绝对地址，
     * 例如：/root/Download/hub 与 ../1.txt 合成 /root/Download/1.txt
     * 兼容本地文件地址和网址与于此类似的其他地址
     */
    fun pathTransformRelativeToAbsolute(absolutePath: String, relativePath: String): String {
        @Suppress("NAME_SHADOWING") var absolutePath = absolutePath
        Log.e(logObjectTag, TAG, String.format("pathTransformRelativeToAbsolute: absolutePath: %s, relativePath: %s", absolutePath, relativePath))
        if (absolutePath != "/") {
            if (absolutePath.endsWith("/"))
                absolutePath = absolutePath.substring(0, absolutePath.length - 1)  // 去除末尾的 /
        }
        // 判断是否为相对地址
        if (relativePath.indexOf(".") == 0) {
            val relativePathList = relativePath.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            var basePathBuilder = StringBuilder(absolutePath)
            for (item in relativePathList) {
                when (item) {
                    ".." -> {
                        val index = basePathBuilder.lastIndexOf("/")
                        basePathBuilder = StringBuilder(basePathBuilder.substring(0, index))
                    }
                    "." -> {
                    }
                    else -> basePathBuilder.append("/").append(item)
                }
            }
            absolutePath = basePathBuilder.toString()
        } else if (!relativePath.startsWith("/"))
            absolutePath += relativePath
        else
            absolutePath = relativePath
        return absolutePath
    }

    /**
     * 使用两个绝对地址合成一个相对地址，
     * 例如：/root/Download/hub (absolutePathRoot) 与 /root/Download/1.txt(absolutePathTarget) 合成 ../1.txt
     * 兼容本地文件地址和网址与于此类似的其他地址
     */
    fun pathTransformAbsoluteToRelative(absolutePathRoot: String, absolutePathTarget: String): String {
        @Suppress("NAME_SHADOWING")
        var absolutePathRoot = absolutePathRoot
        @Suppress("NAME_SHADOWING")
        var absolutePathTarget = absolutePathTarget
        // 根节点输出
        if (absolutePathRoot == "/") return absolutePathTarget
        // 去除末尾的 /
        if (absolutePathRoot.endsWith("/"))
            absolutePathRoot = absolutePathRoot.substring(0, absolutePathRoot.length - 1)
        if (absolutePathTarget != "/" && absolutePathTarget.endsWith("/"))
            absolutePathTarget = absolutePathTarget.substring(0, absolutePathTarget.length - 1)

        val rootPathList = absolutePathRoot.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val targetPathList = absolutePathTarget.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        var splitIndex = 0
        for (i in 0 until if (rootPathList.size < targetPathList.size) rootPathList.size else targetPathList.size) {
            if (rootPathList[i] == targetPathList[i])
                splitIndex = i
            else
                break
        }
        splitIndex += 1 // 换算第一个不同目录的索引
        var relativePath = StringBuilder()
        for (i in splitIndex until rootPathList.size - 1) {
            relativePath.append("../")
        }
        for (i in splitIndex until targetPathList.size) {
            relativePath.append(targetPathList[i]).append("/")
        }
        relativePath = StringBuilder(relativePath.substring(0, relativePath.length - 1)) // 去除结尾多余 /
        if (absolutePathRoot.startsWith("/") || absolutePathTarget.startsWith("/"))
            relativePath.insert(0, "./")
        return relativePath.toString()
    }
}
