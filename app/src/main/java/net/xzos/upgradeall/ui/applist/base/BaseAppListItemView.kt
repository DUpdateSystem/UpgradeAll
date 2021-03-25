package net.xzos.upgradeall.ui.applist.base

import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import androidx.databinding.ObservableField
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.ui.base.list.ListItemTextView

abstract class BaseAppListItemView(val app: App) : ListItemTextView {

    override val appName: ObservableField<String> = ObservableField(app.name)
    override val nameFirst: ObservableField<String> = ObservableField()
    override val appIcon: ObservableField<Drawable> = ObservableField()
    override val iconBackgroundTint: ObservableField<ColorStateList?> = ObservableField()

    val showingVersionNumber: ObservableField<String> = ObservableField()
}