package net.xzos.upgradeAll.application

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context

import org.jetbrains.annotations.Contract
import org.litepal.LitePal

@SuppressLint("Registered")
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        LitePal.initialize(applicationContext)
    }

    companion object {

        @SuppressLint("StaticFieldLeak")
        @get:Contract(pure = true)
        lateinit var context: Context
            private set
    }
}
