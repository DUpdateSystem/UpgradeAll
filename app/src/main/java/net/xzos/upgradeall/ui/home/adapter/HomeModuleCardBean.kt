package net.xzos.upgradeall.ui.home.adapter

import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes


class HomeModuleCardBean(
        @DrawableRes iconRes: Int,
        @StringRes titleRes: Int,
        clickListener: View.OnClickListener
) : HomeModuleBean(iconRes, titleRes, clickListener, STYLE_CARD)