package net.xzos.upgradeall.ui.utils.file_pref

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import net.xzos.upgradeall.R
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.core.androidutils.ToastUtil
import net.xzos.upgradeall.core.androidutils.hasFilePermission
import net.xzos.upgradeall.core.utils.coroutines.wait

abstract class FilePrefActivity : AppCompatActivity() {

    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultData ->
            onActivityResultCallback(resultData)
            finish()
        }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                checkPermissionAndSelectFile()
            } else {
                ToastUtil.showText(this, R.string.please_grant_storage_perm, Toast.LENGTH_LONG)
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blank_wait)
        checkPermissionAndSelectFile()
    }

    open fun onActivityResultCallback(resultData: ActivityResult) {
        when (resultData.resultCode) {
            Activity.RESULT_OK -> onActivityResultOkCallback(resultData)
            Activity.RESULT_CANCELED -> finish()
        }
    }

    open fun onActivityResultOkCallback(resultData: ActivityResult) {}

    override fun onDestroy() {
        super.onDestroy()
        // 运行完成，解锁
        if (mutex.isLocked) mutex.unlock()
    }

    private fun checkPermissionAndSelectFile() {
        if (hasFilePermission(this)) {
            selectFile()
        } else {
            permissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    private fun selectFile() {
        resultLauncher.launch(buildIntent())
    }

    abstract fun buildIntent(): Intent

    companion object {
        private val mutex = Mutex()

        suspend fun startActivity(context: Context, cls: Class<*>) {
            withContext(Dispatchers.Default) {
                mutex.lock()
                val intent = Intent(context, cls).also {
                    if (context == MyApplication.context)
                        it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                mutex.wait()
            }
        }
    }
}
