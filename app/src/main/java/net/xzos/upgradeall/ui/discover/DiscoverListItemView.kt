package net.xzos.upgradeall.ui.discover

import android.content.res.ColorStateList
import android.view.View
import net.xzos.upgradeall.core.manager.AppManager
import net.xzos.upgradeall.ui.base.list.ListItemView
import net.xzos.upgradeall.utils.UxUtils

class DiscoverListItemView(
        name: String,
        val type: Int,
        val hubName: String,
        val uuid: String
) : ListItemView(name) {
    val backgroundTintList = ColorStateList.valueOf(UxUtils.getRandomColor())
    val isSavedIvVisibility = if (AppManager.getAppByUuid(uuid) == null) View.GONE else View.VISIBLE
}