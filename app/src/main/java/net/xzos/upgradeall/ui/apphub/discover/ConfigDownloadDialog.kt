package net.xzos.upgradeall.ui.apphub.discover

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.manager.AppManager
import net.xzos.upgradeall.core.manager.CloudConfigGetter
import net.xzos.upgradeall.ui.viewmodels.viewmodel.DiscoveryViewModel

class ConfigDownloadDialog(
        private val uuid: String,
        private val viewModel: DiscoveryViewModel
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
                        runBlocking {
                            download()
                            dismiss()
                        }
                    }.setNegativeButton(R.string.cancel
                    ) { _, _ ->
                        dismiss()
                    }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private suspend fun download() {
        viewModel.downloadApplicationData(uuid)
        viewModel.viewModelScope.launch(Dispatchers.IO) {
            delay(500)
            withContext(Dispatchers.Main) {
                viewModel.loadData()
            }
        }
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