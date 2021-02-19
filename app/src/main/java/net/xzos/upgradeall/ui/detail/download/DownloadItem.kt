package net.xzos.upgradeall.ui.detail.download

import android.content.res.ColorStateList
import net.xzos.upgradeall.core.module.app.FileAsset
import net.xzos.upgradeall.ui.base.list.ListItemView

class DownloadItem(
        val name: String,
        val hubName: String,
        val hubColor: ColorStateList,
        val fileAsset: FileAsset,
) : ListItemView