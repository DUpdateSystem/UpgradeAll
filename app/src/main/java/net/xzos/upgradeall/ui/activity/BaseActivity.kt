package net.xzos.upgradeall.ui.activity

import android.os.Bundle
import com.akexorcist.localizationactivity.ui.LocalizationActivity
import net.xzos.upgradeall.data.PreferencesMap

open class BaseActivity : LocalizationActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        with(PreferencesMap.language_locale_code) {
            if (getCurrentLanguage() != this)
                setLanguage(this)
        }
        super.onCreate(savedInstanceState)
    }
}
