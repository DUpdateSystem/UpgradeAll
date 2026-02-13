package net.xzos.upgradeall.ui.utils.file_pref

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.result.ActivityResult
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.androidutils.ToastUtil
import net.xzos.upgradeall.core.androidutils.writeToUri
import net.xzos.upgradeall.core.utils.log.ObjectTag

class SaveFileActivity : FilePrefActivity() {

    override fun onActivityResultOkCallback(resultData: ActivityResult) {
        val data = resultData.data
        if (data != null) {
            val uri = data.data
            if (uri != null) {
                val textResId =
                    if (writeToUri(uri, this, byteArray = bytes))
                        R.string.save_file_successfully
                    else
                        R.string.save_file_failed
                ToastUtil.showText(this, textResId, Toast.LENGTH_LONG)
            }
        }
    }

    override fun buildIntent(): Intent {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)

        intent.addCategory(Intent.CATEGORY_OPENABLE)

        if (mimeType != null)
            intent.type = mimeType
        intent.putExtra(Intent.EXTRA_TITLE, fileName)
        return intent
    }

    companion object {
        private const val TAG = "SaveFileActivity"
        private val logObjectTag = ObjectTag("UI", TAG)

        private var isSuccess = false

        private var fileName: String? = null
        private var mimeType: String? = null
        private var bytes: ByteArray? = null

        suspend fun newInstance(
            fileName: String,
            mimeType: String?,
            byteArray: ByteArray,
            context: Context
        ): Boolean {
            isSuccess = false
            this.fileName = fileName
            this.mimeType = mimeType
            bytes = byteArray
            startActivity(context, SaveFileActivity::class.java)
            return isSuccess
        }
    }
}
