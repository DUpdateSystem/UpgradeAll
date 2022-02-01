package net.xzos.upgradeall.utils

import android.util.TypedValue
import androidx.appcompat.app.AppCompatActivity

fun AppCompatActivity.actionBarSize(): Int {
    val tv = TypedValue()
    return if (this.theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
        TypedValue.complexToDimensionPixelSize(tv.data, resources.displayMetrics)
    } else {
        0
    }
}