package net.xzos.upgradeall.ui.activity.file_pref

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.log.ObjectTag
import net.xzos.upgradeall.utils.ToastUtil
import net.xzos.upgradeall.utils.file.FileUtil

class SaveFileActivity : FilePrefActivity() {

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (resultCode == Activity.RESULT_OK && resultData != null) {
            val uri = resultData.data
            if (uri != null) {
                val textResId =
                        if (FileUtil.writeToUri(uri, byteArray = BYTE_ARRAY))
                            R.string.save_file_successfully
                        else
                            R.string.save_file_failed
                ToastUtil.makeText(textResId, Toast.LENGTH_LONG)
            }
        }
        finish()
    }

    override fun selectFile() {
        FileUtil.createFile(this, WRITE_REQUEST_CODE, MIME_TYPE, FILE_NAME)
    }

    companion object {
        private const val TAG = "SaveFileActivity"
        private val logObjectTag = ObjectTag("UI", TAG)

        private const val WRITE_REQUEST_CODE = 2

        private var isSuccess = false

        private var FILE_NAME: String = ""
        private var MIME_TYPE: String? = null
        private var BYTE_ARRAY: ByteArray? = null

        suspend fun newInstance(fileName: String, mimeType: String?, byteArray: ByteArray, context: Context): Boolean {
            isSuccess = false
            FILE_NAME = fileName
            MIME_TYPE = mimeType
            BYTE_ARRAY = byteArray
            startActivity(context, SaveFileActivity::class.java)
            return isSuccess
        }
    }
}
