package net.xzos.upgradeall.core.utils

import java.util.*

/**
 * 提供 Android properties 格式的 Locale
 * 例如：* toString(): zh_CN
 */
fun getLocale(): Locale {
    val locale = Locale.getDefault()
    return Locale(locale.language, locale.country)
}