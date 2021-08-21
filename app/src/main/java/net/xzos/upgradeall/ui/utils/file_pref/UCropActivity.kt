package net.xzos.upgradeall.ui.utils.file_pref

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.ActivityResult
import com.yalantis.ucrop.UCrop
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.androidutils.IMAGE_CACHE_FILE
import net.xzos.upgradeall.core.utils.log.Log
import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.core.utils.log.msg
import net.xzos.upgradeall.core.androidutils.ToastUtil
import net.xzos.upgradeall.core.androidutils.imageUriDump
import java.io.File

class UCropActivity : FilePrefActivity() {

    override fun onActivityResultOkCallback(resultData: ActivityResult) {
        val uri = resultData.data?.data
        if (uri != null) {
            val parent = FILE.parentFile
            if (parent != null && !parent.exists())
                parent.mkdirs()
            val destinationUri = Uri.fromFile(FILE)
            UCrop.of(imageUriDump(uri, this), destinationUri)
                    .withAspectRatio(x, y)
                    .start(this, UCrop.REQUEST_CROP)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        when (resultCode) {
            Activity.RESULT_OK -> {
                isSuccess = true
                finish()
            }
            UCrop.RESULT_ERROR -> {
                val cropError = UCrop.getError(resultData!!)
                if (cropError != null)
                    Log.e(logObjectTag, TAG, "onActivityResult: 图片裁剪错误: ${cropError.msg()}")
                ToastUtil.makeText(R.string.ucrop_error, Toast.LENGTH_LONG)
                finish()
            }
            else -> finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 清除图片缓存
        if (FILE != cacheImageFile)
            cacheImageFile.delete()
    }

    override fun buildIntent(): Intent {
        return Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
        }
    }

    companion object {
        private const val TAG = "UCropActivity"
        private val logObjectTag = ObjectTag("UI", TAG)

        private const val READ_PIC_REQUEST_CODE = 2
        private val cacheImageFile = IMAGE_CACHE_FILE.also {
            it.parentFile?.mkdirs()
            it.createNewFile()
        }

        private var isSuccess = false

        private var FILE: File = File("")
        private var x = 0f
        private var y = 0f

        suspend fun newInstance(x: Float, y: Float, file: File, context: Context): Boolean {
            isSuccess = false
            FILE = file
            Companion.x = x
            Companion.y = y
            startActivity(context, UCropActivity::class.java)
            FILE = File("")
            return isSuccess
        }
    }
}
