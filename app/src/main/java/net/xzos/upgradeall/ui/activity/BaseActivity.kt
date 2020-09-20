package net.xzos.upgradeall.ui.activity

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import net.xzos.upgradeall.data.PreferencesMap
import net.xzos.upgradeall.utils.getSystemLocale
import net.xzos.upgradeall.utils.wrapContextWrapper


open class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context?) {
        val locale = PreferencesMap.custom_language_locale
        if (locale != null && newBase != null
                && getSystemLocale(newBase.resources.configuration).language != locale.displayLanguage) {
            super.attachBaseContext(wrapContextWrapper(newBase, locale))
        } else
            super.attachBaseContext(newBase)
    }
}
