package net.xzos.upgradeall.core.androidutils

import android.app.PendingIntent
import android.os.Build

object FlagDelegate {
    val PENDING_INTENT_FLAG_IMMUTABLE = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        PendingIntent.FLAG_IMMUTABLE
    } else {
        0
    }
}