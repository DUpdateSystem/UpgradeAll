package net.xzos.upgradeall.ui.home.adapter

import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.chad.library.adapter.base.entity.MultiItemEntity

const val STYLE_CARD = 0
const val STYLE_NON_CARD = 1

abstract class HomeModuleBean(
        @DrawableRes val iconRes: Int,
        @StringRes val titleRes: Int,
        val clickListener: View.OnClickListener,
        override val itemType: Int
) : MultiItemEntity