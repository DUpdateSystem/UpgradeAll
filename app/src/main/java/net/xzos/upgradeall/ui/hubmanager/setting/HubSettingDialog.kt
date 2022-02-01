package net.xzos.upgradeall.ui.hubmanager.setting

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.database.table.extra_hub.ExtraHubEntityManager
import net.xzos.upgradeall.core.manager.HubManager
import net.xzos.upgradeall.core.utils.URLReplaceData
import net.xzos.upgradeall.databinding.DialogHubSettingBinding
import net.xzos.upgradeall.utils.layoutInflater

class HubSettingDialog(private val hubUuid: String? = null) {
    private val view by lazy { runBlocking(Dispatchers.Default) { getViewData() } }

    fun show(context: Context) {
        hubUuid?.apply {
            HubManager.getHub(this)?.apply {
                showDialog(context)
            }
        } ?: showDialog(context)
    }

    private suspend fun getViewData(): HubSettingView {
        val extraHubEntity = ExtraHubEntityManager.getExtraHub(null)
        val useGlobal = hubUuid?.let {
            extraHubEntity?.global ?: false
        }
        return HubSettingView(
            useGlobal,
            extraHubEntity?.urlReplaceSearch,
            extraHubEntity?.urlReplaceString
        )
    }

    private fun showDialog(context: Context) {
        val binding = DialogHubSettingBinding.inflate(context.layoutInflater)
        binding.item = view
        MaterialAlertDialogBuilder(context)
            .setView(binding.root)
            .setNegativeButton(context.getString(R.string.cancel)) { dialog, which ->
            }
            .setPositiveButton(context.getString(R.string.ok)) { dialog, which ->
                val (useGlobal, URLReplaceData) = getURLReplaceData()
                runBlocking(Dispatchers.Default) {
                    ExtraHubEntityManager.setUrlReplace(hubUuid, useGlobal, URLReplaceData)
                }
            }
            .show()
    }

    private fun getURLReplaceData(): Pair<Boolean, URLReplaceData> {
        return Pair(
            view.useGlobalSetting.get(), URLReplaceData(
                view.matchRule.get(),
                view.replaceString.get()
            )
        )
    }
}