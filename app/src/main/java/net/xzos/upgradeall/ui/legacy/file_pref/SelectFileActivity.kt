package net.xzos.upgradeall.ui.legacy.file_pref

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResult
import net.xzos.upgradeall.core.log.ObjectTag

class SelectFileActivity : FilePrefActivity() {

    override fun onActivityResultOkCallback(resultData: ActivityResult) {
        contentResolver.openInputStream(resultData.data!!.data!!)?.let { iStream ->
            bytes = iStream.readBytes()
        }
    }

    override fun buildIntent(): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mimeType
        }
    }

    companion object {
        private const val TAG = "SelectFileActivity"
        private val logObjectTag = ObjectTag("UI", TAG)

        private const val READ_REQUEST_CODE = 2
        private var bytes: ByteArray? = null
        private var mimeType: String? = null

        suspend fun newInstance(context: Context, mimeType: String): ByteArray? {
            bytes = null
            this.mimeType = mimeType
            startActivity(context, SelectFileActivity::class.java)
            return bytes
        }
    }
}
