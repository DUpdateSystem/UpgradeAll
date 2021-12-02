package net.xzos.upgradeall.ui.discover

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.view.View
import androidx.databinding.ObservableField
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.androidutils.app_info.*
import net.xzos.upgradeall.core.manager.AppManager
import net.xzos.upgradeall.core.manager.CloudConfigGetter
import net.xzos.upgradeall.core.manager.getAppId
import net.xzos.upgradeall.ui.base.list.ActivityListItemView
import net.xzos.upgradeall.ui.base.list.BaseAppIconItem

class DiscoverListItemView(
        name: String,
        val type: Int,
        val hubName: String,
        val uuid: String,
) : BaseAppIconItem, ActivityListItemView {
    override val appName: ObservableField<String> = ObservableField(name)
    override val nameFirst: ObservableField<String> = ObservableField()
    val isSavedIvVisibility = if (AppManager.getAppByUuid(uuid) == null) View.GONE else View.VISIBLE

    override val appIcon: ObservableField<Drawable> = ObservableField()
    override val iconBackgroundTint: ObservableField<ColorStateList?> = ObservableField()

    override fun getItemIdName(): String {
        return appName.get().toString()
    }

    companion object {
        fun getCloudAppItemCardView(appConfig: net.xzos.upgradeall.core.websdk.json.AppConfigGson, context: Context): DiscoverListItemView? {
            val name = appConfig.info.name
            val appUuid = appConfig.uuid
            val appId = appConfig.getAppId()
            val packageId = appId?.getPackageId() ?: return null
            val type: Int = when (packageId.first) {
                ANDROID_APP_TYPE -> R.string.android_app
                ANDROID_MAGISK_MODULE_TYPE -> R.string.magisk_module
                ANDROID_CUSTOM_SHELL -> R.string.shell
                ANDROID_CUSTOM_SHELL_ROOT -> R.string.shell_root
                else -> return null
            }
            val hubUuid = appConfig.baseHubUuid
            val hubName = CloudConfigGetter.getHubCloudConfig(hubUuid)?.info?.hubName ?: return null
            return DiscoverListItemView(name, type, hubName, appUuid).apply {
                renewAppIcon(packageId.second, context)
            }
        }
    }
}