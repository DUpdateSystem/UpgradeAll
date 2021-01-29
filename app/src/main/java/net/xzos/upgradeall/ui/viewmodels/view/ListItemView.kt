package net.xzos.upgradeall.ui.viewmodels.view

import java.util.*
import java.util.regex.Pattern

abstract class ListItemView(
        open val name: String,
) {
    private val firstChar get() =  name.toCharArray().find { !pattern.matcher(it.toString()).find() }
    val nameFirst: String get() =  firstChar.toString().toUpperCase(Locale.ROOT)

    companion object {
        private val pattern = Pattern.compile("[`~!@#$^&*()=|{}':;',\\[\\].<>/?~！@#￥……&*（）——|{}【】‘；：”“'。，、？]$")
    }
}