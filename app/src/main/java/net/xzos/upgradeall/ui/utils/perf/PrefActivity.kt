package net.xzos.upgradeall.ui.utils.perf

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import net.xzos.upgradeall.ui.base.BaseActivity

abstract class PrefActivity : BaseActivity() {

    protected open fun showRequestPermissionRationale() {}

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your
                // app.
            } else {
                showRequestPermissionRationale()
                // 用户拒接权限
            }
        }

    private fun checkPermission(context: Context) {
        // Storage Access Framework (SAF) doesn't require any storage permissions.
        // Only Android 9 (API 28) and lower need READ_EXTERNAL_STORAGE for legacy file access.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return
        }

        val permission = Manifest.permission.READ_EXTERNAL_STORAGE
        when {
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                // 用户允许了权限
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    && shouldShowRequestPermissionRationale(permission) -> {
                showRequestPermissionRationale()
                // 权限提示
            }
            else -> {
                // You can directly ask for the permission.
                // The registered ActivityResultCallback gets the result of this request.
                requestPermissionLauncher.launch(permission)
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        checkPermission(this)
        super.onCreate(savedInstanceState)
    }
}
