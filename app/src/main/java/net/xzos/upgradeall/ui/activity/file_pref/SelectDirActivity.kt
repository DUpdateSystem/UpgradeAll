package net.xzos.upgradeall.ui.activity.file_pref

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import net.xzos.upgradeall.core.log.ObjectTag
import net.xzos.upgradeall.utils.file.FileUtil


class SelectDirActivity : FilePrefActivity() {

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (resultCode == Activity.RESULT_OK && resultData != null) {
            dirUri = resultData.data?.also {
                FileUtil.takePersistableUriPermission(this, it)
            }
        }
        finish()
    }

    override fun selectFile() {
        FileUtil.getFolder(this, WRITE_REQUEST_CODE, initialPath)
    }

    companion object {
        private const val TAG = "SelectDirActivity"
        private val logObjectTag = ObjectTag("UI", TAG)

        private const val WRITE_REQUEST_CODE = 2
        private var initialPath: String? = null
        private var dirUri: Uri? = null

        suspend fun newInstance(context: Context, initialPath: String? = null): Uri? {
            dirUri = null
            this.initialPath = initialPath
            startActivity(context, SelectDirActivity::class.java)
            return dirUri
        }
    }
}
