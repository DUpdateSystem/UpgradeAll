package net.xzos.upgradeall.ui.discover

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.androidutils.ToastUtil
import net.xzos.upgradeall.core.manager.AppManager
import net.xzos.upgradeall.core.manager.CloudConfigGetter
import net.xzos.upgradeall.core.manager.GetStatus
import net.xzos.upgradeall.core.module.app.getConfigJson

class ConfigDownloadDialog(
    private val uuid: String,
    private val changedFun: () -> Unit
) : DialogFragment() {

    private val appConfig = CloudConfigGetter.getAppCloudConfig(uuid)
        ?: throw IllegalStateException("Config cannot be null")

    fun show(manager: FragmentManager) {
        super.show(manager, TAG)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val positiveButtonText = if (needUpdate())
                R.string.update
            else
                R.string.download
            appConfig.info.desc?.run {
                builder.setMessage(this)
            }
            builder.setTitle(appConfig.info.name)
                .setPositiveButton(
                    positiveButtonText
                ) { _, _ ->
                    with(requireActivity()) {
                        lifecycleScope.launch {
                            download(this@with)
                            changedFun()
                        }
                    }
                }.setNegativeButton(
                    R.string.cancel
                ) { _, _ ->
                    dismiss()
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private suspend fun download(context: Context) {
        ToastUtil.showText(context, R.string.download_start, Toast.LENGTH_LONG)
        downloadApplicationData(uuid, context)
    }

    private suspend fun downloadApplicationData(uuid: String, context: Context) {
        // 下载数据
        CloudConfigGetter.downloadCloudAppConfig(uuid) {
            ToastUtil.showText(context, getStatusMessage(context, it), Toast.LENGTH_LONG)
        }
    }

    private fun getStatusMessage(context: Context, status: GetStatus): String {
        return with(context) {
            if (status.value > 0)
                "${getString(R.string.save_successfully)}\nstatus: ${status.value}"
            else "${getString(R.string.save_failed)}\nstatus: ${status.value}"
        }
    }


    private fun needUpdate(): Boolean {
        val localVersion = AppManager.getAppByUuid(uuid)?.getConfigJson()?.configVersion
            ?: return false
        return appConfig.configVersion > localVersion
    }

    companion object {
        private const val TAG = "ConfigDownloadDialog"
    }
}