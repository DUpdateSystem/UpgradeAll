package net.xzos.upgradeall.ui.discover

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.manager.AppManager
import net.xzos.upgradeall.core.manager.CloudConfigGetter
import net.xzos.upgradeall.core.utils.wait
import net.xzos.upgradeall.utils.MiscellaneousUtils

class ConfigDownloadDialog(
        private val uuid: String,
        private val changedFun: () -> Unit
) : DialogFragment() {

    fun show(manager: FragmentManager) {
        super.show(manager, TAG)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)
            val positiveButtonText = if (needUpdate())
                R.string.update
            else
                R.string.download
            val cloudConfig = CloudConfigGetter.getAppCloudConfig(uuid)
                    ?: throw IllegalStateException("Config cannot be null")
            cloudConfig.info.desc?.run {
                builder.setMessage(this)
            }
            builder.setTitle(cloudConfig.info.name)
                    .setPositiveButton(positiveButtonText
                    ) { _, _ ->
                        val mutex = Mutex(true)
                        lifecycleScope.launch(Dispatchers.IO) {
                            download()
                            mutex.unlock()
                        }
                        runBlocking { mutex.wait() }
                        changedFun()
                    }.setNegativeButton(R.string.cancel
                    ) { _, _ ->
                        dismiss()
                    }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private suspend fun download() {
        downloadApplicationData(uuid)
    }

    private suspend fun downloadApplicationData(uuid: String) {
        MiscellaneousUtils.showToast(R.string.download_start, Toast.LENGTH_LONG)
        // 下载数据
        CloudConfigGetter.downloadCloudAppConfig(uuid) {
            MiscellaneousUtils.showToast(getStatusMessage(it), Toast.LENGTH_LONG)
        }
    }

    private fun getStatusMessage(status: Int): String {
        return if (status > 0) getString(R.string.save_successfully)
        else "${getString(R.string.save_failed)}, status: $status"
    }


    private fun needUpdate(): Boolean {
        val appConfigGson = CloudConfigGetter.getAppCloudConfig(uuid)
                ?: throw IllegalStateException("Config cannot be null")
        val localVersion = AppManager.getAppByUuid(uuid)?.appDatabase?.cloudConfig?.configVersion
                ?: return false
        return appConfigGson.configVersion > localVersion
    }

    companion object {
        private const val TAG = "ConfigDownloadDialog"
    }
}