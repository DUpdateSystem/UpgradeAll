package net.xzos.upgradeall.ui.applist.base

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import androidx.databinding.ObservableField
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.utils.android_app.getPackageId
import net.xzos.upgradeall.ui.base.list.BaseAppIconItem
import net.xzos.upgradeall.ui.base.list.ListItemView
import net.xzos.upgradeall.ui.detail.AppVersionItem

abstract class BaseAppListItemView(val app: App) : BaseAppIconItem, ListItemView {

    override val appName: ObservableField<String> = ObservableField(app.name)
    override val nameFirst: ObservableField<String> = ObservableField()
    override val appIcon: ObservableField<Drawable> = ObservableField()
    override val iconBackgroundTint: ObservableField<ColorStateList?> = ObservableField()

    val versionItem = AppVersionItem()

    fun renewData(context: Context) {
        renewAppIcon(app.appId.getPackageId()?.second, context)
        versionItem.renew(app, context)
    }
}