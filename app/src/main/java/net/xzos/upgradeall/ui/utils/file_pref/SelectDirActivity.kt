package net.xzos.upgradeall.ui.utils.file_pref

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.activity.result.ActivityResult
import androidx.documentfile.provider.DocumentFile
import net.xzos.upgradeall.core.log.ObjectTag
import net.xzos.upgradeall.utils.file.FileUtil
import java.io.File


class SelectDirActivity : FilePrefActivity() {

    override fun onActivityResultOkCallback(resultData: ActivityResult) {
        dirUri = resultData.data?.data?.also {
            FileUtil.takePersistableUriPermission(this, it)
        }
    }

    override fun buildIntent(): Intent {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        val initialPath = initialPath
        if (initialPath != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val file = DocumentFile.fromFile(File(initialPath))
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, file.uri)
        }
        return intent
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
