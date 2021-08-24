package net.xzos.upgradeall.core.androidutils

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract.EXTRA_INITIAL_URI
import android.webkit.MimeTypeMap
import androidx.annotation.StringRes
import androidx.documentfile.provider.DocumentFile
import net.xzos.upgradeall.core.shell.getFileText
import net.xzos.upgradeall.core.utils.log.Log
import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.core.utils.log.msg
import java.io.*
import java.util.*


private const val TAG = "FileUtil"
private val logObjectTag = ObjectTag("Core", TAG)

/**
 * 获取指定文件的 Prop 格式数据
 */
fun getProp(path: String): Properties? {
    return getFileText(path)?.parseProperties()
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
fun dumpFile(file: File, treeFile: DocumentFile, context: Context): DocumentFile? {
    val newFile = treeFile.createFile(file.getMimeType(context), file.name) ?: return null
    writeToUri(newFile.uri, context, byteArray = file.readBytes())
    return newFile
}

/**
 * 通过文件存储框架新建并获取文件
 */
fun createFile(
    activity: Activity,
    WRITE_REQUEST_CODE: Int,
    mimeType: String?,
    fileName: String
) {
    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)

    intent.addCategory(Intent.CATEGORY_OPENABLE)

    if (mimeType != null)
        intent.type = mimeType
    intent.putExtra(Intent.EXTRA_TITLE, fileName)
    try {
        activity.startActivityForResult(intent, WRITE_REQUEST_CODE)
    } catch (e: ActivityNotFoundException) {
        Log.e(logObjectTag, TAG, "你的手机暂不支持该功能. error: ${e.msg()}")
        throw e
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
        Log.e(logObjectTag, TAG, "你的手机暂不支持该功能. error: ${e.msg()}")
        throw e
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
        val fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(
            fileExtension.lowercase(locale)
        )
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

fun fileIsExistsByUri(uri: Uri, context: Context): Boolean {
    return try {
        context.contentResolver.openInputStream(uri)
        true
    } catch (e: FileNotFoundException) {
        false
    }
}

fun readTextFromUri(uri: Uri, context: Context): String? {
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

fun writeToUri(
    uri: Uri,
    context: Context,
    text: String? = null,
    byteArray: ByteArray? = null
): Boolean {
    var writeSuccess = false
    if (text != null || byteArray != null)
        try {
            val outputStream = context.contentResolver.openOutputStream(uri)
            if (outputStream != null) {
                if (text != null) {
                    val writer = BufferedWriter(
                        OutputStreamWriter(
                            outputStream
                        )
                    )
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
            Log.e(
                logObjectTag, TAG, """
                writeTextFromUri: 写入文件异常: 
                ERROR_MESSAGE: ${e.msg()}
                URI_PATH: ${uri.path}
            """.trimIndent()
            )
        } catch (e: SecurityException) {
            Log.e(
                logObjectTag, TAG, """
                writeTextFromUri: 写入文件异常（数据读写安全故障）: 
                ERROR_MESSAGE: ${e.msg()}
                URI_PATH: ${uri.path}
            """.trimIndent()
            )
        }

    return writeSuccess
}

fun requestFilePermission(
    activity: Activity,
    PERMISSIONS_REQUEST_READ_CONTACTS: Int,
    @StringRes tipResId: Int
): Boolean {
    return requestPermission(
        activity, Manifest.permission.READ_EXTERNAL_STORAGE,
        PERMISSIONS_REQUEST_READ_CONTACTS, tipResId
    )
}

fun openInFileManager(path: String, context: Context) {
    val selectedUri = Uri.parse(path)
    val intent = Intent(Intent.ACTION_VIEW)
    intent.setDataAndType(selectedUri, "resource/folder")
    context.startActivity(intent)
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

fun File.getMimeType(context: Context): String {
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

suspend fun DocumentFile.copyAllFile(dirFile: File, context: Context) {
    dirFile.mkdirs()
    for (f in listFiles()) {
        val file = File(dirFile, f.name!!)
        if (file.exists()) continue
        f.copyToFile(file, context)
    }
}

@Suppress("BlockingMethodInNonBlockingContext", "RedundantSuspendModifier")
suspend fun DocumentFile.copyToFile(file: File, context: Context) {
    if (!file.exists()) file.createNewFile()
    val inputStream = context.contentResolver.openInputStream(this.uri) ?: return
    copyInputStreamToFile(inputStream, file)
}

fun copyInputStreamToFile(inputStream: InputStream, file: File) {
    var out: OutputStream? = null
    try {
        out = FileOutputStream(file)
        val buf = ByteArray(1024)
        var len: Int
        while (inputStream.read(buf).also { len = it } > 0) {
            out.write(buf, 0, len)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        // Ensure that the InputStreams are closed even if there's an exception.
        try {
            out?.close()

            // If you want to close the "in" InputStream yourself then remove this
            // from here but ensure that you close it yourself eventually.
            inputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

val DocumentFile.extension: String
    get() = name?.substringAfterLast('.', "") ?: ""
