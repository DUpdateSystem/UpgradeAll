package net.xzos.upgradeall.application

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.Build
import com.akexorcist.localizationactivity.core.LocalizationApplicationDelegate
import com.jakewharton.threetenabp.AndroidThreeTen
import jonathanfinerty.once.Once
import net.xzos.upgradeall.data.PreferencesMap
import net.xzos.upgradeall.data.constants.OnceTag
import org.jetbrains.annotations.Contract
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.io.File

class MyApplication : Application() {

    private val localizationDelegate = LocalizationApplicationDelegate()

    override fun attachBaseContext(base: Context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HiddenApiBypass.addHiddenApiExemptions("L")
        }

        PreferencesMap.setContext(fun() = base)
        val local = PreferencesMap.custom_language_locale ?: return super.attachBaseContext(base)
        localizationDelegate.setDefaultLanguage(base, local)
        super.attachBaseContext(localizationDelegate.attachBaseContext(base))
    }


    override fun onCreate() {
        super.onCreate()
        context = applicationContext

        Once.initialise(this)
        AndroidThreeTen.init(this)

        // 修补旧版本的命名
        if (!Once.beenDone(Once.THIS_APP_INSTALL, OnceTag.DB_NAME_MIGRATION)) {
            getDatabasePath("Repo.db-journal").delete()
            getDatabasePath("Repo.db").apply {
                this.renameTo(File(this.parentFile, "app_metadata_database.db"))
            }
            Once.markDone(OnceTag.DB_NAME_MIGRATION)
        }
        initCore()
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        @get:Contract(pure = true)
        lateinit var context: Context
            private set
    }
}
