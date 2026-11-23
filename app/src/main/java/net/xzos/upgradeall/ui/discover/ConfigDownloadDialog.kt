package net.xzos.upgradeall.ui.discover

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.androidutils.ToastUtil
import net.xzos.upgradeall.core.androidutils.app_info.getPackageId
import net.xzos.upgradeall.core.manager.AppManager
import net.xzos.upgradeall.core.manager.CloudConfigGetter
import net.xzos.upgradeall.core.manager.getAppId
import net.xzos.upgradeall.databinding.DialogDiscoverAppInfoBinding

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
                R.string.add
            appConfig.info.desc?.run {
                builder.setMessage(this)
            }
            val contentView = DialogDiscoverAppInfoBinding.inflate(layoutInflater)
            contentView.tvAppId.text = appConfig.getAppId()?.getPackageId()?.second.toString()
            contentView.tvUrl.text = appConfig.info.url
            builder.setView(contentView.root)
            builder.setTitle(appConfig.info.name)
                .setPositiveButton(
                    positiveButtonText
                ) { _, _ ->
                    with(requireActivity()) {
                        lifecycleScope.launch {
                            importRule(this@with)
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

    private suspend fun importRule(context: Context) {
        val saved = CloudConfigGetter.downloadCloudAppConfig(uuid) { }
        ToastUtil.showText(context, if (saved) R.string.save_successfully else R.string.save_failed)
    }


    private fun needUpdate(): Boolean {
        val localVersion = AppManager.getAppByUuid(uuid)?.cloudConfig?.configVersion
            ?: return false
        return appConfig.configVersion > localVersion
    }

    companion object {
        private const val TAG = "ConfigDownloadDialog"
    }
}