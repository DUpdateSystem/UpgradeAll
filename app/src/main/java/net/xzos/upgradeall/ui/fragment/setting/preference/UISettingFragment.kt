package net.xzos.upgradeall.ui.fragment.setting.preference

import android.content.SharedPreferences
import net.xzos.upgradeall.R
import net.xzos.upgradeall.utils.MiscellaneousUtils

class UISettingFragment : PrefFragment(R.xml.preferences_ui), SharedPreferences.OnSharedPreferenceChangeListener {
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
        MiscellaneousUtils.showToast(R.string.plz_restart)
    }
}
