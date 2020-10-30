package net.xzos.upgradeall.utils

import android.widget.Toast
import java.util.*

fun egg() {
    val cal = Calendar.getInstance()
    val month = cal.get(Calendar.MONTH) + 1
    val day = cal.get(Calendar.DATE)
    when {
        month == 10 && day == 31 -> halloweenAndBirthday()
    }
}

fun halloweenAndBirthday() {
    MiscellaneousUtils.showToast("\uD83C\uDF6C\uD83E\uDD70\uD83D\uDE0B\uD83D\uDE1D\uD83D\uDE09", Toast.LENGTH_LONG)
}
