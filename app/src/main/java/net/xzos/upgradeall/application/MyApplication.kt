package net.xzos.upgradeall.application

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.arialyy.aria.core.Aria
import jonathanfinerty.once.Once
import net.xzos.upgradeall.data.constants.OnceTag
import net.xzos.upgradeall.utils.MiscellaneousUtils
import org.jetbrains.annotations.Contract
import java.io.File

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        context = applicationContext

        Once.initialise(this)

        // 修补旧版本的命名
        if (!Once.beenDone(Once.THIS_APP_INSTALL, OnceTag.DB_NAME_MIGRATION)) {
            getDatabasePath("Repo.db-journal").delete()
            getDatabasePath("Repo.db").apply {
                this.renameTo(File(this.parentFile, "app_metadata_database.db"))
            }
            Once.markDone(OnceTag.DB_NAME_MIGRATION)
        }

        Aria.init(this)
        Aria.download(this).removeAllTask(false)  // TODO: 加入下载控制后移除
        MiscellaneousUtils.initData()
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        @get:Contract(pure = true)
        lateinit var context: Context
            private set
    }
}
