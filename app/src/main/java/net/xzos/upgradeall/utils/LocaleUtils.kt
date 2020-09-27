package net.xzos.upgradeall.utils

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import java.util.*

fun wrapContextWrapper(context: Context, locale: Locale): ContextWrapper {
    val config = context.resources.configuration
    Locale.setDefault(locale)
    setSystemLocale(config, locale)
    val newContext = context.createConfigurationContext(config)
    return ContextWrapper(newContext)
}

fun getSystemLocale(config: Configuration): Locale {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        config.locales.get(0)
    } else {
        @Suppress("DEPRECATION")
        config.locale
    }
}

fun setSystemLocale(config: Configuration, locale: Locale?) {
    config.setLocale(locale)
}
