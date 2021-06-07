package net.xzos.upgradeall.ui.applist.base

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.text.SpannableStringBuilder
import androidx.databinding.ObservableField
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.module.app.VersionUtils
import net.xzos.upgradeall.core.utils.android_app.getPackageId
import net.xzos.upgradeall.ui.base.list.BaseAppIconItem
import net.xzos.upgradeall.ui.base.list.ListItemView
import net.xzos.upgradeall.ui.detail.AppVersionItem
import net.xzos.upgradeall.ui.detail.getVersionNameSpannableString

abstract class BaseAppListItemView(val app: App) : BaseAppIconItem, ListItemView {

    override val appName: ObservableField<String> = ObservableField(app.name)
    override val nameFirst: ObservableField<String> = ObservableField()
    override val appIcon: ObservableField<Drawable> = ObservableField()
    override val iconBackgroundTint: ObservableField<ColorStateList?> = ObservableField()

    val versionItem = AppVersionItem()

    fun renewData(context: Context) {
        renewAppIcon(app.appId.getPackageId()?.second, context)
        versionItem.renew(getShowingVersionNumber(app, context), app, context)
    }

    companion object {
        private fun getShowingVersionNumber(app: App, context: Context): SpannableStringBuilder {
            val sb = SpannableStringBuilder()
            val latestVersionNumber = app.getLatestVersionNumber()
            val rawInstalledVersionStringList = app.rawInstalledVersionStringList
            val installedVersionNumber = if (rawInstalledVersionStringList != null)
                VersionUtils.getKey(rawInstalledVersionStringList)
            else null
            if (installedVersionNumber != latestVersionNumber && latestVersionNumber != null)
                sb.append("$latestVersionNumber -> ")
            rawInstalledVersionStringList?.run {
                getVersionNameSpannableString(
                    this, null,
                    context, sb
                )
            }
            return sb
        }
    }
}