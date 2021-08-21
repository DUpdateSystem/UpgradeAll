package net.xzos.upgradeall.core.androidutils

import android.annotation.SuppressLint
import android.content.Context

@SuppressLint("StaticFieldLeak")
lateinit var androidContext: Context

fun initContext(_context: Context) {
    androidContext = _context
}