package net.xzos.upgradeall.utils.egg

import android.widget.Toast
import net.xzos.upgradeall.utils.MiscellaneousUtils
import java.util.*

fun getEggDay(): Day? {
    val cal = Calendar.getInstance()
    val month = cal.get(Calendar.MONTH) + 1
    val day = cal.get(Calendar.DATE)
    return when {
        month == 10 && day == 31 -> Day.HALLOWEEN
        else -> null
    }
}

fun egg() {
    when (getEggDay()) {
        Day.HALLOWEEN -> halloweenAndBirthday()
    }
}

fun halloweenAndBirthday() {
    MiscellaneousUtils.showToast("\uD83C\uDF6C\uD83E\uDD70\uD83D\uDE0B\uD83D\uDE1D\uD83D\uDE09", Toast.LENGTH_LONG)
}

fun getChineseNewYearExtraText(): String {
    return "\uD83C\uDFEE"
}