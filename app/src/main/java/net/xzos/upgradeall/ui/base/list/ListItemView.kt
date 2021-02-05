package net.xzos.upgradeall.ui.base.list

import android.content.res.ColorStateList
import net.xzos.upgradeall.utils.UxUtils
import java.util.*
import java.util.regex.Pattern

abstract class ListItemView(
        open val name: String,
) {
    private val firstChar get() = name.toCharArray().find { !pattern.matcher(it.toString()).find() }
    val nameFirst: String get() = firstChar.toString().toUpperCase(Locale.ROOT)
    val backgroundTint = ColorStateList.valueOf(UxUtils.getRandomColor())

    companion object {
        private val pattern = Pattern.compile("[`~!@#$^&*()=|{}':;',\\[\\].<>/?~！@#￥……&*（）——|{}【】‘；：”“'。，、？]$")
    }
}