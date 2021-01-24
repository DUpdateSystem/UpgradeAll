package net.xzos.upgradeall.utils.file

import android.Manifest
import android.app.Activity
import android.content.*
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract.EXTRA_INITIAL_URI
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import net.xzos.upgradeall.BuildConfig
import net.xzos.upgradeall.R
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.application.MyApplication.Companion.context
import net.xzos.upgradeall.core.log.Log
import net.xzos.upgradeall.core.log.ObjectTag
import net.xzos.upgradeall.data.PreferencesMap
import net.xzos.upgradeall.utils.MiscellaneousUtils
import net.xzos.upgradeall.utils.ToastUtil
import net.xzos.upgradeall.utils.getSystemLocale
import java.io.*
import java.util.*


object FileUtil {

    private const val TAG = "FileUtil"
    private val logObjectTag = ObjectTag("Core", TAG)
    private val context = MyApplication.context

    val PREFERENCES_FILE by lazy { File(context.filesDir.parentFile, "shared_prefs/${context.packageName}_preferences.xml") }
    internal val UI_CONFIG_FILE by lazy { File(context.filesDir, "ui.json") }
    internal val IMAGE_DIR by lazy { File(context.filesDir, "images").getExistsFile(true) }
    internal const val UPDATE_TAB_IMAGE_NAME = "update_tab.png"
    internal const val USER_STAR_TAB_IMAGE_NAME = "user_star_tab.png"
    internal const val ALL_APP_TAB_IMAGE_NAME = "all_app_tab.png"
    internal val GROUP_IMAGE_DIR by lazy { File(IMAGE_DIR, "groups").getExistsFile(true) }
    internal val NAV_IMAGE_FILE by lazy { File(IMAGE_DIR, "nav_image.png").getExistsFile() }
    private val CACHE_DIR = context.externalCacheDir!!
    internal val IMAGE_CACHE_FILE by lazy { File(CACHE_DIR, "_cache_image.png").getExistsFile() }
    internal val DOWNLOAD_CACHE_DIR by lazy { File(CACHE_DIR, "Download").getExistsFile(true) }
    internal val SHELL_SCRIPT_CACHE_FILE by lazy { File(CACHE_DIR, "run.sh").getExistsFile() }
    internal val DOWNLOAD_DOCUMENT_FILE: DocumentFile?
        get() = if (PreferencesMap.auto_dump_download_file)
            getDocumentFile(context, Uri.parse(PreferencesMap.user_download_path))
        else null


    init {
        // clear cache
        CACHE_DIR.deleteRecursively()
    }

    fun requestFilePermission(activity: Activity, PERMISSIONS_REQUEST_READ_CONTACTS: Int): Boolean {
        return MiscellaneousUtils.requestPermission(
                activity, Manifest.permission.READ_EXTERNAL_STORAGE,
                PERMISSIONS_REQUEST_READ_CONTACTS, R.string.please_grant_storage_perm)
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

    /**
     * TreeUri 转换为 DocumentFile
     * 便于文件目录操作
     */
    private fun getDocumentFile(context: Context, treeUri: Uri): DocumentFile? {
        try {
            takePersistableUriPermission(context, treeUri)
        } catch (e: SecurityException) {
            return null
        }
        return DocumentFile.fromTreeUri(context, treeUri)
    }

    /**
     * 申请文件树读写权限
     */
    fun takePersistableUriPermission(context: Context, treeUri: Uri) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        val takeFlags: Int = intent.flags and
                (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        // Check for the freshest data.
        context.contentResolver.takePersistableUriPermission(treeUri, takeFlags)
    }

    /**
     * 转储文件内容到指定 TreeUri 下
     */
    fun dumpFile(file: File, treeFile: DocumentFile): DocumentFile? {
        val newFile = treeFile.createFile(file.getMimeType(), file.name) ?: return null
        writeToUri(newFile.uri, byteArray = file.readBytes())
        return newFile
    }

    /**
     * 通过文件存储框架新建并获取文件
     */
    fun createFile(activity: Activity, WRITE_REQUEST_CODE: Int, mimeType: String?, fileName: String) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)

        intent.addCategory(Intent.CATEGORY_OPENABLE)

        if (mimeType != null)
            intent.type = mimeType
        intent.putExtra(Intent.EXTRA_TITLE, fileName)
        try {
            activity.startActivityForResult(intent, WRITE_REQUEST_CODE)
        } catch (e: ActivityNotFoundException) {
            MiscellaneousUtils.showToast(R.string.function_unsupported_error, duration = Toast.LENGTH_LONG)
        }
    }

    /**
     * 通过文件存储框架获取文件夹
     */
    fun getFolder(activity: Activity, OPEN_REQUEST_CODE: Int, initialPath: String?) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        if (initialPath != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val file = DocumentFile.fromFile(File(initialPath))
            intent.putExtra(EXTRA_INITIAL_URI, file.uri)
        }
        try {
            activity.startActivityForResult(intent, OPEN_REQUEST_CODE)
        } catch (e: ActivityNotFoundException) {
            MiscellaneousUtils.showToast(R.string.function_unsupported_error, duration = Toast.LENGTH_LONG)
        }
    }

    /**
     * 通过 URI 获取文件类型 MimeType 信息
     */
    fun getMimeTypeByUri(context: Context, uri: Uri): String {
        return if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            val cr: ContentResolver = context.contentResolver
            cr.getType(uri)
        } else {
            val fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri
                    .toString())
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    fileExtension.toLowerCase(getSystemLocale(context.resources.configuration)))
        } ?: "*/*"
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

    fun writeToUri(uri: Uri, text: String? = null, byteArray: ByteArray? = null): Boolean {
        var writeSuccess = false
        if (text != null || byteArray != null)
            try {
                val outputStream = context.contentResolver.openOutputStream(uri)
                if (outputStream != null) {
                    if (text != null) {
                        val writer = BufferedWriter(OutputStreamWriter(
                                outputStream))
                        writer.write(text)
                        writer.close()
                    }
                    if (byteArray != null) {
                        outputStream.write(byteArray)
                    }
                    outputStream.close()
                    writeSuccess = true
                }
            } catch (e: IOException) {
                Log.e(logObjectTag, TAG, """
                writeTextFromUri: 写入文件异常: 
                ERROR_MESSAGE: $e
                URI_PATH: ${uri.path}
            """.trimIndent())
            } catch (e: SecurityException) {
                Log.e(logObjectTag, TAG, """
                writeTextFromUri: 写入文件异常（数据读写安全故障）: 
                ERROR_MESSAGE: $e
                URI_PATH: ${uri.path}
            """.trimIndent())
            }

        return writeSuccess
    }

    fun getPicFormGallery(activity: Activity, REQUEST_CODE_LOAD_IMAGE: Int) {
        val intent = Intent().apply {
            type = "image/*"
            action = Intent.ACTION_GET_CONTENT
        }
        activity.startActivityForResult(intent, REQUEST_CODE_LOAD_IMAGE)
    }

    fun clipStringToClipboard(s: CharSequence, context1: Context = context, showToast: Boolean = true) {
        val cm = context1.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val mClipData = ClipData.newPlainText("Label", s)
        cm.setPrimaryClip(mClipData)
        if (showToast) ToastUtil.makeText(R.string.copied_to_pasteboard, Toast.LENGTH_SHORT)
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

fun File.getFileByAutoRename(): File {
    var index = 0
    val name = this.nameWithoutExtension
    val extension = this.extension
    val parent = this.parentFile
    var file = this
    while (file.exists()) {
        file = File(parent, name + index + extension)
        index += 1
    }
    return file
}

fun File.getMimeType(): String {
    val mime = MimeTypeMap.getSingleton()
    val cR = context.contentResolver
    return mime.getExtensionFromMimeType(cR.getType(Uri.fromFile(this))) ?: "*/*"
}

fun File.getExistsFile(isDir: Boolean = false): File {
    @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    if (isDir) mkdirs()
    else parentFile.mkdirs()
    return this
}

@Throws(IllegalArgumentException::class)
fun File.getProviderUri(): Uri {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
        try {
            FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileprovider", this)
        } catch (e: IllegalArgumentException) {
            throw e
        }
    else Uri.fromFile(this)
}

