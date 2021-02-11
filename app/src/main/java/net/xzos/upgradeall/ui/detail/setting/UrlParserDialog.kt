package net.xzos.upgradeall.ui.detail.setting

import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.manager.HubManager
import net.xzos.upgradeall.core.utils.AutoTemplate
import net.xzos.upgradeall.databinding.ViewEditviewBinding

class UrlParserDialog(
        private val changedFun: (attrMap: Map<String, String?>) -> Unit
) : DialogFragment() {

    private lateinit var clickFun: () -> Unit

    fun show(manager: FragmentManager) {
        super.show(manager, TAG)
    }

    override fun onStart() {
        super.onStart() //super.onStart() is where dialog.show() is actually called on the underlying dialog, so we have to do it after this point
        val d = dialog as AlertDialog?
        if (d != null) {
            val positiveButton = d.getButton(Dialog.BUTTON_POSITIVE) as Button
            positiveButton.setOnClickListener {
                clickFun()
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val urlTemplateList = mutableListOf<String>()
        for (hub in HubManager.getHubList())
            urlTemplateList.addAll(hub.hubConfig.appUrlTemplates)
        return activity?.let {
            val binding = ViewEditviewBinding.inflate(it.layoutInflater)
            binding.nameInputLayout.setHint(R.string.plz_input_app_url)
            val builder = AlertDialog.Builder(it)
            builder.setView(binding.root)
                    .setPositiveButton(R.string.ok) { _, _ ->
                    }
                    .setNegativeButton(R.string.cancel) { dialog, _ ->
                        dialog.cancel()
                    }
            builder.create().apply {
                clickFun = fun() {
                    val url = binding.editUrl.text.toString()
                    val appIdMap = AutoTemplate.urlToAppId(url, urlTemplateList)
                    if (appIdMap != null) {
                        changedFun(appIdMap)
                        dialog?.cancel()
                    } else {
                        binding.editUrl.error = getString(R.string.not_match_any_template)
                    }
                }
            }
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    companion object {
        private const val TAG = "ConfigDownloadDialog"
    }
}