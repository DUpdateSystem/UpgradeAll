package net.xzos.upgradeall.ui.fragment.setting.preference

import android.os.Bundle
import androidx.annotation.XmlRes
import androidx.preference.PreferenceFragmentCompat
import net.xzos.upgradeall.R


open class PrefFragment internal constructor(@XmlRes private val preferencesResId: Int) : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(preferencesResId, rootKey)
    }
}

class InstallationFragment : PrefFragment(R.xml.preferences_installation)
