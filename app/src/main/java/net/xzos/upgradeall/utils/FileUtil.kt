package net.xzos.upgradeall.utils

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
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.application.MyApplication.Companion.context
import net.xzos.upgradeall.data.json.nongson.ObjectTag
import net.xzos.upgradeall.log.Log
import java.io.*
import java.util.*

object FileUtil {

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