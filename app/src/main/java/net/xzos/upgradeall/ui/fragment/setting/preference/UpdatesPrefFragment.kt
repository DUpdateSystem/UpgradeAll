package net.xzos.upgradeall.ui.fragment.setting.preference

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.preference.ListPreference
import kotlinx.android.synthetic.main.view_editview.view.*
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.data.config.AppValue
import net.xzos.upgradeall.data.PreferencesMap
import net.xzos.upgradeall.data.PreferencesMap.CUSTOM_CLOUD_RULES_HUB_URL_KEY

class UpdatesPrefFragment : PrefFragment(R.xml.preferences_update), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences
                .registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences
                .unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    private fun initView() {
        val cloudHubUrlPreference: ListPreference = findPreference(CUSTOM_CLOUD_RULES_HUB_URL_KEY)!!
        cloudHubUrlPreference.summary = if (PreferencesMap.custom_cloud_rules_hub_url)
            PreferencesMap.cloud_rules_hub_url
        else requireContext().getString(R.string.same_as_server_config)
    }

    private fun showGitUrlDialog() {
        val context = requireContext()
        AlertDialog.Builder(context).apply {
            val dialogView = LayoutInflater.from(context)
                    .inflate(R.layout.view_editview, null).apply {
                        editUrl.setText(PreferencesMap.cloud_rules_hub_url)
                    }
            setView(dialogView)
            setPositiveButton(R.string.ok) { _, _ ->
                setCloudRulesHubUrl(dialogView.editUrl.text?.toString())
            }
            setNegativeButton(R.string.cancel) { _, _ ->
            }
            setOnDismissListener {
                initView()
            }
        }.create().show()
    }

    private fun setCloudRulesHubUrl(s: String?) {
        PreferencesMap.cloud_rules_hub_url = s ?: AppValue.default_cloud_rules_hub_url
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == CUSTOM_CLOUD_RULES_HUB_URL_KEY) {
            initView()
            if (PreferencesMap.custom_cloud_rules_hub_url)
                showGitUrlDialog()
        }
    }
}
