package net.xzos.upgradeall.ui.fragment.setting.preference

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.preference.ListPreference
import net.xzos.upgradeall.R
import net.xzos.upgradeall.data.PreferencesMap
import net.xzos.upgradeall.data.PreferencesMap.CUSTOM_CLOUD_RULES_HUB_URL_KEY
import net.xzos.upgradeall.databinding.ViewEditviewBinding

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
        val binding = ViewEditviewBinding.inflate(LayoutInflater.from(context))
        AlertDialog.Builder(context).apply {
            binding.editUrl.setText(PreferencesMap.cloud_rules_hub_url)
            setView(binding.root)
            setPositiveButton(android.R.string.ok) { _, _ ->
                setCloudRulesHubUrl(binding.editUrl.text.toString())
            }
            setNegativeButton(android.R.string.cancel) { _, _ ->
            }
            setOnDismissListener {
                initView()
            }
        }.create().show()
    }

    private fun setCloudRulesHubUrl(s: String) {
        PreferencesMap.cloud_rules_hub_url = s
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == CUSTOM_CLOUD_RULES_HUB_URL_KEY) {
            initView()
            if (PreferencesMap.custom_cloud_rules_hub_url)
                showGitUrlDialog()
        }
    }
}