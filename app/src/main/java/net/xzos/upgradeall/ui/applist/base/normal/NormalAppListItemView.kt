package net.xzos.upgradeall.ui.applist.base.normal

import androidx.activity.viewModels
import androidx.databinding.ObservableField
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.ui.applist.base.BaseAppListItemView
import net.xzos.upgradeall.ui.detail.AppDetailViewModel

class NormalAppListItemView(app: App) : BaseAppListItemView(app) {

    private val viewModel by viewModels<AppDetailViewModel>()
    val statusIcon: ObservableField<Int> = ObservableField()
    val ivStatusVisibility: ObservableField<Int> = ObservableField()
    val pbStatusVisibility: ObservableField<Int> = ObservableField()
}