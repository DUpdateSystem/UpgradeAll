package net.xzos.upgradeall.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.sync.Mutex
import net.xzos.dupdatesystem.core.data.json.nongson.ObjectTag
import net.xzos.upgradeall.R
import net.xzos.upgradeall.utils.FileUtil

class SaveFileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blank_wait)
        getFile()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            val uri = data.data
            if (uri != null) {
                val textResId =
                        if (FileUtil.writeToUri(uri, byteArray = BYTE_ARRAY))
                            R.string.save_file_successfully
                        else
                            R.string.save_file_failed
                Toast.makeText(this, textResId, Toast.LENGTH_LONG).show()
            }
        }
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 运行完成，解锁
        if (mutex.isLocked) mutex.unlock()
    }

    private fun getFile() {
        if (FileUtil.requestPermission(this, PERMISSIONS_REQUEST_WRITE_CONTACTS)) {
            FileUtil.createFile(this, WRITE_REQUEST_CODE, MIME_TYPE, FILE_NAME)
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>, grantResults: IntArray
    ) {
        if (requestCode == PERMISSIONS_REQUEST_WRITE_CONTACTS) {
            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.file_permission_request, Toast.LENGTH_LONG).show()
                finish()
            } else {
                getFile()
            }
        }
    }

    companion object {
        private const val TAG = "SaveFileActivity"
        private val logObjectTag = ObjectTag("UI", TAG)

        private const val PERMISSIONS_REQUEST_WRITE_CONTACTS = 1
        private const val WRITE_REQUEST_CODE = 2

        private val mutex = Mutex()

        private var isSuccess = false

        private var FILE_NAME: String = ""
        private var MIME_TYPE: String? = null
        private var BYTE_ARRAY: ByteArray? = null

        suspend fun newInstance(fileName: String, byteArray: ByteArray, mimeType: String?, context: Context): Boolean {
            mutex.lock()
            isSuccess = false
            FILE_NAME = fileName
            MIME_TYPE = mimeType
            BYTE_ARRAY = byteArray
            context.startActivity(
                    Intent(context, SaveFileActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
            )
            wait()
            return isSuccess
        }

        private suspend fun wait() {
            mutex.lock()
            mutex.unlock()
        }
    }
}
