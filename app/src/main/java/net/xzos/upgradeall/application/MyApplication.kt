package net.xzos.upgradeall.application

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.arialyy.aria.core.Aria
import net.xzos.upgradeall.android_api.IoApi
import net.xzos.upgradeall.data_manager.database.DatabaseManagerApi
import net.xzos.upgradeall.server.log.Log
import net.xzos.upgradeall.utils.VersioningUtils
import org.jetbrains.annotations.Contract
import org.litepal.LitePal
import java.io.File


@SuppressLint("Registered")
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        // 修补旧版本的命名
        getDatabasePath("Repo.db-journal").delete()
        getDatabasePath("Repo.db").apply {
            this.renameTo(File(this.parentFile, "app_metadata_database.db"))
        }

        LitePal.initialize(applicationContext)
        Aria.init(this)
        Aria.download(this).removeAllTask(true) // TODO: 测试防错

        DatabaseManagerApi
        Log
        IoApi
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        @get:Contract(pure = true)
        lateinit var context: Context
            private set
    }
}