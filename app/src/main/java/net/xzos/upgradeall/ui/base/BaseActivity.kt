package net.xzos.upgradeall.ui.base

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import net.xzos.upgradeall.data.PreferencesMap
import net.xzos.upgradeall.utils.UxUtils.setSystemBarStyle
import net.xzos.upgradeall.utils.getSystemLocale
import net.xzos.upgradeall.utils.wrapContextWrapper
import rikka.insets.WindowInsetsHelper
import rikka.layoutinflater.view.LayoutInflaterFactory


open class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context?) {
        val locale = PreferencesMap.custom_language_locale
        if (locale != null && newBase != null
                && getSystemLocale(newBase.resources.configuration).language != locale.displayLanguage) {
            super.attachBaseContext(wrapContextWrapper(newBase, locale))
        } else
            super.attachBaseContext(newBase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        layoutInflater.factory2 = MyViewInflater(delegate) //must called before super.onCreate()
        super.onCreate(savedInstanceState)
        window.decorView.post { setSystemBarStyle(window) }
    }

    open class MyViewInflater(delegate: AppCompatDelegate) : LayoutInflaterFactory(delegate) {

        override fun onViewCreated(view: View, parent: View?, name: String, context: Context, attrs: AttributeSet) {
            WindowInsetsHelper.attach(view, attrs)
        }
    }
}
