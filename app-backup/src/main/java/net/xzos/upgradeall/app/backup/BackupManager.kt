package net.xzos.upgradeall.app.backup

import android.os.Build
import net.xzos.upgradeall.app.backup.utils.dbFile
import net.xzos.upgradeall.app.backup.utils.prefsFile
import net.xzos.upgradeall.core.utils.file.ZipFile
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

object BackupManager {

    fun newFileName(): String {
        val dataFormat = "yyyy-MM-dd_HH-mm"
        val timeString = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val current = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern(dataFormat)
            current.format(formatter)
        } else {
            val formatter = SimpleDateFormat(dataFormat, Locale.getDefault())
            formatter.format(Date())
        }
        return "UpgradeAll_$timeString.zip"
    }

    suspend fun mkZipFileBytes(): ByteArray? {
        return try {
            val zipFile = ZipFile()
            // backup database
            zipFile.zipFile(dbFile)
            // backup
            zipFile.zipFile(prefsFile)

            zipFile.getByteArray()
        } catch (ioe: IOException) {
            ioe.printStackTrace()
            null
        }
    }
}
