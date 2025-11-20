package net.xzos.upgradeall.core.androidutils

import android.app.PendingIntent
import android.os.Build

object FlagDelegate {
    @Deprecated("Use Int.withImmutableFlag() extension function instead", ReplaceWith("this.withImmutableFlag()"))
    val PENDING_INTENT_FLAG_IMMUTABLE = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        PendingIntent.FLAG_IMMUTABLE
    } else {
        0
    }
}

/**
 * Adds PendingIntent.FLAG_IMMUTABLE to the flags if API level >= 23
 * This is a Kotlin-native way to avoid Java version compatibility issues
 */
fun Int.withImmutableFlag(): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        this or PendingIntent.FLAG_IMMUTABLE
    } else {
        this
    }
}