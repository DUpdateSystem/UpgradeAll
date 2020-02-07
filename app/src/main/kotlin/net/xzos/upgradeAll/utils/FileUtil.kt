package net.xzos.upgradeAll.utils

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import net.xzos.upgradeAll.application.MyApplication
import net.xzos.upgradeAll.application.MyApplication.Companion.context
import net.xzos.upgradeAll.data.json.nongson.ObjectTag
import net.xzos.upgradeAll.server.log.LogUtil
import java.io.*
import java.util.*

object FileUtil {

    private val Log = LogUtil
    private const val TAG = "FileUtil"
    private val logObjectTag = ObjectTag("Core", TAG)

    internal val UI_CONFIG_FILE = File(context.filesDir, "ui.json")
    private val IMAGE_DIR = File(context.filesDir, "images")
    internal val UPDATE_TAB_IMAGE_NAME = "update_tab.png"
    internal val USER_STAR_TAB_IMAGE_NAME = "user_star_tab.png"
    internal val ALL_APP_TAB_IMAGE_NAME = "all_app_tab.png"
    internal val GROUP_IMAGE_DIR = File(IMAGE_DIR, "groups")
    internal val NAV_IMAGE_FILE = File(IMAGE_DIR, "nav_image.png")
    internal val IMAGE_CACHE_FILE = File(context.externalCacheDir, "_cache_image.png")

    fun clearCache(filePath: String) = File(context.externalCacheDir, filePath).deleteRecursively()

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

    fun requestPermission(activity: Activity, PERMISSIONS_REQUEST_READ_CONTACTS: Int): Boolean {
        var havePermission = false
        if (ContextCompat.checkSelfPermission(activity,
                        Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                            Manifest.permission.READ_EXTERNAL_STORAGE)) {
                Toast.makeText(context, "请给予本软件 读写存储空间权限", Toast.LENGTH_LONG).show()
            }
            ActivityCompat.requestPermissions(activity,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    PERMISSIONS_REQUEST_READ_CONTACTS)
        } else
            havePermission = true
        return havePermission
    }

    fun getUserGroupIcon(iconFileName: String?): File? =
            if (iconFileName != null) File(GROUP_IMAGE_DIR, iconFileName) else null

    fun getNewRandomNameFile(targetDir: File): File {
        if (!targetDir.exists())
            targetDir.mkdirs()
        val randomName = UUID.randomUUID().toString()
        return File(targetDir, randomName).also {
            it.createNewFile()
        }
    }

    fun performFileSearch(activity: Activity, READ_REQUEST_CODE: Int, mimeType: String) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = mimeType
        activity.startActivityForResult(intent, READ_REQUEST_CODE)
    }

    fun createFile(activity: Activity, WRITE_REQUEST_CODE: Int, mimeType: String, fileName: String) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)

        intent.addCategory(Intent.CATEGORY_OPENABLE)

        intent.type = mimeType
        intent.putExtra(Intent.EXTRA_TITLE, fileName)
        activity.startActivityForResult(intent, WRITE_REQUEST_CODE)
    }

    fun uriToPath(uri: Uri): String {
        var path = uri.path
        Log.d(logObjectTag, TAG, String.format(" uriToPath: uri: %s, path: %s", uri, path))
        return if (path != null && path.contains(":")) {
            path = path.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
            path
        } else ""
    }

    fun fileIsExistsByPath(path: String): Boolean {
        val file = File(path)
        return file.exists()
    }

    fun fileIsExistsByUri(uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)
            true
        } catch (e: FileNotFoundException) {
            false
        }
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

    fun readTextFromUri(uri: Uri): String? {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val text = inputStream.bufferedReader().use(BufferedReader::readText)
                inputStream.close()
                return text
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    fun writeTextFromUri(uri: Uri, text: String): Boolean {
        var writeSuccess = false
        try {
            val outputStream = context.contentResolver.openOutputStream(uri)
            if (outputStream != null) {
                val writer = BufferedWriter(OutputStreamWriter(
                        outputStream))
                writer.write(text)
                writer.close()
                outputStream.close()
                writeSuccess = true
            }
        } catch (e: IOException) {
            Log.d(logObjectTag, TAG, "writeTextFromUri: " + uri.path!!)
            Log.e(logObjectTag, TAG, "writeTextFromUri: 写入文件异常: ERROR_MESSAGE: $e")
        }

        return writeSuccess
    }

    fun getPicFormGallery(activity: Activity, REQUEST_CODE_LOAD_IMAGE: Int) {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        activity.startActivityForResult(intent, REQUEST_CODE_LOAD_IMAGE)
    }

    fun clipStringToClipboard(s: CharSequence, context: Context = MyApplication.context, showToast: Boolean = true) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val mClipData = ClipData.newPlainText("Label", s)
        cm.setPrimaryClip(mClipData)
        if (showToast) Toast.makeText(context, "已复制到粘贴板", Toast.LENGTH_SHORT).show()
    }

    private fun convertBitmapToFile(destinationFile: File, bitmap: Bitmap) {
        //create a file to write bitmap data
        destinationFile.createNewFile()
        //Convert bitmap to byte array
        val bos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 50, bos)
        val bitmapData = bos.toByteArray()
        //write the bytes in file
        val fos = FileOutputStream(destinationFile)
        fos.write(bitmapData)
        fos.flush()
        fos.close()
    }

    fun imageUriDump(selectedImageUri: Uri, activity: Activity): Uri {
        //Later we will use this bitmap to create the File.
        val selectedBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(
                    ImageDecoder.createSource(activity.contentResolver, selectedImageUri))
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(activity.contentResolver, selectedImageUri)
        }

        /*We can access getExternalFileDir() without asking any storage permission.*/

        convertBitmapToFile(IMAGE_CACHE_FILE, selectedBitmap)
        return Uri.fromFile(IMAGE_CACHE_FILE)
    }
}