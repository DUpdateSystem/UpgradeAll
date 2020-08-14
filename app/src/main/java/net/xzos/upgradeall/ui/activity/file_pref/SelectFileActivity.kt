package net.xzos.upgradeall.ui.activity.file_pref

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import net.xzos.upgradeall.core.data.json.nongson.ObjectTag

class SelectFileActivity : FilePrefActivity() {

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (resultCode == Activity.RESULT_OK && resultData != null)
            uri = resultData.data
        finish()
    }

    override fun selectFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = mimeType
            addCategory(Intent.CATEGORY_OPENABLE)
        }

        startActivityForResult(intent, READ_REQUEST_CODE)
    }

    companion object {
        private const val TAG = "SelectFileActivity"
        private val logObjectTag = ObjectTag("UI", TAG)

        private const val READ_REQUEST_CODE = 2
        private var uri: Uri? = null
        private var mimeType: String? = null

        suspend fun newInstance(context: Context, mimeType: String): Uri? {
            uri = null
            this.mimeType = mimeType
            startActivity(context, SelectFileActivity::class.java)
            return uri
        }
    }
}
