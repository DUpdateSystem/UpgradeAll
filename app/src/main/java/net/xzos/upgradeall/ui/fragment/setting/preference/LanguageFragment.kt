package net.xzos.upgradeall.ui.fragment.setting.preference

import android.content.SharedPreferences
import net.xzos.upgradeall.R
import net.xzos.upgradeall.utils.MiscellaneousUtils


class LanguageFragment : PrefFragment(R.xml.preferences_language), SharedPreferences.OnSharedPreferenceChangeListener {
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        MiscellaneousUtils.showToast(R.string.plz_restart)
    }
}
