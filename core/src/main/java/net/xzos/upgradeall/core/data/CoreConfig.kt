package net.xzos.upgradeall.core.data

import android.content.Context
import android.os.Build
import androidx.documentfile.provider.DocumentFile
import java.util.*

data class CoreConfig(
        @JvmField internal val androidContext: Context,
        internal val data_expiration_time: Int,
        internal val update_server_url: String,
        internal val cloud_rules_hub_url: String?,
        internal val user_download_document_file: DocumentFile?,
        internal val download_max_task_num: Int,
        internal val download_thread_num: Int,
        internal val download_auto_retry_max_attempts: Int,

        internal val install_apk_api: String,
) {
    internal val locale: Locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        androidContext.resources.configuration.locales.get(0)
    } else {
        @Suppress("DEPRECATION")
        androidContext.resources.configuration.locale
    }
}

data class WebDavConfig(
        internal val url: String?,
        internal val path: String?,
        internal val username: String?,
        internal val password: String?,
)