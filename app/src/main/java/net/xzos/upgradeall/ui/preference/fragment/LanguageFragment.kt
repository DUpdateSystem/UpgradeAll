package net.xzos.upgradeall.ui.preference.fragment

import android.content.SharedPreferences
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.androidutils.ToastUtil


class LanguageFragment : PrefFragment(R.xml.preferences_language),
    SharedPreferences.OnSharedPreferenceChangeListener {
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

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        ToastUtil.showText(requireContext(), R.string.plz_restart)
    }
}
