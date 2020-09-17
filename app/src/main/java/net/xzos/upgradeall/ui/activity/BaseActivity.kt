package net.xzos.upgradeall.ui.activity

import com.akexorcist.localizationactivity.ui.LocalizationActivity
import net.xzos.upgradeall.data.PreferencesMap

open class BaseActivity : LocalizationActivity() {
    override fun onResume() {
        with(PreferencesMap.language_locale_code) {
            if (getCurrentLanguage() != this)
                setLanguage(this)
        }
        super.onResume()
    }
}
